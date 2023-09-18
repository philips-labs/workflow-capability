package com.philips.healthsuite.workflowcapability.core.wfcservice;


import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.apache.jena.base.Sys;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
        if (httpResponse.getBody().getObject().isNull("id")) {
            return null;
        }
        return (String) httpResponse.getBody().getObject().get("id");
    }

    @Override
    public String deployModel(File bpmnFile, String deploymentName) {
        HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl + "/engine-rest/deployment/create")
                .field("deployment-name", deploymentName)
                .field("enable-duplicate-filtering", "false")
                .field("deploy-changed-only", "false")
                .field("*", bpmnFile)
                .asJson();
        if (httpResponse.getStatus() != 200) {
            return null;
        }
        return "ok";
    }

    @Override
    public String completeTask(String taskId) {
        try {
            HttpResponse<JsonNode> httpResponse = Unirest.get(camundaUrl +
                            "/engine-rest/task/" + taskId)
                    .asJson();

            if (httpResponse.getStatus() == 200) {
                httpResponse = Unirest.post(camundaUrl +
                                "/engine-rest/task/" + taskId + "/complete")
                        .header("Content-Type", "application/json")
                        .asJson();

                if (httpResponse.getStatus() == 204) {
                    System.out.println("Task with id " + taskId + " has been completed.");
                    return "Task completed";
                } else {
                    return "Error completing task: " + httpResponse.getStatusText();
                }
            } else {
                return "Task not found";
            }
        } catch (Exception e) {
            return "Exception caught: " + e.getMessage();
        }
    }


    @Override
    public void sendMessage(String messageID, String processID, String variableName, String variableJson) {
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
        System.out.println(variableJson);
        HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl +
                        "/engine-rest/message/")
                .header("Content-Type", "application/json")
                .body(jsonObject)
                .asJson();

    }

    public void sendVariable(String processInstanceId,
                             String variableName, Integer variableValue) {
        JSONObject newVariable = new JSONObject();
        newVariable.put("type", "Integer");
        newVariable.put("value", variableValue);

        JSONObject modifications = new JSONObject();
        modifications.put(variableName, newVariable);

        JSONObject variables = new JSONObject();
        variables.put("modifications", modifications);

        try {
            System.out.println(processInstanceId);
            System.out.println(variables);
            HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl +
                            "/engine-rest/process-instance/" + processInstanceId + "/variables")
                    .header("Content-Type", "application/json")
                    .body(variables)
                    .asJson();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
