# Workflow Capability
This is the Workflow Capability prototype. It is a tool to be used by caregivers and hospital management
in order to (among others) start, control, and view the status of medical protocol workflows of patients. It currently supports custom implementations of the following medical protocols:
- Sepsis diagnosis and treatment plan.  
- Blood Loss guidance example for an ICU use case. 

## Contents
- [Workflow Capability](#hs-workflow-capability)
  - [Contents](#contents)
  - [Modules](#modules)
  - [Installation](#installation)
    - [Workflow Capability dependencies](#workflow-capability-dependencies)
    - [Caregiver Application dependencies](#caregiver-application-dependencies)
    - [Management Dashboard dependencies](#management-dashboard-dependencies)
    - [Workflow Engine dependencies](#workflow-engine-dependencies)
    - [Other dependencies](#other-dependencies)
  - [Execution](#execution)
    - [Execution using scripts](#execution-using-scripts)
      - [- Run example](#--run-example)
    - [Execution using commands](#execution-using-commands)
    - [Demos](#demos)
  - [Configuration](#configuration)
  - [Additional information](#additional-information)


## Modules
This project consists of five modules:
1. **Workflow Capability Core (wfc):** Provides services, preprocessing and model handling and conversions. 
2. **Caregiver Application (ca):** Caregivers can use this app to start and control care protocols for patients.
3. **Management Dashboard (md):** Hospital management can use this app to view statistics about patients and protocols.
4. **Workflow Engine (we):** The app used to execute the BPMN & DMN workflows. The currently used one is Camunda.
5. **FHIR Store:** This is the FHIR database used to store patient and workflow data. We use the existing 'Hapi-FHIR' implementation.
6. **PULSE-Physiology-Engine_FHIR**  The module used to simulate vital sign and sends the values to FHIR

### Architecture
The architecture to these models can be found here: [docs/Architecture.md](docs/Architecture.md)


## Installation

### Workflow Capability dependencies
- Minimal version: Java version 17. OpenJDK 17 is recommended and can be found [here](https://openjdk.java.net/projects/jdk/17/).
- Maven ([instructions](https://maven.apache.org/install.html#))

### Caregiver Application dependencies
- Python 3 ([download](https://www.python.org/downloads/)).
- Pip ([download](https://pip.pypa.io/en/stable/installing/)).
- Flask (run: `pip install Flask`)  

### Management Dashboard dependencies  
- Node.js  
- npm  

### Other dependencies
- Docker. The image required is `docker pull hapiproject/hapi:latest`. The FHIR Server is assumed to be run on port: `8180`

## Execution  
### Execution using docker-compose
The HAPI FHIR, the camunda engine and the workflow capability core can build and run using the compose file `docker-compose.yml`, but since the workflow capability core runs after the HAPI FHIR is started, it may fail to run, so manual runing on the docker itslef is mandatory.
For Pulse-Physiology_FHIR check the README ` Pulse-Physiology_FHIR\README.md` file and the Caregiver App `caregiver_application\README.md` file.

### Execution using scripts
Note: All sh Scripts have been made for bash
In order to facilitate easier cleaning, building, and execution of the applications, various scripts were created. They are placed in the 'scripts' folder. 
For more information on how to use the scripts, see `scripts/README.md`. 

#### - Run example    
1. Go to directory 'scripts' (`cd scripts`).  
2. Ensure you have a running docker instance.
3. Execute 'remove_all_docker_containers_start_FHIR_store.sh'. After it "stabilizes", go to step 3.
4. Execute 'build_all.bat'. Now, all applications will be built and tested.
5. Execute the following (in parallel):
    - run_camunda.bat (Camunda will start).
    - run_caregiver_app.bat (Caregiver Application will start).
    - run_wfc_withDemos.bat (Workflow Capability Core will start and demo patients will be added to FHIR).

### Execution using commands  
- For FHIR store: Run a Hapi FHIR instance (port 8180): `docker run -p 8180:8080 -e hapi.fhir.subscription.resthook_enabled=true hapiproject/hapi:latest`
- For the other modules: Use the instructions provided in the READMEs of [WCC](/workflow_capability_core), CA, MD, and WE.

### Demos  
Regarding Sepsis v2 models, the app can be run with the following demo patient, or a newly registered one:
- **(New patient)**: The patient's journey depends on the observation values entered by the user on-demand.
- **Joe the Fifth**: This patient goes to surgery because of bad CT results.
- **Joe the Sixth**: This patient is not sober and will be discharged immediately.
- **Joe the Seventh**: This patient is sober but does not have bad physiological measurements. Therefore, he will be discharged after initial tests.
- **Joe the Eighth**: This patient has sepsis but does not need surgery. He will get a prescription.
- **Joe the Ninth**: This patient goes to surgery because of bad blood results.
- **Joe the Tenth**: This patient goes to surgery because of bad MRI results.

## Configuration
The service ports and the service references can be set at the following locations:  
1. **Workflow Capability Core**: `workflow_capability_core/src/main/resources/application.properties`   
2. **Caregiver Application**: `caregiver_application/service_config.py`  
3. **Management Dashboard**: `management_dashboard/package.json`, `management_dashboard\src\environments\environment.ts`
4. **Workflow Engine**: `engine/camunda/src/main/resources/`  
 * If you are using docker, use http://host.docker.internal:port in each configration files
## Additional information
- This prototype is based on BPMN and DMN models, and shows an architecture that makes Vendor Replacement obtainable.
- The application is an example application that uses the FHIR store, but can be exchanged with an arbitrary application. The application and Workflow Capability part of the system, can be used independent of eachother.

#### Infromation about adding new feature
- Please check in feature folder on Pulse-physiology_FHIR `README.md` and ` ppe_fhir.ipynb`.
