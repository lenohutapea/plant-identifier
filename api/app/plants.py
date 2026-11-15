"""Indonesian plant care and condition information (app/data/plants.json)."""
from __future__ import annotations

import json
from pathlib import Path


class PlantStore:
    def __init__(self, path: Path):
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        self.disclaimer: str = data.get("disclaimer", "")
        self.plants: dict[str, dict] = data["plants"]

    def get(self, plant_id: str) -> dict | None:
        return self.plants.get(plant_id)

    def condition(self, plant_id: str, condition_id: str) -> dict | None:
        plant = self.get(plant_id)
        return None if plant is None else plant["conditions"].get(condition_id)

    def summaries(self) -> list[dict]:
        return [
            {
                "id": plant_id,
                "name": plant["name"],
                "scientific_name": plant["scientific_name"],
                "category": plant["category"],
                "condition_supported": plant["condition_supported"],
                "conditions": [{"id": cid, "name": c["name"], "type": c["type"]}
                               for cid, c in plant["conditions"].items()],
            }
            for plant_id, plant in sorted(self.plants.items())
        ]

    def missing_for(self, class_ids: list[str]) -> list[str]:
        """Model classes ('Plant___Condition') that have no entry in plants.json."""
        missing = []
        for class_id in class_ids:
            plant_id, _, condition_id = class_id.partition("___")
            if self.condition(plant_id, condition_id) is None:
                missing.append(class_id)
        return missing
