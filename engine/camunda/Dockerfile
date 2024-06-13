FROM maven:3.8.6-openjdk-11 as builder
WORKDIR /app
COPY . /app
RUN mvn -B package --file pom.xml

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /app/target/camunda_engine-0.0.2-SNAPSHOT.jar /app
CMD ["java", "-jar", "camunda_engine-0.0.2-SNAPSHOT.jar"]
