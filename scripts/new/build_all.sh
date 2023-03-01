#!/bin/bash
C=$(tput setaf 4) #Blue
NC=$(tput sgr 0) # No Color
echo ${C}--------------- Now building apps... ---------------${NC}
cd ../..
echo ${C}---------- Camunda build-script starting ----------${NC}
cd engine/camunda
mvn -B package --file pom.xml
cd ../..
echo ${C}---------- Camunda build-script finished ----------${NC}

echo ${C}---------- Caregiver app build-script starting ----------${NC}
cd caregiver_application
pip install -r requirements.txt
cd ..
echo ${C}---------- Caregiver app build-script finished ----------${NC}

echo ${C}---------- Management Dashboard build-script starting ----------${NC}
cd management_dashboard
npm install --legacy-peer-deps
cd ..
echo ${C}---------- Management Dashboard build-script finished ----------${NC}

echo ${C}---------- WFC build-script starting ----------${NC}
cd workflow_capability_core
mvn -B package --file pom.xml
cd ..
echo ${C}---------- WFC build-script finished ----------${NC}
echo ${C}----------------- Apps built! -----------------${NC}