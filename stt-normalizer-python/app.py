import io, json, os, tempfile
from dataclasses import dataclass
from typing import Optional

from fastapi import FastAPI, UploadFile, Form
from fastapi.responses import JSONResponse
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()
client = OpenAI()
app = FastAPI()

@dataclass
class NormalizeOptions:
    stt_model: str = "whisper-1"
    chat_model: str = "gpt-5-nano"
    language_hint: str = "ko"
    keep_style: bool = True

PROMPT_TEMPLATE = """\
다음 문장은 한국어 방언입니다. 
요구사항:
- 의미 보존, 정보 누락 금지, 과도한 의역 금지
- 맞춤법/띄어쓰기 및 어휘를 표준어로 변환
- 존대/반말은 원문 존중{style_note}
- 숫자/고유명사 유지, 불필요한 구두점 제거
- 단어 단위로 나눠서 사투리를 번역
- 결과만 JSON으로 반환: {{"standard": "..."}}

방언:
{dialect_text}
"""

def transcribe_bytes(audio_bytes: bytes, filename: str, model: str, language_hint: Optional[str]) -> str:
    # OpenAI는 file-like 필요 → temp 파일 사용
    with tempfile.NamedTemporaryFile(delete=False, suffix=os.path.splitext(filename)[1] or ".wav") as tmp:
        tmp.write(audio_bytes)
        tmp.flush()
        tmp_path = tmp.name
    try:
        with open(tmp_path, "rb") as f:
            stt = client.audio.transcriptions.create(
                model=model,
                file=f,
                language=language_hint
            )
        return getattr(stt, "text", "").strip()
    finally:
        try: os.remove(tmp_path)
        except: pass

def normalize_to_standard(text: str, model: str, keep_style: bool, temperature: float) -> str:
    style_note = " (말투/높임은 유지)" if keep_style else ""
    prompt = PROMPT_TEMPLATE.format(dialect_text=text, style_note=style_note)
    chat = client.chat.completions.create(
        model=model,
        temperature=temperature,
        messages=[
            {"role": "system", "content": "You are a Korean text normalizer."},
            {"role": "user", "content": prompt},
        ],
        response_format={"type": "json_object"}
    )
    content = chat.choices[0].message.content
    try:
        data = json.loads(content)
        return data.get("standard", "").strip()
    except Exception:
        return content.strip()

@app.get("/health")
def health():
    return {"ok": True}

@app.post("/api/stt-normalize")
async def stt_normalize(
    file: UploadFile,
    engine: str = Form("whisper-1"),
    llm: str = Form("gpt-4o-mini"),
    keep_style: bool = Form(True),
    language_hint: str = Form("ko"),
    temperature: float = Form(0.0),
):
    raw = await file.read()
    dialect = transcribe_bytes(raw, file.filename or "audio.wav", engine, language_hint)
    if not dialect:
        return JSONResponse({"error": "empty transcription"}, status_code=400)
    standard = normalize_to_standard(dialect, llm, keep_style, temperature)
    return {"dialect": dialect, "standard": standard}
