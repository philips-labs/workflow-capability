@echo off
set CURRENT_DIR=%cd%
set CURRENT_DIR_LENGTH=%CURRENT_DIR:~-12%
if not "%CURRENT_DIR_LENGTH%" == "\new_scripts" (
    echo.
    echo You are not in the correct directory. Please run this script from the root directory of the project.
    echo.
    exit
)

echo --------------- Now building apps... ---------------
cd ..

echo.
echo ---------- Camunda build-script starting ----------
cd engine\camunda
call mvn -B package --file pom.xml
cd ..\..
echo ---------- Camunda build-script finished ----------

echo.
echo ---------- Caregiver app build-script starting ----------
cd caregiver_application
call pip install -r requirements.txt
cd ..
echo ---------- Caregiver app build-script finished ----------

echo.
echo ---------- Management Dashboard build-script starting ----------
cd management_dashboard
call npm install
cd ..
echo ---------- Management Dashboard build-script finished ----------

echo.
echo ---------- WFC build-script starting ----------
cd workflow_capability_core
call mvn -B package --file pom.xml
cd ..
echo ---------- WFC build-script finished ----------

cd scripts
echo.
echo ----------------- Apps built! -----------------