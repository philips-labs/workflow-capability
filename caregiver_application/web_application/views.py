from pickle import TRUE
import flask
import requests
import pathlib
import service_config as cfg
from flask import Blueprint, render_template, request, redirect
from cdr.cdr_fhir import Cdr
from flask import jsonify
cdr = Cdr("http://127.0.0.1:8180/fhir", {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Cache-Control': 'no-cache'})

views = Blueprint('views', __name__)


@views.route('/')
def home():
    return render_template("home.html")

@views.route('/register-new-patient', methods=['POST', 'GET'])
def register_new_patient():
    newPatient = {}
    if flask.request.method == 'GET':
        return render_template("register-new-patient.html")

    if flask.request.method == 'POST':
        newPatient = {"active": True,
                   "birthDate": request.form["birthdate"],
                   "name": [{"text": request.form["lastname"] + " " + request.form["firstname"],
                             "family": request.form["lastname"],
                             "given": [
                                 request.form["firstname"]
                             ]}],
                   "telecom": [
                       {
                           "system": "phone",
                           "value": request.form["phone_number"],
                           "use": "mobile",
                           "rank": 2
                       }]
                   }
        json_return = cdr.create_patient(newPatient)

        return render_template("register-new-patient.html", id=json_return["id"], success=True)


@views.route('/patients', methods=['GET'])
def patients():
    patientList = cdr.get_patients()
    return render_template("patients.html", patient_list=patientList, len=len(patientList))

@views.route("/patient/<id>", methods=['GET','POST'])
def patient(id):
    if flask.request.method == 'GET':        
            current_patient = cdr.get_patient(id)
            name = ""
            care_plan_list = getPatientWorkflows(id)
            all_plan_defs = cdr.get_plan_definitions()
            if "name" in current_patient:
                name = current_patient["name"][0]["given"][0] + " " + current_patient["name"][0]["family"]
            # print("all_plan_defs",all_plan_defs)
            return render_template("patient.html", patid=current_patient["id"], name=name, care_plan_list=care_plan_list,
                            len=len(care_plan_list), all_plan_defs=all_plan_defs, all_len=len(all_plan_defs))
    if flask.request.method == 'POST':
            json_return = cdr.apply_plan_definition(request.form['workflow'], request.form['patient'])
            print('workflow: ',request.form['workflow'])
            print("patient: ",request.form['patient'])
            return redirect('/patient/'+id)


@views.route("/workflow/<id>")
def workflow(id):
    care_plan = cdr.get_care_plan(id)   
    workflowName=""      
    task_list = []
    if care_plan is None:
        return patients()
    print("care_plan: ",care_plan)
    if "activity" in care_plan:
        #workflowName = care_plan["meta"].identifier[0].value
        workflowName = care_plan["identifier"][0]["value"]
        for activity in care_plan["activity"]:
            print(activity["reference"]["reference"])
            task = cdr.get_resource(activity["reference"]["reference"])
            print("task: ",task)
            task_list.append(task)
    
    patId=care_plan["subject"]["reference"].split("/",1)[1]

    current_patient = cdr.get_patient(patId)
    name = ""    
    if "name" in current_patient:
        name = current_patient["name"][0]["given"][0] + " " + current_patient["name"][0]["family"]
    return render_template("workflow.html", id=id, task_list=task_list, len=len(task_list),workflowName=workflowName, status=care_plan["status"], name=name, patId=patId)

@views.route("/workflow/<id>/<taskid>/done")
def task_done(id, taskid):
    cdr.set_task_to_done(taskid)
    return redirect('/workflow/'+id)

# get-observation added in a way that we wanted to fetch the observation value for a patient and observation code 
@views.route('/get-observation/<patient_id>/<observation_code>')
def get_observation(patient_id, observation_code):
    observations = cdr.get_observations_for_patient_and_code(patient_id, observation_code)
    # print("Observations: ", observations)
    if observations:
        most_recent = sorted(observations, key=lambda obs: obs.get('effectiveDateTime'), reverse=True)[0]      
        if 'valueQuantity' in most_recent:
            observation_value = most_recent['valueQuantity'].get('value', 'N/A')
        elif 'valueString' in most_recent:
            observation_value = most_recent.get('valueString', 'N/A')
        elif 'valueCodeableConcept' in most_recent:
            observation_value = most_recent['valueCodeableConcept']['coding'][0].get('code', 'N/A') 
        else:
            observation_value = 'Unsupported observation value type'
        last_updated = most_recent.get('effectiveDateTime')
        return jsonify(success=True, observationValue=observation_value, lastUpdated=last_updated)
    else:
        return jsonify(success=False)
    
@views.route("/workflow/<id>/<taskid>/observe", methods=['POST'])
def observation_add(id, taskid):
    observationCode = request.form['observationCode']
    observationValue = request.form['observationValue']
    obervationValueType = request.form['obervationValueType']
    valueQuantity_code = '{s}'      # It can also be an emoty {} based on the type of observation
    valueQuantity_unit = '{s}'      # It can also be an emoty {} based on the type of observation
   
    if(obervationValueType == 'quantity'):
        res = cdr.create_observation_quantity(observationCode,observationCode,request.form['patientId'],observationValue,valueQuantity_unit,valueQuantity_code)
    else:
        res = cdr.create_observation_string(observationCode,observationCode,request.form['patientId'],observationValue)
    observeDone = False
    if res["status"] == 'final':
        observeDone = True
    else :
        observeDone = False
    
    return redirect('/workflow/'+id+'?observeDone=' + str(observeDone) + '&taskid=' + str(taskid))


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
    return render_template("labview.html", patient_list=patientList, len=len(patientList))


@views.route("/deploy", methods=['GET', 'POST'])
def deploy():
    success = False
    if flask.request.method == 'POST':
        file = request.files['file']
        myfiles = {'file': file}
        if pathlib.Path(request.files['file'].filename).suffix == ".bpmn" or pathlib.Path(
                request.files['file'].filename).suffix == ".bpm":
            msg = requests.post(cfg.wfc_url + "/DeployBpmn", files=myfiles)
            if msg.status_code == 200:
                success = True
        if pathlib.Path(request.files['file'].filename).suffix == ".dmn":
            msg = requests.post(cfg.wfc_url + "/DeployDmn", files=myfiles)
            print(msg.text)
            if msg.status_code == 200:
                success = True

    return render_template("deploy.html", success=success)


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
