package com.philips.healthsuite.workflowcapability.core.wfcservice;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.elasticsearch.common.ParsingException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.jetbrains.annotations.NotNull;

import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

public class EngineQueryHandler {
    HashMap<String, HashMap<String, String[]>> pendingRequests;
    Properties properties;
    Logger logger = Logger.getLogger(EngineQueryHandler.class.getName());

    public EngineQueryHandler() throws IOException {
        properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        pendingRequests = new HashMap<>();
    }

    /*
     * After extracting CRUD operator, the following cencepts are introduced by the workflow capability:
     * GET -> geting data from FHIR and return
     * FETCH -> Getting data from FHIR if empty, subscribe
     * SUBSCRIBE -> regardless of availability of data in FHIR just SUBSCRIBE
     */

    public Resource getFhirResource(@NotNull String query, String returnMessage, String processID, String variableName,
            String taskIdentifier, boolean isInterrupting) throws IOException {
        FhirContext ctx = FhirContext.forR4();
        try {
            // Extract CRUD Operator
            String[] crudOperationSplit = query.split(":", 2);
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
                subscribeToFhirObject(fhirResource, query, parser, processID, returnMessage, variableName,
                        taskIdentifier, isInterrupting);
            }
            if (crudOperation.equals("FHIR(FETCH)")) {
                resource = getFhirObject(fhirResource, query, parser);
                if (resource != null) {
                    logger.info("Resource from fetch condition: ");
                    return resource;
                } else
                    subscribeToFhirObject(fhirResource, query, parser, processID, returnMessage, variableName,
                            taskIdentifier, isInterrupting);
            }

        } catch (IncorrectQueryException e) {
            logger.info("Incorrect query, please use the format {CRUD Operation}:{FHIR Resource Type}?{FHIR Query} -> "
                    + e);
        }
        return null;
    }


    private void subscribeToFhirObject(String fhirResource, String query, IParser parser, String processID,
            String returnMessage, String variableName, String taskIdentifier, boolean isInterrupting) {
        logger.info("Subscribing to FHIR Resource");
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
                + variableName + "/" + taskIdentifier + "/" + isInterrupting);
        List<StringType> headers = new ArrayList<StringType>();
        headers.add(new StringType("returnMessage: " + returnMessage));
        headers.add(new StringType("processID: " + processID));
        headers.add(new StringType("variableName: " + variableName));
        headers.add(new StringType("taskIdentifier: " + taskIdentifier));
        headers.add(new StringType("isInterrupting: " + isInterrupting));
        hook.setHeader(headers);

        subscription.setChannel(hook);

        // Send Subscription to FHIR
        HttpResponse<JsonNode> subResponse = Unirest.post(properties.get("config.fhirUrl") + "/fhir/Subscription")
                .header("Content-Type", "application/json+fhir")
                .body(parser.encodeResourceToString(subscription))
                .asJson();

        pendingRequests.get(processID).put(returnMessage,
                new String[] { fhirResource + "?" + query, subResponse.getBody().getObject().getString("id") });
    }

    /*
     * This method is used to get the FHIR resource from the FHIR server
     * 
     * @param fhirResource - The FHIR resource type to fetch
     * 
     * @param query - The query to fetch the FHIR resource
     * 
     * @param parser - The parser to parse the FHIR resource
     * The retry mechanism is used to wait and check for the database in case FHIR
     * is in the process of updating the database
     */

    public Resource getFhirObject(String fhirResource, String query, IParser parser) {
        String baseFhirUrl = properties.get("config.fhirUrl") + "/fhir/";
        String requestUrl = baseFhirUrl + fhirResource + "?" + query;
        int waitTime = 500; // milliseconds
        int retries = 0;
        while (retries < 5) {
            try {
                HttpResponse<JsonNode> httpResponse = Unirest.get(requestUrl).asJson();
                if (httpResponse.isSuccess()) {
                    Bundle bundle = (Bundle) parser.parseResource(httpResponse.getBody().toString());
                    if (bundle.hasEntry()) {
                        return new FhirDataResources(baseFhirUrl).getMostRecentBundleEntry(bundle);
                    }
                    logger.info("No FHIR resource found. Retrying...");
                } else {
                    logger.info("Failed to fetch FHIR resource. Status code: " + httpResponse.getStatus());
                    throw new FhirResourceAccessException(
                            "Failed to fetch FHIR resource. Status code: " + httpResponse.getStatus());
                }
            } catch (UnirestException e) {
                logger.severe("Network error occurred while fetching FHIR resource (retry " + (retries + 1) + "): "
                        + e.getMessage());
            } catch (ParsingException e) {
                logger.severe("Error parsing FHIR response (retry " + (retries + 1) + "): " + e.getMessage());
            } catch (Exception e) {
                logger.severe("Unexpected exception occurred while fetching FHIR resource (retry " + (retries + 1)
                        + "): " + e.getMessage());
            }
            waitTime *= 2;
            retries++;
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                logger.severe("Thread sleep interrupted: " + e.getMessage());
                break;
            }
        }
        logger.info("Failed to fetch FHIR resource after " + retries + " retries.");
        return null;
    }

    public class FhirResourceAccessException extends Exception {
        public FhirResourceAccessException(String message) {
            super(message);
        }
    }
}

class IncorrectQueryException extends Exception {
    IncorrectQueryException(String errorMessage) {
        super(errorMessage);
    }
}