# Camunda workflow engine

## Build
- Change directory to the main folder of Camunda engine: `cd engine/camunda`
- Build the java project using maven:  `mvn -B package --file pom.xml`
	
## Run
- Change directory to the main folder of Camunda engine:  `cd engine/camunda`
- Run the created .jar file in the target folder:  `java -jar target/camunda_engine-0.0.2-SNAPSHOT.jar`  
- Access Camunda web interface at: http://localhost:8080/. The login credentials are:  
	-- *User Name:* admin  
	-- *Password:* geheim