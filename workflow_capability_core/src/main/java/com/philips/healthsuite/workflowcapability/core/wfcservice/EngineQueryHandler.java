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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EngineQueryHandler {
    HashMap<String, HashMap<String, String[]>> pendingRequests;
    Properties properties;
    Logger logger =  Logger.getLogger(EngineQueryHandler.class.getName());
    public EngineQueryHandler() throws IOException {
        properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        pendingRequests = new HashMap<>();
    }

    public String removeDateParameter(String originalQuery) {

        Pattern datePattern = Pattern.compile("&date=[^&]*");
        Matcher matcher = datePattern.matcher(originalQuery);

        if (matcher.find()) {
            logger.info("Date pattern found and removed");
            originalQuery = matcher.replaceAll("");
        }

        return originalQuery;
    }
    public String fetchByTime(String originalQuery) {
        // Updated pattern to match the new format date=(NOW -4s)
        Pattern timePattern = Pattern.compile("date=\\(NOW -?(\\d+)([SMHD])\\)");
        Matcher matcher = timePattern.matcher(originalQuery);
        if (!matcher.find()) {
            logger.info("No time pattern found");
            return originalQuery;
        }

        int value = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);
        Instant currentDate = Instant.now();
        switch (unit) {
            case "S":
                currentDate = currentDate.minus(value, ChronoUnit.SECONDS);
                break;
            case "M":
                currentDate = currentDate.minus(value, ChronoUnit.MINUTES);
                break;
            case "H":
                currentDate = currentDate.minus(value, ChronoUnit.HOURS);
                break;
            case "D":
                currentDate = currentDate.minus(value, ChronoUnit.DAYS);
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit");
        }

        String isoDate = DateTimeFormatter.ISO_INSTANT.format(currentDate);
        logger.info("Current Date is: " + isoDate + " " + unit + " " + value);

        // Update the original query with the computed date
        String updatedQuery = originalQuery.replaceAll("date=\\(NOW -?\\d+[SMHD]\\)", "date=ge" + isoDate);

        return updatedQuery;
    }

    public Resource getFhirResource(@NotNull String query, String returnMessage, String processID, String variableName, String taskIdentifier) throws IOException {
        FhirContext ctx = FhirContext.forR4();
        logger.info("Get params: " + query + ": " + returnMessage + " : " + processID + " : " + variableName + " : " + taskIdentifier);
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
                    logger.info("Resource from fetch condition: ");
                    return resource;
                }
                else
                subscribeToFhirObject(fhirResource, query, parser, processID, returnMessage, variableName, taskIdentifier);
            }

        } catch (IncorrectQueryException e) {
            logger.info("Incorrect query, please use the format {CRUD Operation}:{FHIR Resource Type}?{FHIR Query} -> " + e);
        }
        return null;
    }

    private void subscribeToFhirObject(String fhirResource, String query, IParser parser, String processID,
        String returnMessage, String variableName, String taskIdentifier) {
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
                + variableName + "/" + taskIdentifier);
        List<StringType> headers = new ArrayList<StringType>();
        headers.add(new StringType("returnMessage: " + returnMessage));
        headers.add(new StringType("processID: " + processID));
        headers.add(new StringType("variableName: " + variableName));
        headers.add(new StringType("taskIdentifier: " + taskIdentifier));
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
 * @param fhirResource - The FHIR resource type to fetch
 * @param query - The query to fetch the FHIR resource
 * @param parser - The parser to parse the FHIR resource
 *  The retry mechanism is used to wait and check for the database in case FHIR is in the process of updating the database
 */
    public Resource getFhirObject(String fhirResource, String query, IParser parser) {
        String baseFhirUrl = properties.get("config.fhirUrl") + "/fhir/";
        String requestUrl = baseFhirUrl + fhirResource + "?" + query;
        logger.info("Getting FHIR Resource from: " + requestUrl);
        int maxRetries = 3;
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
                logger.severe("Exception occurred while fetching FHIR resource (retry " + retryCount + "): " + e.getMessage());
            }
            
            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            retryDelayMillis *= 2; // Exponential backoff
        }
        
        logger.severe("Failed to fetch FHIR resource after " + maxRetries + " retries.");
        return null;
    }
}

class IncorrectQueryException extends Exception {
    IncorrectQueryException(String errorMessage) {
        super(errorMessage);
    }
}