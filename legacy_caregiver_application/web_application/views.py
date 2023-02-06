import flask
import requests
import pathlib
import service_config as cfg
from flask import Blueprint, render_template, request
from cdr.cdr_fhir import Cdr

cdr = Cdr("http://127.0.0.1:8180/fhir", {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Cache-Control': 'no-cache'})

views = Blueprint('views', __name__)

@views.route('/')
def home():
    return render_template("home.html")

@views.route('/patients')
def patients():
    patientList = cdr.get_patients()
    return render_template("patients.html", patient_list=patientList, len = len(patientList))


@views.route("/patient/<id>")
def patient(id):
    current_patient = cdr.get_patient(id)
    name = ""
    care_plan_list = getPatientWorkflows(id)
    all_plan_defs = cdr.get_plan_definitions()
    if "name" in current_patient:
        name = current_patient["name"][0]["given"][0] + " " + current_patient["name"][0]["family"]
    print(all_plan_defs)
    return render_template("patient.html", patid = current_patient["id"], name = name, care_plan_list = care_plan_list, len = len(care_plan_list), all_plan_defs = all_plan_defs, all_len = len(all_plan_defs))


@views.route("/workflow/<id>")
def workflow(id):
    care_plan = cdr.get_care_plan(id)
    task_list = []
    if care_plan is None:
        print("Test")
        return patients()
    if "activity" in care_plan:
        for activity in care_plan["activity"]:
            print(activity["reference"]["reference"])
            task = cdr.get_resource(activity["reference"]["reference"])
            task_list.append(task)

    return render_template("workflow.html", id=id, task_list = task_list, len=len(task_list))


@views.route("/workflow/<id>/<taskid>/done")
def task_done(id,taskid):
    cdr.set_task_to_done(taskid)
    return workflow(id)


@views.route("/patient/apply", methods=['POST'])
def apply_workflow():
    json_return = cdr.apply_plan_definition(request.form['workflow'], request.form['patient'])
    return render_template("apply.html", workflow = request.form['workflow'], patient = request.form['patient'])



@views.route("/labview", methods=['GET', 'POST'])
def labview():
    patientList = cdr.get_patients()
    if flask.request.method == 'POST':
        category = request.form['category']
        code = request.form['code']
        patient = request.form['patient']
        value = request.form['value']
        unit = request.form['unit']
        json_return = cdr.create_observation(category, code, patient, value, unit)
    return render_template("labview.html", patient_list=patientList, len = len(patientList))


@views.route("/deploy", methods=['GET', 'POST'])
def deploy():
    success = False
    if flask.request.method == 'POST':
        file = request.files['file']
        myfiles = {'file': file}
        if pathlib.Path(request.files['file'].filename).suffix == ".bpmn" or pathlib.Path(request.files['file'].filename).suffix == ".bpm":
            msg = requests.post(cfg.wfc_url + "/DeployBpmn", files = myfiles)
            if msg.status_code == 200:
                success = True
        if pathlib.Path(request.files['file'].filename).suffix == ".dmn":
            msg = requests.post(cfg.wfc_url + "/DeployDmn", files=myfiles)
            print(msg.text)
            if msg.status_code == 200:
                success = True

    return render_template("deploy.html", success = success)

@views.route("/devtools")
def devtools():
    return render_template("devtools.html")


### USE WITH CARE
@views.route("/devtools/<exec>")
def devtoolsexec(exec):
    if exec == "del-cp":
        print("Deleting CarePlans")
        care_plans = cdr.get_care_plans()
        for care_plan in care_plans:
            response = requests.delete("http://localhost:8180/fhir/CarePlan/" + care_plan["id"])
    if exec == "del-pd":
        print("Deleting PlanDefintions")
        plan_defs = cdr.get_plan_definitions()
        for plan_def in plan_defs:
            response = requests.delete("http://localhost:8180/fhir/PlanDefinition/" + plan_def["id"])
    if exec == "del-ts":
        print("Deleting Tasks")
        tasks = cdr.get_tasks()
        for task in tasks:
            response = requests.delete("http://localhost:8180/fhir/Task/" + task["id"])
    return devtools()


def getPatientWorkflows(patientid):
    return cdr.get_care_plans("subject=Patient/" + patientid)