FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src

RUN mvn -B package --file pom.xml -DskipTests

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/workflow_capability_core-0.0.2-SNAPSHOT.jar /app/app.jar
EXPOSE 5003

# CMD ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
CMD ["java", "-Djava.net.preferIPv4Stack=true", "-Dserver.address=0.0.0.0", "-jar", "app.jar"]