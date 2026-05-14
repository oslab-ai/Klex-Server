import os
import json
import logging
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage

from reports.models import (
    Report, ReportPermission, OrganizationReportPermission,
    ReportGroupMember, GroupUserPermission, GroupOrganizationPermission,
)

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


@tool
def navigate_to_page(page_url: str) -> str:
    """Navigates the user to a specific page in the application.

    Valid page_url values are:
    - "/reports" (to view all reports)
    - "/adapters" or "/data-adapters" (to view or create data adapters/sources)
    - "/users" (to manage users)
    - "/permissions" (to manage roles and permissions)
    - "/schedules" (to view scheduled report deliveries)
    - "/embeddings" (API Reference)
    """
    return f"Navigating to {page_url}"


@tool
def open_report(report_id: int, report_name: str) -> str:
    """Opens a specific report by its ID.

    Use this when the user asks to open, view, or run a specific report.
    Pick the report_id from the report catalog provided in the system context.
    The report_name parameter is for confirmation messaging only.
    """
    return f"Opening report '{report_name}' (id={report_id})"


class ChatbotAPIView(APIView):
    # Depending on requirements, you might want IsAuthenticated, 
    # but for a global UI widget, we'll keep it authenticated if Klex is authenticated.
    permission_classes = [IsAuthenticated]

    def post(self, request):
        messages_data = request.data.get("messages", [])
        if not messages_data:
            return Response({"error": "No messages provided"}, status=400)

        # Build a permission-filtered report catalog for this user
        report_catalog = _build_report_catalog(request.user)
        catalog_json = json.dumps(report_catalog, indent=None, ensure_ascii=False)

        # Build system prompt with report catalog
        system_content = (
            "You are a helpful AI assistant for the Klex reporting platform. "
            "Your primary capabilities are:\n"
            "1. Navigate users to pages using the navigate_to_page tool.\n"
            "2. Open specific reports using the open_report tool.\n\n"
            "Available reports for this user:\n"
            f"{catalog_json}\n\n"
            "When the user asks to open a report, find the best match from the "
            "catalog above and use the open_report tool with the matching report_id. "
            "If the user's request is ambiguous (multiple reports match), list the "
            "matching reports and ask which one they mean. "
            "If no report matches, let the user know politely."
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
        # Fallback to a free model if not specified
        model_name = os.getenv("OPENROUTER_MODEL", "inclusionai/ring-2.6-1t:free")
        api_key = os.getenv("OPENROUTER_API_KEY")
        
        if not api_key:
            return Response({"error": "OpenRouter API Key not configured on the server."}, status=500)

        llm = ChatOpenAI(
            base_url="https://openrouter.ai/api/v1",
            api_key=api_key,
            model=model_name,
            temperature=0,
        )

        # Bind tools
        llm_with_tools = llm.bind_tools([navigate_to_page, open_report])

        # Invoke LLM
        try:
            response = llm_with_tools.invoke(langchain_messages)
        except Exception as e:
            return Response({"error": str(e)}, status=500)

        # Parse output for tool calls
        navigation_target = None
        if hasattr(response, 'tool_calls') and response.tool_calls:
            for tc in response.tool_calls:
                if tc['name'] == 'navigate_to_page':
                    navigation_target = tc['args'].get('page_url')
                    break
                elif tc['name'] == 'open_report':
                    report_id = tc['args'].get('report_id')
                    if report_id:
                        navigation_target = f"/reports/{report_id}"
                    break
        
        # If the LLM makes a tool call, its content might be empty.
        # We handle this by returning a friendly message along with the target.
        content = response.content
        if not content and navigation_target:
            if navigation_target.startswith('/reports/'):
                # Find the report name for a nicer message
                report_name = "the report"
                for tc in response.tool_calls:
                    if tc['name'] == 'open_report':
                        report_name = tc['args'].get('report_name', report_name)
                        break
                content = f"Opening {report_name} for you now."
            else:
                content = f"I'm taking you to {navigation_target} now."
        
        return Response({
            "role": "assistant",
            "content": content,
            "navigation_target": navigation_target
        })
