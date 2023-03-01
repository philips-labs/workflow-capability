@echo off

:: Ask if demos should be served
set /p SERVE_DEMOS=Serve all of the demos? (y/n) [n]:

echo ---- Removing docker containers ----
for /f "usebackq tokens=*" %%a in (`docker container ls -aq`) do docker container stop %%a
for /f "usebackq tokens=*" %%a in (`docker container ls -aq`) do docker container rm %%a

echo ---- Starting Postgres ----
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=admin -d postgres
@timeout /t 5 /nobreak>nul

echo ---- Starting FHIR store ----
cd ..
docker run -d -p 8180:8080 -v "%cd%/fhir_jpa_config:/data" -e "--spring.config.location=file:///data/application.yaml" hapiproject/hapi:latest
cd new_scripts
:: get container id of this container
for /f "usebackq tokens=*" %%a in (`docker ps --filter "ancestor=hapiproject/hapi:latest" --format "{{.ID}}"`) do set CONTAINER_ID=%%a
@timeout /t 5 /nobreak>nul

echo --- Wait until HAPI FHIR has started ---
:: We wait by checking the last line of the logs of the container for the string "Started Application in"
:can_continue
for /f "usebackq tokens=*" %%a in (`docker logs --tail 1 %CONTAINER_ID%`) do set CONTAINER_LOG_LAST_LINE=%%a
echo %CONTAINER_LOG_LAST_LINE% | findstr /C:"Started Application in" 1>nul
if errorlevel 1 (
    @timeout /t 1 /nobreak>nul
    goto :can_continue
)

echo -- STARTING CAMUNDA --
cd ..
start call java -jar engine\camunda\target\camunda_engine-0.0.2-SNAPSHOT.jar ^& pause
cd new_scripts

echo -- STARTING CAREGIVER APP --
cd ..
start call python caregiver_application\main.py ^& pause
cd new_scripts

:: echo -- STARTING MANAGEMENT DASHBOARD --
:: cd ..
:: cd management_dashboard
:: start npm run start
:: cd ..
:: cd new_scripts

@timeout /t 10 /nobreak>nul

echo -- STARTING WFC --
cd ..
if "%SERVE_DEMOS%" == "y" (
    call java -jar workflow_capability_core\target\workflow_capability_core-0.0.2-SNAPSHOT.jar withAllDemos
) else (
    call java -jar workflow_capability_core\target\workflow_capability_core-0.0.2-SNAPSHOT.jar
)
cd new_scripts

pause