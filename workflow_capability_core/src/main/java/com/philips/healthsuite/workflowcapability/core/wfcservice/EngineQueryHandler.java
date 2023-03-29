package com.philips.healthsuite.workflowcapability.core.wfcservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;

import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class EngineQueryHandler {
    HashMap<String, HashMap<String, String[]>> pendingRequests;
    Properties properties;


    public EngineQueryHandler() throws IOException {
        properties = new Properties();
        InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        pendingRequests = new HashMap<>();
    }


    public Resource getFhirResource(@NotNull String query, String returnMessage, String processID, String variableName) {
        FhirContext ctx = FhirContext.forR4();
        try {

            // Extract CRUD Operator
            String[] crudOperationSplit = query.split(":");
            if (crudOperationSplit.length != 2) {
                throw new IncorrectQueryException("Multiple : used");
            }
            String crudOperation = crudOperationSplit[0];
            query = crudOperationSplit[1];

            // Extract FHIR Resource
            String[] fhirResourceSplit = query.split("\\?");
            if (fhirResourceSplit.length != 2) {
                throw new IncorrectQueryException("Multiple ? used");
            }

            // Extract FHIR Query
            String fhirResource = fhirResourceSplit[0];
            query = fhirResourceSplit[1];

            IParser parser = ctx.newJsonParser();

            if (crudOperation.equals("FHIR(GET)")) {
                return getFhirObject(fhirResource, query, parser);
            }
            if (crudOperation.equals("FHIR(SUBSCRIBE)")) {
                subscribeToFhirObject(fhirResource, query, parser, processID, returnMessage, variableName);
            }
        } catch (IncorrectQueryException e) {
            System.out.println("Incorrect query, please use the format {CRUD Operation}:{FHIR Resource Type}?{FHIR Query} -> " + e);
        }
        return null;
    }

    public void makeFhirResource(String method, String resourceType, String query, @NotNull String data) {
        HttpRequestWithBody httpRequestWithBody = null;
        String request = properties.get("config.fhirUrl") + "/fhir/" + resourceType + "?" + query;
        if(method.equals("POST")) {
            httpRequestWithBody = Unirest.post(request);
        } else if (method.equals("PUT")) {
            httpRequestWithBody = Unirest.put(request);
        } else if (method.equals("PATCH")) {
            httpRequestWithBody = Unirest.patch(request);
        } else if (method.equals("DELETE")) {
            httpRequestWithBody = Unirest.delete(request);
        }

        if (httpRequestWithBody != null) {
            httpRequestWithBody.header("Content-Type", "application/json+fhir")
                    .body(data)
                    .asJson();
        }
        else {
            System.out.println("Incorrect method, please use POST, PUT, PATCH or DELETE");
        }
    }

    private void subscribeToFhirObject(String fhirResource, String query, IParser parser, String processID, String returnMessage, String variableName) {
        if (!pendingRequests.containsKey(processID)) {
            pendingRequests.put(processID, new HashMap<>());
        }
        Subscription subscription = new Subscription();
        subscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
        subscription.setReason("Receive Task needs FHIR Store Value");
        subscription.setCriteria(fhirResource + "?" + query);
        Subscription.SubscriptionChannelComponent hook = new Subscription.SubscriptionChannelComponent();
        hook.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        hook.setEndpoint(properties.get("config.wfcUrl") + "/OnRequestChange/" + processID + "/" + returnMessage + "/" + variableName);
        List<StringType> headers = new ArrayList<StringType>();
        headers.add(new StringType("returnMessage: " + returnMessage));
        headers.add(new StringType("processID: " + processID));
        headers.add(new StringType("variableName: " + variableName));
        hook.setHeader(headers);

        subscription.setChannel(hook);


        // Send Subscription to FHIR
        HttpResponse<JsonNode> subResponse = Unirest.post(properties.get("config.fhirUrl") + "/fhir/Subscription"
                )
                .header("Content-Type", "application/json+fhir")
                .body(parser.encodeResourceToString(subscription))
                .asJson();

        pendingRequests.get(processID).put(returnMessage, new String[]{fhirResource + "?" + query, subResponse.getBody().getObject().getString("id")});

    }


    private Resource getFhirObject(String fhirResource, String query, IParser parser) {
        HttpResponse<JsonNode> httpResponse = Unirest.get(properties.get("config.fhirUrl") + "/fhir/" +
                        fhirResource + "?" + query)
                .asJson();

        Bundle bundle = (Bundle) parser.parseResource(httpResponse.getBody().toString());
        Resource resource = null;
        if (bundle.hasEntry()) {
            resource = new FhirDataResources(properties.get("config.fhirUrl") + "/fhir/").getFirstBundleEntry(bundle);
            System.out.println("RESOURCE: " + resource.getId());
        }
        return resource;
    }
}


class IncorrectQueryException extends Exception {
    IncorrectQueryException(String errorMessage) {
        super(errorMessage);
    }
}