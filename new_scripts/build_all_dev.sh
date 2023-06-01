#!/bin/bash
CURRENT_DIR=$(pwd)
CURRENT_DIR_LENGTH=${#CURRENT_DIR}
if [ "${CURRENT_DIR: -12}" != "/new_scripts" ]; then
    echo ""
    echo "You are not in the correct directory. Please run this script from the root directory of the project."
    echo ""
    exit 1
fi

echo "--------------- Now building apps... ---------------"
cd ..

echo ""
echo "---------- Camunda build-script starting ----------"
cd engine/camunda
mvn -B package --file pom.xml
cd ../..
echo "---------- Camunda build-script finished ----------"

echo ""
echo "---------- Caregiver app build-script starting ----------"
cd caregiver_application
pip install -r requirements.txt
cd ..
echo "---------- Caregiver app build-script finished ----------"

echo ""
echo "---------- Management Dashboard build-script starting ----------"
cd management_dashboard
npm install
cd ..
echo "---------- Management Dashboard build-script finished ----------"

echo ""
echo "---------- WFC build-script starting ----------"
cd workflow_capability_core
mvn -B package --file pom.xml
cd ..
echo "---------- WFC build-script finished ----------"

cd scripts
echo ""
echo "----------------- Apps built! -----------------"