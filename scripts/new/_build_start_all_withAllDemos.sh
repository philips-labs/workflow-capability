#!/bin/bash

echo ---- Removing docker containers and starting FHIR store... ----
sh remove_all_docker_containers_start_FHIR_store.sh > logs/hapi.log &

echo Before building the apps, we should wait for some time while FHIR store starts...
sleep 120s
sh build_all.sh > logs/build.log

sh run_camunda.sh > logs/camunda.log &
sh run_caregiver_app.sh > logs/caregiver_app.log &
sh run_management_dashboard.sh > logs/management_dashboard.log &
sh run_wfc_withAllDemos.sh > logs/wfc.log &

python3 -mwebbrowser http://localhost:8080
python3 -mwebbrowser http://localhost:5000
python3 -mwebbrowser http://localhost:4200
python3 -mwebbrowser http://localhost:8180