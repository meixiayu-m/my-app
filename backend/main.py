import json
import os
import re
from base64 import b64encode
from datetime import date
from pathlib import Path
from typing import Any, List, Optional
from uuid import uuid4
import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel, Field

load_dotenv(Path(__file__).with_name(".env"))

app = FastAPI(title="CalorieLens API", version="0.2.0")

ZHIPU_API_URL = os.getenv("ZHIPU_API_URL", "https://open.bigmodel.cn/api/paas/v4/chat/completions")
ZHIPU_MODEL = os.getenv("ZHIPUAI_MODEL", "glm-4.6v-flash")
MAX_IMAGE_BYTES = 12 * 1024 * 1024

class MealItem(BaseModel):
    name: str
    grams: float = Field(gt=0)
    calories: int = Field(ge=0)
    protein: float = Field(ge=0)
    fat: float = Field(ge=0)
    carbs: float = Field(ge=0)
    fiber: float = Field(ge=0)

class Meal(BaseModel):
    id: Optional[str] = None
    title: str = "餐食"
    eaten_on: date = Field(default_factory=date.today)
    items: List[MealItem]


class RecognitionItem(BaseModel):
    name: str
    grams: float = Field(gt=0)
    calories: float = Field(ge=0)
    protein: float = Field(ge=0)
    fat: float = Field(ge=0)
    carbs: float = Field(ge=0)
    fiber: float = Field(ge=0)
    confidence: float = Field(default=0.5, ge=0, le=1)


class RecognitionResponse(BaseModel):
    items: List[RecognitionItem]

MEALS: dict[str, Meal] = {}

class Score(BaseModel):
    total: int
    reasons: List[str]

class Profile(BaseModel):
    sex: str = "female"
    age: int = Field(ge=13, le=120)
    height_cm: float = Field(gt=100, lt=250)
    weight_kg: float = Field(gt=25, lt=400)
    activity_multiplier: float = Field(default=1.375, ge=1.2, le=1.9)
    deficit_percent: int = Field(default=15, ge=0, le=30)

class Target(BaseModel):
    bmr: int
    tdee: int
    target_calories: int
    max_safe_deficit: int

PROFILE = Profile(age=30, height_cm=165, weight_kg=60)

@app.get("/health")
def health():
    return {"status": "ok", "service": "calorielens", "model": ZHIPU_MODEL}


def _content_to_text(content: Any) -> str:
    """兼容视觉模型返回字符串或 text 内容数组。"""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return "\n".join(
            part.get("text", "") if isinstance(part, dict) else str(part)
            for part in content
        )
    return str(content)


def _parse_json(text: str) -> dict[str, Any]:
    cleaned = text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        start, end = cleaned.find("{"), cleaned.rfind("}")
        if start < 0 or end <= start:
            raise HTTPException(status_code=502, detail="视觉模型返回的不是有效 JSON")
        try:
            return json.loads(cleaned[start:end + 1])
        except json.JSONDecodeError as exc:
            raise HTTPException(status_code=502, detail="视觉模型返回的 JSON 无法解析") from exc


@app.post("/recognize", response_model=RecognitionResponse)
async def recognize(image: UploadFile = File(...)):
    """接收图片并调用智谱 GLM-4.6V-Flash，API Key 只从服务器环境变量读取。"""
    api_key = os.getenv("ZHIPUAI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=503, detail="后端未配置 ZHIPUAI_API_KEY")
    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(status_code=415, detail="只支持图片文件")

    raw = await image.read(MAX_IMAGE_BYTES + 1)
    if not raw:
        raise HTTPException(status_code=400, detail="图片为空")
    if len(raw) > MAX_IMAGE_BYTES:
        raise HTTPException(status_code=413, detail="图片太大，请选择 12MB 以内的图片")

    prompt = """
你是一个严谨的食物营养估算助手。识别图片中能看见的食物，按独立食物项返回。
请估算每项可食部分重量（克）和整份对应的热量、蛋白质、脂肪、碳水、膳食纤维。
只能返回 JSON，不要 Markdown，不要解释文字，格式必须是：
{"items":[{"name":"食物名称","grams":150,"calories":174,"protein":3.9,"fat":0.5,"carbs":38.9,"fiber":0.5,"confidence":0.86}]}
看不清时可以返回空 items，不要编造固定的菜名；所有数值都必须是非负数字，confidence 在 0 到 1 之间。
""".strip()
    data_url = f"data:{image.content_type};base64,{b64encode(raw).decode('ascii')}"
    payload = {
        "model": ZHIPU_MODEL,
        "temperature": 0.1,
        "messages": [
            {"role": "system", "content": prompt},
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "请识别这张餐食图片并严格按要求返回 JSON。"},
                    {"type": "image_url", "image_url": {"url": data_url}},
                ],
            },
        ],
    }
    try:
        async with httpx.AsyncClient(timeout=90) as client:
            response = await client.post(
                ZHIPU_API_URL,
                headers={"Authorization": f"Bearer {api_key}"},
                json=payload,
            )
            response.raise_for_status()
    except httpx.HTTPStatusError as exc:
        detail = exc.response.text[:500]
        raise HTTPException(status_code=502, detail=f"智谱接口返回错误：{detail}") from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="无法连接智谱接口") from exc

    try:
        choices = response.json().get("choices", [])
        content = choices[0]["message"]["content"]
        parsed = _parse_json(_content_to_text(content))
        return RecognitionResponse.model_validate(parsed)
    except (IndexError, KeyError, TypeError, ValueError) as exc:
        raise HTTPException(status_code=502, detail="智谱返回格式不符合预期") from exc

@app.get("/profile", response_model=Profile)
def get_profile():
    return PROFILE

@app.put("/profile", response_model=Profile)
def update_profile(profile: Profile):
    global PROFILE
    PROFILE = profile
    return PROFILE

@app.get("/target", response_model=Target)
def target():
    p = PROFILE
    bmr = round(10 * p.weight_kg + 6.25 * p.height_cm - 5 * p.age + (5 if p.sex == "male" else -161))
    bmr = max(900, bmr)
    tdee = round(bmr * p.activity_multiplier)
    deficit = min(500, round(tdee * p.deficit_percent / 100))
    return Target(bmr=bmr, tdee=tdee, target_calories=max(1200, tdee - deficit), max_safe_deficit=deficit)

@app.get("/meals", response_model=List[Meal])
def list_meals(eaten_on: Optional[date] = None):
    values = list(MEALS.values())
    return [m for m in values if eaten_on is None or m.eaten_on == eaten_on]

@app.post("/meals", response_model=Meal)
def create_meal(meal: Meal):
    saved = meal.model_copy(update={"id": str(uuid4())})
    MEALS[saved.id] = saved
    return saved

@app.delete("/meals/{meal_id}")
def delete_meal(meal_id: str):
    if meal_id not in MEALS:
        return {"deleted": False}
    del MEALS[meal_id]
    return {"deleted": True}

@app.post("/score", response_model=Score)
def score(meal: Meal):
    calories = sum(x.calories for x in meal.items)
    protein = sum(x.protein for x in meal.items)
    fiber = sum(x.fiber for x in meal.items)
    fat = sum(x.fat for x in meal.items)
    total = 100
    reasons = []
    if calories > 700:
        total -= 15; reasons.append("本餐热量超过建议预算，-15")
    else:
        reasons.append("热量符合本餐预算，+0")
    if protein < 20:
        total -= 15; reasons.append("蛋白质偏少，-15")
    else:
        reasons.append("蛋白质较充足，+0")
    if fiber < 5:
        total -= 10; reasons.append("蔬菜或膳食纤维偏少，-10")
    if fat > 35:
        total -= 10; reasons.append("脂肪偏高，-10")
    return Score(total=max(0, total), reasons=reasons)
