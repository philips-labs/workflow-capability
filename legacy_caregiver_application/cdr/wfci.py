from fhir.resources.careplan import CarePlan, CarePlanActivity


def find_tag(resource, system, code=None):
    if ("meta" in resource) and ("tag" in resource["meta"]):
        for tag in resource["meta"]['tag']:
            if tag["system"] == system and ((code is None) or (tag["code"] == code)):
                return tag
    return None


def is_care_plan_representing_workflow(care_plan):
    try:
        return care_plan["category"][0]["coding"][0]["code"] == "ClinicalWorkflowCapability"
    except Exception:
        return False


def is_new_workflow_request(care_plan):
    try:
        if is_care_plan_representing_workflow(care_plan):
            tag = find_tag(care_plan, "CWC.workflow.request", "activate workflow") 
            return tag is not None
    except Exception:
        pass
    return False


def abort_plan(care_plan):
    care_plan["status"] = "entered-in-error"
    return care_plan


def close_plan(care_plan):
    care_plan["status"] = "completed"
    return care_plan


def get_workflow_case_instance_id(care_plan):
    try:
        tag = find_tag(care_plan, "CWC.workflow.instance")
        if (tag is not None):
            return tag["code"]
    except Exception:
        pass
    return None


def activate_plan(care_plan, case_id):

    tag = find_tag(care_plan, "CWC.workflow.request", "activate workflow") 
    if tag is not None:
        tag["system"] = "CWC.workflow.instance"
        tag["code"] = case_id

    cp = CarePlan(care_plan)
    cp.activity = [
        CarePlanActivity({
            "detail": {
                "status": "in-progress",
                "kind": "Task",
                "description": case_id,
                "code" : {
                    "coding" : [ { "code": case_id } ]
                }
            }
        })
    ]
    return cp.as_json()


def get_workflow_name(cdr, care_plan):
    try:
        if is_care_plan_representing_workflow(care_plan):
            ad = cdr.get_resource(care_plan["instantiatesCanonical"][0])
            return ad["name"]
    except Exception:
        return None


def get_workflow_subject_id(cdr, care_plan):
    try:
        if is_care_plan_representing_workflow(care_plan):
            return cdr.get_resource(care_plan["subject"]["reference"])["id"]
    except Exception:
        return None


def get_workflow_main_task_order(care_plan):
    try:
        if is_care_plan_representing_workflow(care_plan):
            return [act["reference"]["reference"] for act in care_plan["activity"]]
    except Exception:
        pass
    return None


def get_task_input_param(task, param_type, value_type="valueString"):
    if "input" in task:
        for ip in task["input"]:
            if ip["type"] == param_type:
                return ip[value_type]
    return None


def remove_task_input_param(task, param_type):
    if "input" in task:
        for ip in task["input"]:
            if ip["type"] == param_type:
                task["input"].remove(ip)
                return True
    return False


def set_task_input_param(task, param_type, param_value, value_type = "valueString"):
    remove_task_input_param(task, param_type)
    ip = { "type": param_type, value_type: param_value }
    if "input" not in task:
        task["input"] = [ ip ]
    else:
        task["input"].append(ip)
    return True

