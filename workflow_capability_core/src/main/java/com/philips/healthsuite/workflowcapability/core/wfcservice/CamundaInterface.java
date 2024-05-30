package com.philips.healthsuite.workflowcapability.core.wfcservice;


import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.jena.base.Sys;


public class CamundaInterface implements EngineInterface {

    Properties properties;
    String camundaUrl;

    public CamundaInterface() throws IOException {
        properties = new Properties();
        InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("application.properties");
        properties.load(inputStream);
        camundaUrl = properties.getProperty("config.camundaUrl");
    }

    @Override
    public String instantiateWorkflow(String workflowID, String patientID) {
        JsonNode jsonNode = new JsonNode("{\n" +
                "  \"variables\": {\n" +
                "    \"patient\" : {\n" +
                "        \"value\" : \"" + patientID + "\",\n" +
                "        \"type\": \"String\"\n" +
                "    }\n" +
                "  }\n" +
                "}");
        HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl +
                        "/engine-rest/process-definition/key/" + workflowID + "/start")
                .header("Content-Type", "application/json")
                .body(jsonNode)
                .asJson();
        
                System.out.println(httpResponse.getBody().getObject());
        if (httpResponse.getBody().getObject().isNull("id")) {
            System.out.println(" Instance is not created in engine ==>");
            return null;
        }
        return (String) httpResponse.getBody().getObject().get("id");
    }

    @Override
    public String deployModel(File bpmnFile, String deploymentName) {
        HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl + "/engine-rest/deployment/create")
        // HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl + "/engine-rest/deployment/create")
                .field("deployment-name", deploymentName)
                .field("enable-duplicate-filtering", "false")
                .field("deploy-changed-only", "false")
                .field("*", bpmnFile)
                .asJson();
        if (httpResponse.getStatus() != 200) {
            System.out.println(" the error log is ==>  " +  httpResponse.getStatus()+ " file is " + deploymentName+" ==>");
            return null;
        }
        return "ok";
    }

    //completeTask is not used in the current version of the code base......

    @Override
    public String completeTask(String taskID) {
        if (isTaskActive(taskID)) {
            HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl +
                            "/engine-rest/task/" + taskID + "/complete")
                    .header("Content-Type", "application/json")
                    .asJson();
            System.out.println("Text after completing the task in Camunda: " + httpResponse.getBody().toString());
            return "ok";
        } else {
            System.out.println("Task with ID " + taskID + " is not active or does not exist.");
            return "Task not found or not active";
        }
    }

    private boolean isTaskActive(String taskID) {
        HttpResponse<JsonNode> response = Unirest.get(camundaUrl + "/engine-rest/task")
                .queryString("taskId", taskID)
                .asJson();
        System.out.println("Response from Camunda: " + response.getBody().toString());
        if (response.getStatus() == 200) {
            // Assuming that the response will contain an array of tasks
            return response.getBody().getArray().length() > 0;
        } else {
            System.out.println("Error checking task status: " + response.getStatusText());
            return false;
        }
    }

    @Override
public void sendMessage(String messageID, String processID, String variableName, String variableJson) {
    int maxRetries = 5; // maximum number of retries
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
                System.out.println("Message sent successfully: " + httpResponse.getStatus() + " " + httpResponse.getStatusText());
                messageSent = true;
            } else {
                System.out.println("Failed to send message: " + httpResponse.getStatusText());
                retries++;
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            retries++;
            // wait for 1 second before retrying
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                System.err.println("Error sleeping thread: " + e1.getMessage());
                e1.printStackTrace();
            }
        }
    }

    if (!messageSent) {
        System.out.println("Failed to send message after " + maxRetries + " retries");
    }
}
}
