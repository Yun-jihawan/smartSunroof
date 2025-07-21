import json
import os
from config.file_path import PRESET_FILE

class PresetManager:
    def __init__(self):
        self.filepath = PRESET_FILE
        self.preset_data = self._load_presets()
        self.ps_id = 1 

    def _load_presets(self):
        if not os.path.exists(self.filepath):
            return self.default_presets()
        
        with open(self.filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
        
    def save_presets(self):
        with open(self.filepath, 'w', encoding='utf-8') as f:
            json.dump(self.preset_data, f, indent=4)

    def default_presets(self):
        return [
            {
                "id": 1,
                "PS_TEMP": 16,
                "PS_AQL": 0,
                "PS_DDL": 0,
                "PS_BRL": 0
            },
            {
                "id": 2,
                "PS_TEMP": 16.0,
                "PS_AQL": 0,
                "PS_DDL": 0,
                "PS_BRL": 0
            },
            {
                "id": 3,
                "PS_TEMP": 16.0,
                "PS_AQL": 0,
                "PS_DDL": 0,
                "PS_BRL": 0
            }
        ]
    
    def get(self, ps_id=None):
        target_id = ps_id if ps_id is not None else self.ps_id
        for preset in self.preset_data:
            if preset["id"] == target_id:
                return preset
        return None
        
    def get_all(self):
        return self.preset_data
    
    def set(self, ps_id, ps_data):
        for i, preset in enumerate(self.preset_data):
            if preset["id"] == ps_id:
                self.preset_data[i].update(ps_data)
                self.save_presets()
                return