from fastapi import APIRouter
from pydantic import BaseModel
from app.chatbot import get_response

router = APIRouter()

class ChatMessage(BaseModel):
    message: str

@router.post("/")
def chat_endpoint(msg: ChatMessage):
    return {"response": get_response(msg.message)}
