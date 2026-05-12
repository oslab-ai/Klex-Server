import os
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage

@tool
def navigate_to_page(page_url: str) -> str:
    """Navigates the user to a specific page in the application.
    
    Valid page_url values are:
    - "/reports" (to view reports)
    - "/adapters" (to view or create data adapters/sources)
    - "/users" (to manage users)
    - "/permissions" (to manage roles and permissions)
    - "/schedules" (to view scheduled report deliveries)
    - "/embeddings" (API Reference)
    """
    return f"Navigating to {page_url}"

class ChatbotAPIView(APIView):
    # Depending on requirements, you might want IsAuthenticated, 
    # but for a global UI widget, we'll keep it authenticated if Klex is authenticated.
    permission_classes = [IsAuthenticated]

    def post(self, request):
        messages_data = request.data.get("messages", [])
        if not messages_data:
            return Response({"error": "No messages provided"}, status=400)

        # Convert simple JSON messages to LangChain messages
        langchain_messages = [
            SystemMessage(content="You are a helpful AI assistant for the Klex reporting platform. "
                                  "Your primary capability is to guide the user and navigate them to the correct pages. "
                                  "If the user asks to go somewhere, use the navigate_to_page tool.")
        ]
        
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
        llm_with_tools = llm.bind_tools([navigate_to_page])

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
        
        # If the LLM makes a tool call, its content might be empty.
        # We handle this by returning a friendly message along with the target.
        content = response.content
        if not content and navigation_target:
            content = f"I'm taking you to {navigation_target} now."
        
        return Response({
            "role": "assistant",
            "content": content,
            "navigation_target": navigation_target
        })
