"""Response models (also documented in the OpenAPI spec at /docs)."""
from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, Field


class PlantPrediction(BaseModel):
    id: str = Field(examples=["Tomato"])
    name: str = Field(examples=["Tomat"])
    confidence: float = Field(ge=0, le=1)


class ConditionPrediction(BaseModel):
    id: str = Field(examples=["Early_blight"])
    label: str = Field(examples=["Tomato___Early_blight"])
    name: str = Field(examples=["Bercak kering"])
    type: str = Field(examples=["fungal"])
    confidence: float = Field(ge=0, le=1)


class ClassPrediction(BaseModel):
    label: str
    plant_id: str
    plant_name: str
    condition_id: str
    condition_name: str
    confidence: float = Field(ge=0, le=1)


class PredictResponse(BaseModel):
    recognized: bool = Field(description="False when the photo does not look like any supported plant")
    plant: PlantPrediction
    condition_status: Literal["assessed", "uncertain", "not_supported", "unrecognized"] = Field(
        description="assessed: confident diagnosis; uncertain: best guess below threshold; "
                    "not_supported: dataset cannot assess this plant's leaf condition; "
                    "unrecognized: plant not recognized")
    condition: Optional[ConditionPrediction] = None
    top_predictions: list[ClassPrediction]
    top_plants: list[PlantPrediction]
    model_version: str
    inference_ms: float
    disclaimer: str


class HealthResponse(BaseModel):
    status: str


class ReadyResponse(BaseModel):
    status: str
    model_version: Optional[str] = None
