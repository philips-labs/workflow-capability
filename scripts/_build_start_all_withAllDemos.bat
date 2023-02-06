@echo off

echo ---- Removing docker containers and starting FHIR store... ----
start remove_all_docker_containers_start_FHIR_store.sh

echo Before building the apps, we should wait for some time while FHIR store starts...
timeout /t 25 /nobreak

call build_all.bat

start run_camunda.bat
start run_caregiver_app.bat
start run_management_dashboard.bat
start run_wfc_withAllDemos.bat