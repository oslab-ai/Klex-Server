import os
import json
import logging
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage, ToolMessage

from reports.models import (
    Report, ReportPermission, OrganizationReportPermission,
    ReportGroupMember, GroupUserPermission, GroupOrganizationPermission,
    ScheduledJob,
)

from .tools import build_tools_for_user

logger = logging.getLogger(__name__)


def _get_accessible_report_ids(user):
    """
    Return the set of report IDs the user can access across all
    permission layers: direct user grants, org grants, and group grants.
    """
    ids = set()
    ids.update(
        ReportPermission.objects.filter(
            user=user, can_access=True
        ).values_list('report_id', flat=True)
    )
    if user.organization_id:
        ids.update(
            OrganizationReportPermission.objects.filter(
                organization=user.organization, can_access=True
            ).values_list('report_id', flat=True)
        )
    group_ids = set(
        GroupUserPermission.objects.filter(
            user=user, can_access=True
        ).values_list('group_id', flat=True)
    )
    if user.organization_id:
        group_ids.update(
            GroupOrganizationPermission.objects.filter(
                organization=user.organization, can_access=True
            ).values_list('group_id', flat=True)
        )
    if group_ids:
        ids.update(
            ReportGroupMember.objects.filter(
                group_id__in=group_ids
            ).values_list('report_id', flat=True)
        )
    return ids


def _build_report_catalog(user):
    """
    Build a compact JSON-serializable list of reports the user can access,
    including metadata summaries for the LLM to match against.
    """
    from django.db.models import Q

    if user.is_admin:
        reports = Report.objects.all().order_by('report_name')
    else:
        permitted_ids = _get_accessible_report_ids(user)
        reports = Report.objects.filter(
            Q(id__in=permitted_ids) | Q(is_public=True)
        ).order_by('report_name')

    catalog = []
    for r in reports:
        entry = {
            'id': r.id,
            'name': r.display_name or r.report_name,
            'path': r.path,
        }
        if r.metadata_cache:
            entry['summary'] = r.metadata_cache.get('summary', '')
            params = r.metadata_cache.get('parameter_names', [])
            if params:
                entry['parameters'] = params
            charts = r.metadata_cache.get('chart_types', [])
            if charts:
                entry['charts'] = charts
        catalog.append(entry)

    return catalog


def _build_adapter_catalog():
    """Build a compact list of existing data adapters for the LLM."""
    from dataadapter.models import DataAdapter
    adapters = DataAdapter.objects.all().order_by('-created_at')
    return [
        {'id': a.id, 'name': a.name, 'type': a.adapter_type, 'active': a.is_active}
        for a in adapters[:30]
    ]


def _build_jobs_summary():
    """Build a compact summary of active scheduled jobs for the LLM."""
    jobs = ScheduledJob.objects.filter(is_active=True).order_by('-priority', '-created_at')[:15]
    return [
        {
            'id': j.id,
            'name': j.schedule_name,
            'report': (j.report.display_name or j.report.report_name) if j.report else '',
            'status': j.status,
            'cron': j.cron_expression,
            'department': j.department,
        }
        for j in jobs
    ]


def _build_system_prompt(user, report_catalog, adapter_catalog=None, jobs_summary=None):
    """Build the enhanced system prompt with catalogs and capabilities."""
    catalog_json = json.dumps(report_catalog, indent=None, ensure_ascii=False)

    # Base capabilities
    capabilities = [
        "1. **Navigation**: Open pages and specific reports using navigate_to_page and open_report tools.",
        "2. **Report Compilation**: Compile/run reports in various formats (PDF, XLSX, CSV, HTML, DOCX, PPTX, ODT) using the compile_report tool.",
    ]

    if user.is_admin:
        capabilities += [
            "3. **Scheduling** (Admin): Create scheduled report jobs using the schedule_report tool.",
            "4. **Job Management** (Admin): List, hold, release, restart, cancel, or kill scheduled jobs using list_jobs and manage_job tools.",
            "5. **Data Adapters** (Admin): Create, list, and test data source connections using create_data_adapter, list_adapters, and test_adapter_connection tools.",
        ]

    capabilities_text = "\n".join(capabilities)

    system_content = (
        "You are Klex AI, an intelligent assistant for the Klex reporting platform.\n\n"
        "## Your Capabilities\n"
        f"{capabilities_text}\n\n"
        "## CRITICAL: Natural Language Understanding\n"
        "You MUST extract parameters from natural language. NEVER ask the user for "
        "technical formats like cron expressions, JSON, or IDs. Instead, understand "
        "their intent and convert it yourself.\n\n"
        "### How to parse user messages:\n"
        "When a user provides multiple pieces of information in a single message, "
        "extract ALL parameters at once. Examples:\n\n"
        '- User: "name - test and schedule for every minute with only 3 occurrences"\n'
        '  → schedule_name="test", cron_expression="* * * * *", max_runs=3\n\n'
        '- User: "call it Daily Sales, run every day at 9am"\n'
        '  → schedule_name="Daily Sales", cron_expression="0 9 * * *"\n\n'
        '- User: "name it Weekly Metrics, every Monday 8am, high priority"\n'
        '  → schedule_name="Weekly Metrics", cron_expression="0 8 * * 1", priority=1\n\n'
        '- User: "run the customer report as Excel"\n'
        '  → Find "customer" report from catalog, output_format="XLSX"\n\n'
        '- User: "schedule customer report every friday at 5pm"\n'
        '  → Find "customer" report, schedule_name="Customer Report - Weekly", '
        'cron_expression="0 17 * * 5"\n\n'
        "### Cron expression translation (do this SILENTLY — never show cron to user):\n"
        '- "every minute" → "* * * * *"\n'
        '- "every hour" → "0 * * * *"\n'
        '- "every day at 9 AM" / "daily at 9" / "every morning at 9" → "0 9 * * *"\n'
        '- "every Monday at 8 AM" / "weekly on monday 8am" → "0 8 * * 1"\n'
        '- "every Friday at 5 PM" → "0 17 * * 5"\n'
        '- "every weekday at 6 AM" → "0 6 * * 1-5"\n'
        '- "first of every month" / "monthly" → "0 0 1 * *"\n'
        '- "every 15 minutes" → "*/15 * * * *"\n'
        '- "every 2 hours" → "0 */2 * * *"\n'
        '- "twice a day at 9am and 5pm" → "0 9,17 * * *"\n\n'
        "### Schedule name generation:\n"
        "If the user provides a name (e.g., 'name - test', 'call it X', 'name: X'), use it. "
        "If not, auto-generate a descriptive name like 'Daily Customer Report' or "
        "'Weekly Sales - Friday'.\n\n"
        "### Occurrence / max runs handling:\n"
        '"only 3 times", "3 occurrences", "run it 5 times" → pass as max_runs parameter.\n\n'
        "## Conversation Guidelines\n"
        "- **Parse first, ask later**: Always try to extract ALL parameters from what "
        "the user said. Only ask for truly missing REQUIRED information.\n"
        "- **Be smart about matching**: If the user says 'customer report', match it "
        "to the closest report in the catalog. Don't ask for the exact report ID.\n"
        "- **Use sensible defaults**: department='General', priority=0, format='PDF'. "
        "Don't ask for optional parameters unless the user seems to want to customize.\n"
        "- **Confirm before executing** destructive/high-impact actions by summarizing "
        "what you understood in plain language (not technical details).\n"
        "  Example: 'I\'ll schedule the Customers report to run every minute, limited to "
        "3 runs, named \"test\". Shall I go ahead?'\n"
        "- **After completing an action**, summarize what was done clearly.\n"
        "- When the user asks to open a report, find the best match from the "
        "catalog. If ambiguous, list options and ask which one.\n"
        "- Keep responses concise and conversational.\n\n"
        "## Available Reports\n"
        f"{catalog_json}\n"
    )

    if user.is_admin and adapter_catalog:
        adapter_json = json.dumps(adapter_catalog, indent=None, ensure_ascii=False)
        system_content += f"\n## Configured Data Adapters\n{adapter_json}\n"

    if user.is_admin and jobs_summary:
        jobs_json = json.dumps(jobs_summary, indent=None, ensure_ascii=False)
        system_content += f"\n## Active Scheduled Jobs\n{jobs_json}\n"

    return system_content


def _run_agent_loop(llm, tools, messages, max_iterations=5):
    """
    Run the tool-calling agent loop:
    1. Send messages to LLM
    2. If LLM returns tool calls, execute them
    3. Feed tool results back to LLM
    4. Repeat until LLM returns a text response (no tool calls)

    Returns (final_content, collected_actions) where collected_actions
    is a list of parsed action results from tool outputs.
    """
    llm_with_tools = llm.bind_tools(tools)
    current_messages = list(messages)
    tool_map = {t.name: t for t in tools}
    collected_actions = []

    for _ in range(max_iterations):
        response = llm_with_tools.invoke(current_messages)

        if not response.tool_calls:
            # LLM is done — return the final text response
            return response.content, collected_actions

        # Execute each tool call
        current_messages.append(response)  # Add AI message with tool calls

        for tc in response.tool_calls:
            tool_name = tc['name']
            tool_fn = tool_map.get(tool_name)

            if not tool_fn:
                tool_result = json.dumps({
                    "message": f"Unknown tool: {tool_name}",
                    "__action_result__": {"type": "error", "action": tool_name,
                                          "details": {"error": "Tool not found"}},
                })
            else:
                try:
                    tool_result = tool_fn.invoke(tc['args'])
                except Exception as e:
                    logger.error(f"Tool {tool_name} failed: {e}")
                    tool_result = json.dumps({
                        "message": f"Tool error: {str(e)}",
                        "__action_result__": {"type": "error", "action": tool_name,
                                              "details": {"error": str(e)}},
                    })

            # Parse structured actions from tool result
            try:
                parsed = json.loads(tool_result)
                if "__action__" in parsed:
                    collected_actions.append(parsed)
                if "__action_result__" in parsed:
                    collected_actions.append(parsed)
            except (json.JSONDecodeError, TypeError):
                pass

            current_messages.append(
                ToolMessage(content=str(tool_result), tool_call_id=tc['id'])
            )

    # Hit max iterations — return whatever we have
    return response.content, collected_actions


class ChatbotAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        messages_data = request.data.get("messages", [])
        if not messages_data:
            return Response({"error": "No messages provided"}, status=400)

        user = request.user

        # Build context catalogs
        report_catalog = _build_report_catalog(user)
        adapter_catalog = _build_adapter_catalog() if user.is_admin else None
        jobs_summary = _build_jobs_summary() if user.is_admin else None

        # Build system prompt
        system_content = _build_system_prompt(
            user, report_catalog, adapter_catalog, jobs_summary
        )

        # Convert simple JSON messages to LangChain messages
        langchain_messages = [SystemMessage(content=system_content)]

        for msg in messages_data:
            role = msg.get("role")
            content = msg.get("content")
            if role == "user":
                langchain_messages.append(HumanMessage(content=content))
            elif role == "assistant":
                langchain_messages.append(AIMessage(content=content))

        # Setup OpenRouter LLM
        model_name = os.getenv("OPENROUTER_MODEL", "inclusionai/ring-2.6-1t:free")
        api_key = os.getenv("OPENROUTER_API_KEY")

        if not api_key:
            return Response({"error": "OpenRouter API Key not configured on the server."}, status=500)

        llm = ChatOpenAI(
            base_url="https://openrouter.ai/api/v1",
            api_key=api_key,
            model=model_name,
            temperature=0.1,
            default_headers={
                "HTTP-Referer": os.getenv("FRONTEND_BASE_URL", "http://localhost:5173"),
                "X-Title": "Klex AI Assistant",
            },
            extra_body={
                "provider": {
                    "require_parameters": True,
                },
            },
        )

        # Build tools for this user
        tools = build_tools_for_user(user)

        # Run the agentic loop
        try:
            final_content, collected_actions = _run_agent_loop(
                llm, tools, langchain_messages, max_iterations=5
            )
        except Exception as e:
            logger.error(f"Agent loop error: {e}")
            return Response({"error": str(e)}, status=500)

        # Parse navigation targets and action results from collected actions
        navigation_target = None
        action_result = None

        for action_data in collected_actions:
            # Check for navigation
            if action_data.get("__action__") == "navigate":
                navigation_target = action_data.get("navigation_target")

            # Check for action results
            ar = action_data.get("__action_result__")
            if ar:
                action_result = ar
                # Navigation from open_report
                if ar.get("action") == "compile_report" and ar.get("type") == "success":
                    pass  # Compile success — no navigation needed
                nav = action_data.get("navigation_target")
                if nav:
                    navigation_target = nav

        # If the LLM content is empty but we have actions, generate a message
        content = final_content or ""
        if not content and navigation_target:
            if navigation_target.startswith('/reports/'):
                content = f"Opening the report for you now."
            else:
                content = f"Taking you to {navigation_target} now."

        response_data = {
            "role": "assistant",
            "content": content,
            "navigation_target": navigation_target,
        }

        if action_result:
            response_data["action_result"] = action_result

        return Response(response_data)
