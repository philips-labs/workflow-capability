@echo off


echo ------------- Now cleaning apps... -------------
cd ..


echo.
echo ---------- Camunda clean-script starting ----------
cd engine\camunda
call mvn clean
cd ..\..
echo ---------- Camunda clean-script finished ----------


echo.
echo ---------- WFC clean-script starting ----------
cd workflow_capability_core
call mvn clean
echo ---------- WFC clean-script finished ----------
cd ..


echo.
echo --------------- Apps cleaned! ---------------
timeout 3