from fhir.resources.fhirtypes import Code
from fhir.resources.reference import Reference
from fhir.resources.task import Task
from fhir.resources.activitydefinition import ActivityDefinition
from fhir.resources.careplan import CarePlan
from fhir.resources.subscription import Subscription
from fhir.resources.patient import Patient

import requests
import urllib
import json



def _same_end_point(c1, c2):
    try:
        return c1["channel"]["endpoint"] == c2["channel"]["endpoint"]
    except Exception:
        return False


class Cdr:
    """ class that manages interaction with the Clinical Data Repository,
    i.e., the FHIR server.
    """
    
    def __init__(self, base_url, default_header):
        self.base_url = base_url
        self.header = default_header


    def get_resource(self, res_url):
        print("Load resource from ", res_url)
        # prefix with fhir base url when not fully qualified url given
        if "://" not in res_url:
            res_url = self.base_url + "/" + res_url
        response = requests.get(res_url, headers=self.header)
        res = None
        if response:
            res = json.loads(response.text)
            if res["resourceType"] == "Bundle":
                res = res["entry"][0]["resource"] if res["total"] == 1 else None
        return res


    def remove_tag(self, res_url, tag_values):
        parameters = {
            "resourceType" : "Parameters",
            "parameter" : [{
                "name" : "meta",
                "valueMeta" : {
                    "tag": tag_values
                }
            }]      
        }
        response = requests.post(self.base_url + "/" + res_url + "/$meta-delete", json=parameters, headers=self.header)
        return response.ok


    def update_resource(self, resource):
        url = type(resource)+"/"+resource.id
        print("update resource on: ", url)


    def create_activity_definition(self, act_def_props: dict):
        ad = ActivityDefinition(act_def_props)
        response = requests.post(self.base_url+"/ActivityDefinition", data=str(ad.as_json()), headers=self.header)
        return json.loads(response.text) if response else None


    def get_activity_definitions(self, title=None):
        ad_url = "/ActivityDefinition?context-type=ClinicalWorkflowCapability"
        if title:
            ad_url += "&title=" + title
        response = requests.get(self.base_url+ad_url, headers=self.header)
        if response:
            bundle = json.loads(response.text)
            if "entry" in bundle:
                return bundle["entry"]
            return []
        return None

    def apply_plan_definition(self, plan_def_id, patient_id):
        plan_definition = self.get_resource('PlanDefinition/' + plan_def_id)

        name = plan_definition.get("name")
        if name == None:
            name = ""

        if plan_definition:
            jsoncp = {
                'resourceType': 'CarePlan',
                'name': name,
                'identifier': plan_definition["identifier"],
                'instantiatesCanonical': ["PlanDefinition/" + plan_definition["id"]],
                'status': 'draft',
                'intent': 'plan',
                'category': {
                    "coding": [
                        {
                            "code": "WorkflowCapability"
                        }
                    ]
                },
                'subject': { 'resourceType': 'Reference',
                             'reference': 'Patient/' + patient_id},
                'activity': []
            }
            return self.create_care_plan(jsoncp)
        return None

    def create_observation(self, code, patient, value, unit):
        jsoncp = {
            'resourceType': 'Observation'
        }

    def create_observation(self, category, code, patient, value, unit):
        jsoncp = {
            'resourceType': 'Observation',
            'status': 'final',
            'category': [ {
            'system': "http://terminology.hl7.org/CodeSystem/observation-category",
            'code': category,
            'display': category
            } ],
            'code': {
                'coding': [{
                    'system': 'http://loinc.org',
                    'code': code,
                    'display': code
                }],
                'text': code
            },
            'subject': {
                'reference': 'Patient/' + patient
            },
            'valueQuantity': {
                'value': value,
                'unit': unit,
                'system': 'http://unitsofmeasure.org',
                'code': unit
            }
        }
        response = requests.post(self.base_url + "/Observation", data=str(jsoncp), headers=self.header)
        if not response:
            print("Observation create failed ", response.status_code, "  - ", response.text)
            return None
        return json.loads(response.text)

    def create_care_plan(self, cp):
        response = requests.post(self.base_url+"/CarePlan", data=str(cp), headers=self.header)
        if not response:
            print("CarePlan create failed ", response.status_code, "  - ", response.text)
            return None
        return json.loads(response.text) 


    def get_care_plan(self, care_plan_id):
        try:
            cp_url = "/CarePlan/" + care_plan_id
            response = requests.get(self.base_url+cp_url, headers=self.header)
            return json.loads(response.text) if response else None
        except:
            return None

    def get_task(self, task_id):
        try:
            cp_url = "/Task/" + task_id
            response = requests.get(self.base_url+cp_url, headers=self.header)
            return json.loads(response.text) if response else None
        except:
            return None


    def get_care_plans(self, filter=None):
        return self._get_bundle_entries(self._get_filter_url("CarePlan", filter))

    def get_plan_definitions(self, filter=None):
        return self._get_bundle_entries(self._get_filter_url("PlanDefinition", filter))

    def get_patients(self, filter=None):
        return self._get_bundle_entries(self._get_filter_url("Patient", filter))

    def update_care_plan(self, care_plan_id, cp_props):
        try:
            cp_url = "/CarePlan/" + str(care_plan_id)
            if "id" not in cp_props:
                cp_props["id"] = care_plan_id
            cp = CarePlan(cp_props)
            response = requests.put(self.base_url+cp_url, data=str(cp.as_json()), headers=self.header)
            return json.loads(response.text) if response else None
        except Exception:
            return None


    def set_task_to_done(self, taskid):
        task = self.get_task(taskid)
        task['status'] = 'completed'
        print(task)
        json = self.update_task(taskid, task)
        print(json)
        return None

    def create_task(self, cp):
        response = requests.post(self.base_url+"/Task", data=str(cp), headers=self.header)
        if not response:
            print("Task create failed ", response.status_code, "  - ", response.text)
            return None
        return json.loads(response.text)


    def get_tasks(self, filter=None):
        return self._get_bundle_entries(self._get_filter_url("Task", filter))


    def update_task(self, task_id: int, task_props: dict):
        task_url = "/Task/"+str(task_id)
        if "id" not in task_props:
            task_props["id"] = task_id
        response = requests.put(self.base_url+task_url, data=str(task_props), headers=self.header)
        return json.loads(response.text) if response else None


    def get_subscriptions(self, filter=None):
        return self._get_bundle_entries(self._get_filter_url("Subscription", filter))


    def remove_subscription(self, subscript_id):
        sub_url = "/Subscription/" + subscript_id
        response = requests.delete(self.base_url+sub_url, headers=self.header)
        return True if response else False


    def create_subscription(self, subs_props: dict):
        # No duplicates
        existing_sub = self.get_subscriptions("criteria="+urllib.parse.quote(subs_props["criteria"]))
        for es in existing_sub:
            if _same_end_point(es, subs_props):
                return es
        
        #create
        sub_url = "/Subscription"
        sub = Subscription(jsondict=subs_props)
        response = requests.post(self.base_url+sub_url, data=str(sub.as_json()), headers=self.header)
        return json.loads(response.text) if response else None


    def get_patient(self, patient_id):
        try:
            if not patient_id.startswith("Patient/"):
                patient_id = "Patient/"+patient_id
            response = requests.get(f"{self.base_url}/{patient_id}", headers=self.header)
            return json.loads(response.text) if response else None
        except:
            return None

    def get_observation(self, obsv_id):
        try:
            if not obsv_id.startswith("Observation/"):
                obsv_id = "Observation/" + obsv_id
            response = requests.get(f"{self.base_url}/{obsv_id}", headers=self.header)
            return json.loads(response.text) if response else None
        except:
            return None


    def find_patient(self, patient_search_filter):
        return self.get_resource("Patient?" + patient_search_filter)


    def create_patient(self, patient_props: dict):
        patient_url = "/Patient"
        patient = Patient(patient_props)
        response = requests.post(self.base_url + patient_url, data=str(patient.as_json()), headers=self.header)
        return json.loads(response.text) if response else None


    def _get_filter_url(self, resource_type, filter):
        url = f"{self.base_url}/{resource_type}"
        if filter is not None:
            url += "?"+filter
        return url


    def _get_bundle_entries(self, url):
        result = []
        response = requests.get(url, headers=self.header)
        if response:
            bundle = json.loads(response.text)
            while (bundle is not None) and ("entry" in bundle):
                result.extend([t["resource"] for t in bundle["entry"]])
                bundle = self._get_next_page(bundle)
        return result


    def _get_next_page(self, bundle):
        if (bundle is not None) and ("link" in bundle):
            for l in bundle["link"]:
                if l["relation"]=="next":
                    response=requests.get(l["url"], headers=self.header)
                    if response:
                        return json.loads(response.text)
        return None

            
