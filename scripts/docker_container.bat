cd..
docker run -p 8180:8080 -v "%cd%/fhir_jpa_config:/data" -e "--spring.config.location=file:///data/application.yaml" hapiproject/hapi:latest