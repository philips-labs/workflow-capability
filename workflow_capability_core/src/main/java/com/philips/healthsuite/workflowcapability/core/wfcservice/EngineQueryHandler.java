package com.philips.healthsuite.workflowcapability.core.wfcservice;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;
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
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        pendingRequests = new HashMap<>();
    }

    public Resource getFhirResource(@NotNull String query, String returnMessage, String processID, String variableName, String taskIdentifier) throws IOException {
        FhirContext ctx = FhirContext.forR4();
        System.out.println("Get params: " + query + ": " + returnMessage + " : " + processID + " : " + variableName + " : " + taskIdentifier);
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
            Resource resource = null;
            if (crudOperation.equals("FHIR(GET)")) {
                resource = getFhirObject(fhirResource, query, parser);
                if (resource != null) {
                    return resource;
                }

            }
            if (crudOperation.equals("FHIR(SUBSCRIBE)")) {
                subscribeToFhirObject(fhirResource, query, parser, processID, returnMessage, variableName, taskIdentifier);
            }
            if (crudOperation.equals("FHIR(FETCH)")) {
                 resource = getFhirObject(fhirResource, query, parser);
                if (resource != null) {
                    System.out.println("Resource from fetch condition: ");
                    return resource;
                }
                else
                subscribeToFhirObject(fhirResource, query, parser, processID, returnMessage, variableName, taskIdentifier);
            }

        } catch (IncorrectQueryException e) {
            System.out.println("Incorrect query, please use the format {CRUD Operation}:{FHIR Resource Type}?{FHIR Query} -> " + e);
        }
        return null;
    }

    private void subscribeToFhirObject(String fhirResource, String query, IParser parser, String processID,
        String returnMessage, String variableName, String taskIdentifier) {
        if (!pendingRequests.containsKey(processID)) {
            pendingRequests.put(processID, new HashMap<>());
        }
        Subscription subscription = new Subscription();
        subscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
        subscription.setReason("Task needs FHIR Store Value");
        subscription.setCriteria(fhirResource + "?" + query);
        Subscription.SubscriptionChannelComponent hook = new Subscription.SubscriptionChannelComponent();
        hook.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        hook.setEndpoint(properties.get("config.wfcUrl") + "/OnRequestChange/" + processID + "/" + returnMessage + "/"
                + variableName + "/" + taskIdentifier);
        List<StringType> headers = new ArrayList<StringType>();
        headers.add(new StringType("returnMessage: " + returnMessage));
        headers.add(new StringType("processID: " + processID));
        headers.add(new StringType("variableName: " + variableName));
        headers.add(new StringType("taskIdentifier: " + taskIdentifier));
        hook.setHeader(headers);

        subscription.setChannel(hook);

        HttpResponse<JsonNode> subResponse = Unirest.post(properties.get("config.fhirUrl") + "/fhir/Subscription")
                .header("Content-Type", "application/json+fhir")
                .body(parser.encodeResourceToString(subscription))
                .asJson();

        pendingRequests.get(processID).put(returnMessage,
                new String[] { fhirResource + "?" + query, subResponse.getBody().getObject().getString("id") });
    }
    public Resource getFhirObject(String fhirResource, String query, IParser parser) {
        String baseFhirUrl = properties.get("config.fhirUrl") + "/fhir/";
        String requestUrl = baseFhirUrl + fhirResource + "?" + query;
        System.out.println("Getting FHIR Resource from: " + requestUrl);
        int maxRetries = 5;
        int retryDelayMillis = 1000;
        
        for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
            try {
                HttpResponse<JsonNode> httpResponse = Unirest.get(requestUrl).asJson();
                
                if (httpResponse.isSuccess()) {
                    Bundle bundle = (Bundle) parser.parseResource(httpResponse.getBody().toString());
                    if (bundle.hasEntry()) {
                        return new FhirDataResources(baseFhirUrl).getFirstBundleEntry(bundle);
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception occurred while fetching FHIR resource (retry " + retryCount + "): " + e.getMessage());
            }
            
            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                System.err.println("Thread sleep interrupted: " + e.getMessage());
            }
            
            retryDelayMillis *= 2; // Exponential backoff
        }
        
        System.err.println("Failed to fetch FHIR resource after " + maxRetries + " retries.");
        return null;
    }
    public class IncorrectQueryException extends Exception {
        public IncorrectQueryException(String message) {
            super(message);
        }
    }
}