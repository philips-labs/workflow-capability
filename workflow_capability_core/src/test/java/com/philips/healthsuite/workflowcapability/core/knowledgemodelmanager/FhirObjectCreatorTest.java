package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FhirObjectCreatorTest {

    FhirObjectCreator objectCreator;
    FhirDataResources fhirDataResources;
    ArrayList<Resource> resourceList = new ArrayList<>();


    @BeforeEach
    void setUp() {
        objectCreator = new FhirObjectCreator("http://127.0.0.1:8180/fhir");
        fhirDataResources = objectCreator.getFhirDataResources();
    }

    @Disabled
    @Test
    @DisplayName("Ensure PlanDefinition is created in the FHIR Store")
    void createPlanDefinition() {
        PlanDefinition planDefinition = new PlanDefinition();
        planDefinition.addIdentifier().setSystem("workflowId").setValue("testID");
        planDefinition.setName("testName");
        String planDefinitionId = objectCreator.createPlanDefinition(planDefinition);
        assertNotNull(planDefinitionId);
        assertEquals("testID", fhirDataResources.getPlanDefinitionById(planDefinitionId).getIdentifier().get(0).getValue());
        PlanDefinition planDefinitionRes = fhirDataResources.getPlanDefinitionById(planDefinitionId);
        this.resourceList.add(planDefinitionRes); // Add for teardown
    }

    @Disabled
    @Test
    @DisplayName("Ensure Actions can be added to PlanDefinitions in the FHIR Store")
    void addActionToPlan() {
        PlanDefinition planDefinition = new PlanDefinition();
        planDefinition.setId("testPlanID1");
        planDefinition.setName("testName");
        String planDefinitionId = objectCreator.createPlanDefinition(planDefinition);
        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        action.setId("testActionID");
        action.setTitle("testActionName");
        MethodOutcome outcome = objectCreator.addActionToPlan(planDefinitionId, action);
        PlanDefinition planDefinitionRes = fhirDataResources.getPlanDefinitionById(outcome.getId().getIdPart());
        this.resourceList.add(planDefinitionRes); // Add for teardown
        assertNotEquals(0, planDefinitionRes.getAction().size());
    }

    @Disabled
    @Test
    @DisplayName("Ensure List of Actions can be added to PlanDefinitions in the FHIR Store")
    void addAllActionToPlan() {
        PlanDefinition planDefinition = new PlanDefinition();
        planDefinition.setId("testPlanID2");
        planDefinition.setName("testName");
        String planDefinitionId = objectCreator.createPlanDefinition(planDefinition);
        ArrayList<PlanDefinition.PlanDefinitionActionComponent> taskList = new ArrayList<>();
        PlanDefinition.PlanDefinitionActionComponent action1 = new PlanDefinition.PlanDefinitionActionComponent();
        action1.setId("testActionID1");
        action1.setTitle("testActionName1");
        PlanDefinition.PlanDefinitionActionComponent action2 = new PlanDefinition.PlanDefinitionActionComponent();
        action2.setId("testActionID2");
        action2.setTitle("testActionName2");
        taskList.add(action1);
        taskList.add(action2);
        ArrayList<MethodOutcome> outcomes = objectCreator.addAllActionToPlan(planDefinitionId, taskList);
        PlanDefinition planDefinitionRes = fhirDataResources.getPlanDefinitionById(outcomes.get(0).getId().getIdPart());
        this.resourceList.add(planDefinitionRes); // Add for teardown
        assertEquals(2, planDefinitionRes.getAction().size());
    }

    @AfterEach
    void tearDown() {
        removeFHIRResources(this.resourceList);
        this.resourceList.clear();
    }

    void removeFHIRResources(List<Resource> resourceList){
        for (Resource resource : resourceList) {
            if (resource != null) {
                fhirDataResources.removeResource(resource);
            }
        }
    }
}