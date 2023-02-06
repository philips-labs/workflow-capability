docker container stop $(docker container ls -aq)
docker container rm $(docker container ls -aq) --force
docker run -p 5432:5432 -e POSTGRES_PASSWORD=admin -d postgres
sleep 3
# Starting HAPI FHIR, script does not terminate
docker run -p 8180:8080 -v "/$(pwd)/../fhir_jpa_config:/data" -e "--spring.config.location=file:///data/application.yaml" hapiproject/hapi:latest
