@echo off


cd ..

echo ---------- WFC build-script starting ----------
cd workflow_capability_core
call mvn install -DskipTests --file pom.xml
cd ..
echo ---------- WFC build-script finished ----------

cd scripts
timeout 3