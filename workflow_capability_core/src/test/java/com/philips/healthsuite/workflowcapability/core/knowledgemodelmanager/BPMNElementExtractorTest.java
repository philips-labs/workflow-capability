package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager;

import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

class BPMNElementExtractorTest {

    private BPMNElementExtractor elementExtractor;

    @BeforeEach
    public void setUp() throws Exception{
        elementExtractor = new BPMNElementExtractor(new File("src/test/resources/test_case_diagram.bpmn"));
    }

    @Test
    @DisplayName("Ensure ID and Name are extracted from diagram")
    void GetDiagramInfo() {
        PlanDefinition clinicalProcess = elementExtractor.getDiagramInfo();
        for(Identifier identifier : clinicalProcess.getIdentifier()){
            if(identifier.getSystem().equals("workflowId")){
                assertEquals("Process_0q47gav", identifier.getValue());
            }
        }
        assertEquals("Test Case BPMN", clinicalProcess.getName());
    }


    @Test
    @DisplayName("Ensure ID and Name are extracted from a Receive Task")
    void getReceiveTask() {
        List<PlanDefinitionActionComponent> receiveTasks = elementExtractor.getReceiveTasks();
        assertEquals("Activity_0q7q0bb", receiveTasks.get(0).getId());
        assertEquals("Receive Task 1", receiveTasks.get(0).getTitle());
    }
    @Test
    @DisplayName("Ensure multiple Receive Tasks are extracted")
    void getReceiveTasks() {
        List<PlanDefinitionActionComponent> receiveTasks = elementExtractor.getReceiveTasks();
        assertEquals(2, receiveTasks.size());
        assertEquals("Activity_0q7q0bb", receiveTasks.get(0).getId());
        assertEquals("Activity_1chlbhx", receiveTasks.get(1).getId());
    }

    @Test
    @DisplayName("Ensure ID and Name are extracted from a User Task")
    void getUserTask() {
        List<PlanDefinitionActionComponent> userTasks = elementExtractor.getUserTasks();
        assertEquals("Activity_0vwm6s0", userTasks.get(0).getId());
        assertEquals("User Task 1", userTasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Ensure multiple User Tasks are extracted")
    void getUserTasks() {
        List<PlanDefinitionActionComponent> userTasks = elementExtractor.getUserTasks();
        assertEquals(2, userTasks.size());
        assertEquals("Activity_0vwm6s0", userTasks.get(0).getId());
        assertEquals("Activity_0wdkaj7", userTasks.get(1).getId());
    }
}