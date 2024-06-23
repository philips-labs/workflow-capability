package com.philips.healthsuite.workflowcapability.core.wfcservice;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import java.util.logging.Logger;

import com.philips.healthsuite.workflowcapability.core.WfcServiceApplication;

public class CamundaInterface implements EngineInterface {
    Logger logger = Logger.getLogger(WfcServiceApplication.class.getName());
    Properties properties;
    String camundaUrl;
    
    public CamundaInterface() throws IOException {
        properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        camundaUrl = properties.getProperty("config.camundaUrl");

    }

    @Override
    public String instantiateWorkflow(String workflowID, String patientID) {
        // Construct JSON payload for the request
        String jsonPayload = "{\n" +
                "  \"variables\": {\n" +
                "    \"patient\" : {\n" +
                "        \"value\" : \"" + patientID + "\",\n" +
                "        \"type\": \"String\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // Send POST request to start the workflow
        HttpResponse<JsonNode> httpResponse = Unirest
                .post(camundaUrl + "/engine-rest/process-definition/key/" + workflowID + "/start")
                .header("Content-Type", "application/json")
                .body(jsonPayload)
                .asJson();
        if (httpResponse.getBody().getObject().isNull("id")) {
            logger.warning("Instance is not created in the engine.");
            return null;
        }
        String instanceId = httpResponse.getBody().getObject().getString("id");
        logger.info("Instance ID: created in the engine");
        return instanceId;
    }

    @Override
    public String deployModel(File bpmnFile, String deploymentName) {
        HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl + "/engine-rest/deployment/create")
                // HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl +
                // "/engine-rest/deployment/create")
                .field("deployment-name", deploymentName)
                .field("enable-duplicate-filtering", "false")
                .field("deploy-changed-only", "false")
                .field("*", bpmnFile)
                .asJson();
        if (httpResponse.getStatus() != 200) {
            logger.severe(" the error log is ==>  " + httpResponse.getStatus() + " file is " + deploymentName + " ==>");
            return null;
        }
        return "ok";
    }

    
    @Override
    public String completeTask(String taskID) {
        // Check if the task exists
        HttpResponse<JsonNode> checkResponse = Unirest.get(camundaUrl + "/engine-rest/task/" + taskID)
                .header("Content-Type", "application/json")
                .asJson();

        if (checkResponse.getStatus() == 200) {
            // Task exists, proceed to complete it
            HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl + "/engine-rest/task/" + taskID + "/complete")
                    .header("Content-Type", "application/json")
                    .asJson();
            logger.info("Response after completing the task in Camunda: ");

            return "Task completed successfully";
        } else if (checkResponse.getStatus() == 404) {
            return " ";
        } else {
            // Some other error occurred
            logger.severe(
                    "Error checking task status: " + checkResponse.getStatus() + " " + checkResponse.getStatusText());
            return "Error checking task status";
        }
    }

    @Override
    public void sendMessage(String messageID, String processID, String variableName, String variableJson) {
        int maxRetries = 3; // adjust the maximum retries as needed
        int retries = 0;
        boolean messageSent = false;

        while (retries <= maxRetries && !messageSent) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("messageName", messageID);
                jsonObject.put("processInstanceId", processID);
                if (variableJson != null) {
                    JSONObject processVariables = new JSONObject();
                    JSONObject jsonVars = new JSONObject();
                    jsonVars.put("value", variableJson);
                    jsonVars.put("type", "Json");
                    processVariables.put(variableName, jsonVars);
                    jsonObject.put("processVariables", processVariables);
                }

                HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl + "/engine-rest/message/")
                        .header("Content-Type", "application/json")
                        .body(jsonObject)
                        .asJson();

                if (httpResponse.getStatus() == 200 || httpResponse.getStatus() == 204) {
                    logger.info("Message sent successfully: " + httpResponse.getStatus() + " "
                            + httpResponse.getStatusText());
                    messageSent = true;
                } else {
                    logger.info("Failed to send message: " + httpResponse.getStatusText());
                    retries++;
                    // wait for 1 second before retrying
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                logger.severe("Error sending message: " + e.getMessage());
                retries++;
                // wait for 1 second before retrying
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    logger.severe("Error sleeping thread: " + e1.getMessage());
                    e1.printStackTrace();
                }
            }
        }

        if (!messageSent) {
            logger.severe("Failed to send message after " + maxRetries + " retries");
        }
    }
}
