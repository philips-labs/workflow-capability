package com.philips.healthsuite.workflowcapability.core.wfcservice;

import java.io.File;

public interface EngineInterface {

    /**
     * Instantiates a defined workflow, on a patient based on patientID, in the Workflow Engine based on the WorkflowID
     *
     * @param workflowID The workflowID of the Workflow Definition in the Workflow Engine
     * @param patientID  The FHIR patientID the workflow has to be instantiated for
     * @return The workflow instance ID as a String if succesfull, otherwise returns null
     */
    String instantiateWorkflow(String workflowID, String patientID);

    /**
     * Deploys a Workflow Model (BPMN, DMN) to the Workflow Engine.
     *
     * @param WfModelfile    A BPMN or DMN File containing the Workflow Definition
     * @param deploymentName The name of the to be deployed Workflow
     * @return Confirmation that the model is succesfully deployed as a String ( "ok" )
     */
    String deployModel(File WfModelfile, String deploymentName);

    /**
     * Completes a task in an instantiated Workflow
     *
     * @param taskID The instanceID of the Task to be completed
     * @return Confirmation that the task is succesfully completed as a String ( "ok" )
     */
    String completeTask(String taskID);

    /**
     * Sends a BPMN "Message" to the workflow engine for a certain instantiated Workflow.
     * This message contains a payload which is a variable that is sent to the engine
     *
     * @param messageID    The message identifier of the message to be sent
     * @param processID    The process identifier of the instantiated workflow in the engine
     * @param variableName The name of the payload variable
     * @param variableJson The JSON value of the payload variable
     */
    void sendMessage(String messageID, String processID, String variableName, String variableJson);
}
