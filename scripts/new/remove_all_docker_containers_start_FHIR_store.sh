#!/bin/bash

docker container stop $(docker container ls -aq)
docker container rm $(docker container ls -aq)
docker run -p 8180:8080 -e hapi.fhir.subscription.resthook_enabled=true hapiproject/hapi:latest