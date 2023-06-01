cd..
docker run -p 5432:5432 -e POSTGRES_PASSWORD=admin -d postgres
timeout /T 5
docker run -p 8180:8080 -v "%cd%/fhir_jpa_config:/data" -e "--spring.config.location=file:///data/application.yaml" hapiproject/hapi:latest