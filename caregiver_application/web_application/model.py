import random
import logging
import requests
import random
from datetime import datetime

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

FHIR_SERVER_URL = "http://127.0.0.1:8180/fhir"
CAMUNDA_URL = "http://localhost:8080/engine-rest"
class PhysiologicalModel:
    @staticmethod
    def send_observation(observation_data):
        """Send an observation to the FHIR server and return the observation ID."""
        response = requests.post(f"{FHIR_SERVER_URL}/Observation", json=observation_data, headers={"Content-Type": "application/fhir+json"})
        if response.status_code == 201:
            observation_id = response.json()["id"]
            logging.info(f"Physiological data observation added successfully: {observation_data['code']['coding'][0]['display']} with ID {observation_id} and Value: {observation_data['valueQuantity']['value']}")
            return observation_id
        else:
            logging.error(f"Failed to add observation {observation_data['code']['coding'][0]['display']}, status code: {response.status_code}, reason: {response.text}")
            return None
     
    @staticmethod
    def get_uncompleted_tasks(patient_id):
        careplan_url = f"{FHIR_SERVER_URL}/CarePlan?subject=Patient/{patient_id}"
        care_plan_response = requests.get(careplan_url, headers={"Content-Type": "application/fhir+json"})
        uncompleted_tasks = []

        if care_plan_response.status_code == 200:
            care_plan_data = care_plan_response.json()
            for entry in care_plan_data.get("entry", []):
                care_plan = entry["resource"]
                if "activity" in care_plan:
                    for activity in care_plan["activity"]:
                        if "reference" in activity:
                            task_url = f"{FHIR_SERVER_URL}/{activity['reference']['reference']}"
                            task_response = requests.get(task_url, headers={"Content-Type": "application/fhir+json"})
                            if task_response.status_code == 200:
                                task = task_response.json()
                                if task["status"] != "completed":
                                    uncompleted_tasks.append(task["id"])
                            else:
                                logging.error(f"Failed to get task, status code: {task_response.status_code}, reason: {task_response.text}")
        else:
            logging.error(f"Failed to get care plans for patient {patient_id}, status code: {care_plan_response.status_code}, reason: {care_plan_response.text}")

        if uncompleted_tasks:
            logging.info(f"Uncompleted task IDs for patient {patient_id}: {uncompleted_tasks}")
        else:
            logging.info(f"No uncompleted tasks found for patient {patient_id}")
        
        return uncompleted_tasks
   
    @staticmethod
    def update_fhir_task_status(task_id, new_status):
        url = f"{FHIR_SERVER_URL}/Task/{task_id}"
        data = {
            "resourceType": "Task",
            "id": task_id,
            "status": new_status
        }
        response = requests.patch(url, json=data, headers={"Content-Type": "application/fhir+json"})
        if response.status_code in [200, 204]:
            logging.info(f"FHIR Task {task_id} status updated to '{new_status}' successfully")
        else:
            logging.error(f"Failed to update FHIR Task {task_id} status, response code: {response.status_code}, reason: {response.json()}")
    @staticmethod
    def create_generic_observation(patient_id, loinc_code, display, value, unit, system="http://unitsofmeasure.org", code=None):
        """Create a generic observation data structure."""
        observation_data = {
            "resourceType": "Observation",
            "status": "final",
            "code": {
                "coding": [{"system": "http://loinc.org", "code": loinc_code, "display": display}]
            },
            "subject": {"reference": f"Patient/{patient_id}"},
            "valueQuantity": {"value": value, "unit": unit, "system": system}
        }
        if code:  # Optionally add a code
            observation_data["valueQuantity"]["code"] = code
        return observation_data
    
    @staticmethod
    def simulate_patient_data(patient_id):
        """Simulate sending physiological data for a patient and update tasks and observations."""
        observations = [("11331-6", "Sobriety Level", random.randint(1, 20), "score"),
            ("8867-4", "Heart rate", random.randint(60, 100), "beats/minute"),
            ("9279-1", "Respiratory rate", random.randint(12, 20), "breaths/minute"),
            ("8310-5", "Body temperature", random.randint(97, 99), "Fahrenheit", "http://unitsofmeasure.org", "degF"),
            ("8480-6", "Systolic blood pressure", random.randint(100, 140), "mmHg"),
            ("8462-4", "Diastolic blood pressure", random.randint(60, 90), "mmHg"),
        ]

        for loinc_code, display, value, unit, *optional in observations:
            system, code = optional if optional else ("http://unitsofmeasure.org", None)
            observation_data = PhysiologicalModel.create_generic_observation(patient_id, loinc_code, display, value, unit, system, code)
            observation_id = PhysiologicalModel.send_observation(observation_data)
            if observation_id: 
                task_ids = PhysiologicalModel.get_uncompleted_tasks(patient_id)
                if not task_ids:
                    logging.info(f"No uncompleted tasks found for patient {patient_id}")
                    return None

        return observations[1][2], observations[2][2] # Return heart rate and respiratory rate