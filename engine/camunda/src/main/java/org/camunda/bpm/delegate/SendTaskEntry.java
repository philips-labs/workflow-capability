package org.camunda.bpm.delegate;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.DataObjectReferenceImpl;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.Documentation;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.SendTask;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

public class SendTaskEntry implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        SendTask sendTask = null;
        try {
            sendTask = delegateExecution.getProcessEngine().getRepositoryService().getBpmnModelInstance(delegateExecution.getProcessDefinitionId()).getModelElementById(delegateExecution.getCurrentActivityId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String data = "";
        
        try {
            BpmnModelInstance bpmModel = delegateExecution.getProcessEngine().getRepositoryService().getBpmnModelInstance(delegateExecution.getProcessDefinitionId());
            for (DataInputAssociation dataInputAssociation : sendTask.getDataInputAssociations()) {
                for (ItemAwareElement source : dataInputAssociation.getSources()) {
                    DataObjectReferenceImpl dataReference = bpmModel.getModelElementById(source.getId());
                    for (Documentation documentation : dataReference.getDocumentations()) {
                        data = documentation.getRawTextContent();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\((.*?)\\)").matcher(data);
        int count = 1;
        while (m.find()) {
            String reqVariable = m.group(count);
            m.appendReplacement(sb, (String) delegateExecution.getVariable(reqVariable));
            count++;
        }
        m.appendTail(sb);

        HttpResponse<JsonNode> httpResponse = Unirest.post(properties.getProperty("wfc.url") +
                        "/PostObservationValue/")
                .body(sb.toString())
                .asJson();
    }
}
