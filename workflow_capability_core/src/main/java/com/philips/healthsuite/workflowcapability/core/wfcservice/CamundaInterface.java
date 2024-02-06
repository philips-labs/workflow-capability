package com.philips.healthsuite.workflowcapability.core.wfcservice;


import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

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
            System.out.println(" the error log is for  no id ==>");
            return null;
        }
        System.out.println(" the response log is ==>  " +  httpResponse.getBody().getObject().get("id"));
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
        HttpResponse<JsonNode> httpResponse = Unirest.post(camundaUrl +
                        "/engine-rest/task/" + taskID + "/complete")
                .header("Content-Type", "application/json")
                .asJson();
        System.out.println(httpResponse.getBody().toString());// this is just for debugging purposes 

        // I suppose httpResponse contains the response from Camunda after telling it to complete a task. Maybe we can use it
        return "ok";
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
    // I suppose httpResponse contains the response from Camunda after telling it to send a message. Maybe we can use it
    

}
