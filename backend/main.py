from datetime import date
from typing import List, Optional
from uuid import uuid4
from fastapi import FastAPI
from pydantic import BaseModel, Field

app = FastAPI(title="CalorieLens API", version="0.1.0")

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
    return {"status": "ok", "service": "calorielens"}

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
