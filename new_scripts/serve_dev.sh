#!/bin/bash

# Ask if demos should be served
read -p "Serve all of the demos? (y/n) [n]: " SERVE_DEMOS

echo "---- Removing docker containers ----"
docker container stop $(docker container ls -aq)
docker container rm $(docker container ls -aq)

echo "---- Starting Postgres ----"
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=admin postgres
sleep 5

echo "---- Starting FHIR store ----"
docker run -d -p 8180:8080 -v "/$(pwd)/../fhir_jpa_config:/data" -e "--spring.config.location=file:///data/application.yaml" hapiproject/hapi:latest
# get container id of this container
CONTAINER_ID=$(docker ps --filter "ancestor=hapiproject/hapi:latest" --format "{{.ID}}")
sleep 5

echo "--- Wait until HAPI FHIR has started ---"
# We wait by checking the last line of the logs of the container for the string "Started Application in"
while true; do
  CONTAINER_LOG_LAST_LINE=$(docker logs --tail 1 $CONTAINER_ID)
  if [[ $CONTAINER_LOG_LAST_LINE == *"Started Application in"* ]]; then
    break
  fi
  sleep 1
done

echo "-- STARTING CAMUNDA --"
cd ..
bash -eo pipefail -c "java -jar engine/camunda/target/camunda_engine-0.0.2-SNAPSHOT.jar | awk '{print \"[CAM] \"\$0}'" &
cd new_scripts

echo "-- STARTING CAREGIVER APP --"
cd ..
bash -eo pipefail -c "python caregiver_application/main.py | awk '{print \"[CGA] \"\$0}'" &
cd new_scripts

# echo "-- STARTING MANAGEMENT DASHBOARD --"
# cd ..
# cd management_dashboard
# npm run start &
# cd ..
# cd new_scripts

sleep 10

echo "-- STARTING WFC --"
cd ..
if [ "$SERVE_DEMOS" == "y" ]; then
  bash -eo pipefail -c "java -jar workflow_capability_core/target/workflow_capability_core-0.0.2-SNAPSHOT.jar withAllDemos | awk '{print \"[WFC] \"\$0}'" &
else
  bash -eo pipefail -c "java -jar workflow_capability_core/target/workflow_capability_core-0.0.2-SNAPSHOT.jar | awk '{print \"[WFC] \"\$0}'" &
fi
cd new_scripts

read -n 1 -s -r -p "Press any key to continue..."