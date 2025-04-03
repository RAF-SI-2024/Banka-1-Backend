from google import genai
from google.genai import types
import pathlib
import httpx
from dotenv import load_dotenv
import os

load_dotenv()

client = genai.Client(api_key=os.getenv("GOOGLE_API"))

response = client.models.generate_content(
    model="gemini-2.0-flash",
    contents="Explain how AI works",
)

print(response.text)

def customer_service_response(prompt):

    pdf_path = "app/documents/context.pdf"

    file_path = pathlib.Path(pdf_path)
    file_path.write_bytes(httpx.get(pdf_path).content)

    sample_file = client.files.upload(
        file=file_path,
    )


    response = client.models.generate_content(
        model="gemini-2.0-flash",
        contents=[sample_file, prompt]
    )

    return response.text