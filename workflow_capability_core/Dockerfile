FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src
# Build the application
RUN mvn -B package --file pom.xml -DskipTests

# Stage 2: Setup the runtime container
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/workflow_capability_core-0.0.2-SNAPSHOT.jar /app/app.jar
EXPOSE 8080

CMD ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
