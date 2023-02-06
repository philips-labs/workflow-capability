# Scripts  

## Instructions  
Use the scripts as per the descriptions provided below. It is assumed that a Docker instance is running in the backround.  

## Description  
- **(01)** : Does (09),(04),(10),(11),(12),(13): _build_start_all_noDemos.bat
- **(02)** : Does (09),(04),(10),(11),(12),(14): _build_start_all_withAllDemos.bat
- **(03)** : Does (09),(04),(10),(11),(12),(15): _build_start_all_withSepsisV2Demos.bat
- **(04)** : Builds (and tests) Camunda, CA, MD, WFCC: build_all.bat
- **(05)** : Builds (without testing) Camunda, CA, MD, WFCC: build_all_noTesting.bat
- **(06)** : Cleans Camunda, WFCC: clean_apps.bat
- **(07)** : This readme: README.md
- **(08)** : Stops and removes ALL docker containers: remove_all_docker_containers.sh
- **(09)** : Does (08) and (16): remove_all_docker_containers_start_FHIR_store.sh
- **(10)** : Runs Camunda: run_camunda.bat
- **(11)** : Runs Caregiver Application: run_caregiver_app.bat
- **(12)** : Runs Management Dashboard: run_management_dashboard.bat
- **(13)** : Runs WFCC without initializing FHIR with demos: run_wfc_noDemos.bat
- **(14)** : Runs WFCC and initializes FHIR with all demos: run_wfc_withAllDemos.bat
- **(15)** : Runs WFCC and initializes FHIR with sepsis v2 demos: run_wfc_withSepsisv2Demos.bat
- **(16)** : Starts a docker container with HAPI-FHIR: start_FHIR_store.sh
- **(17)** : Builds (without testing) WFCC: build_wfc_noTesting.bat

## Glossary  
- **WFCC** : Workflow Capability Core  
- **CA** : Caregiver Application  
- **MD** : Management Dashboard  
- **Camunda** : Camunda workflow engine


