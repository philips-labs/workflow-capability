# HealthSuite Workflow Capability Core

## Build
- Change directory to the main folder of the app: `cd workflow_capability_core`
- Build the java project using maven: `mvn -B package --file pom.xml`

## Run
- Change Directory to the main folder of the app: `cd workflow_capability_core`  
- Run the created .jar file in the target folder:  `java -jar workflow_capability_core-0.0.2SNAPSHOT.jar <DEMOS>`, where <DEMOS> is:  
  - 'empty', if you do not want to add any demo data to FHIR  
  - 'withAllDemos', if you want to add all demo patients to FHIR  
  - 'withSepsisV2Demos', if you want to add all demo patients (compatible with sepsis v2 protocol) to FHIR  
  - 'withPreprocessingX withPreprocessingY <...>', if you want to add demo patients X and Y to FHIR 