package org.camunda.bpm.delegate;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.DataObjectReferenceImpl;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ReceiveTask;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiveTaskEntry implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String message = "";
        ReceiveTask receiveTask = null;
        try {
            receiveTask = delegateExecution.getProcessEngine().getRepositoryService().getBpmnModelInstance(delegateExecution.getProcessDefinitionId()).getModelElementById(delegateExecution.getCurrentActivityId());
            message = receiveTask.getMessage().getName();
        } catch (Exception e) {
//            System.out.println("No message found for current Receive Task");
            e.printStackTrace();
        }
        String variableName = "";
        String query = "";
        try {
            BpmnModelInstance bpmModel = delegateExecution.getProcessEngine().getRepositoryService().getBpmnModelInstance(delegateExecution.getProcessDefinitionId());
            receiveTask = bpmModel.getModelElementById(delegateExecution.getCurrentActivityId());
            // Get documentation from dataReference, NOTE: Assumed only 1 dataRef with a single documentation is found.
            for (DataInputAssociation dataInputAssociation : receiveTask.getDataInputAssociations()) {
                for (ItemAwareElement source : dataInputAssociation.getSources()) {
                    DataObjectReferenceImpl dataReference = bpmModel.getModelElementById(source.getId());
                    variableName = dataReference.getName();
                    for (Documentation documentation : dataReference.getDocumentations()) {
                        query = documentation.getRawTextContent();
                    }
                }
            }
        } catch (Exception e) {
//            System.out.println("No message found for current Receive Task");
            e.printStackTrace();
        }

        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\((.*?)\\)").matcher(query);
        int count = 1;
        while (m.find()) {
            String reqVariable = m.group(count);
            m.appendReplacement(sb, (String) delegateExecution.getVariable(reqVariable));
            count++;
        }
        m.appendTail(sb);

        // Asks WFCC to create a Receive task in FHIR and add it to care plan.
//        HttpResponse<JsonNode> httpResponse1 = Unirest.post(properties.getProperty("wfc.url") +
//                        "/RequestReceiveTask/" + delegateExecution.getProcessInstanceId() + "/" + receiveTask.getId() + "/" + delegateExecution.getActivityInstanceId())
//                .body(sb.toString())
//                .asJson();


        // Asks WFCC if observation is available
        HttpResponse<JsonNode> httpResponse2 = Unirest.post(properties.getProperty("wfc.url") +
                        "/RequestObservationValue/" + delegateExecution.getProcessInstanceId() + "/" + message + "/" + variableName)
                .body(sb.toString())
                .asJson();

    }
}
