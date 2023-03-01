@echo off


echo --------------- Now building apps... ---------------
cd ..


echo.
echo ---------- Camunda build-script starting ----------
cd engine\camunda
call mvn install -DskipTests --file pom.xml
cd ..\..
echo ---------- Camunda build-script finished ----------

echo.
echo ---------- Caregiver app build-script starting ----------
cd caregiver_application
pip install -r requirements.txt
cd ..
echo ---------- Caregiver app build-script finished ----------

echo.
echo ---------- Management Dashboard build-script starting ----------
cd management_dashboard
call npm install --legacy-peer-deps
cd ..
echo ---------- Management Dashboard build-script finished ----------

echo.
echo ---------- WFC build-script starting ----------
cd workflow_capability_core
call mvn install -DskipTests --file pom.xml
cd ..
echo ---------- WFC build-script finished ----------

cd scripts
echo.
echo ----------------- Apps built! -----------------
timeout 3