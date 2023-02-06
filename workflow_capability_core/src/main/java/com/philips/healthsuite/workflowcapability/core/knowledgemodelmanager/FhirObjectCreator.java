package com.philips.healthsuite.workflowcapability.core.knowledgemodelmanager;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.philips.healthsuite.workflowcapability.core.fhirresources.FhirDataResources;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;

import java.util.ArrayList;
import java.util.List;


public class FhirObjectCreator {
    private final FhirDataResources fhirDataResources;


    public FhirObjectCreator(String baseURL) {
        this.fhirDataResources = new FhirDataResources(baseURL);
    }


    /**
     * Creates PlanDefinition Object, uses the fhirInterface to store it in the FHIR Store
     *
     * @param planDefinition PlanDefinition Object that should be pushed to the FHIR Store
     * @return The Plan Definition ID
     */
    public String createPlanDefinition(PlanDefinition planDefinition) {

        // Set type to workflow-definition
        CodeableConcept type = new CodeableConcept();
        type.addCoding().setCode("clinical-process");
        planDefinition.setType(type);
        org.hl7.fhir.r4.model.Enumerations.PublicationStatus status = PublicationStatus.ACTIVE;
        planDefinition.setStatus(status);

        MethodOutcome outcome = fhirDataResources.createPlanDefinition(planDefinition);

        return outcome.getId().getIdPart();
    }





    /**
     * Adds the information of the ProcessTask as an action to a PlanDefinition.
     *
     * @param planID PlanDefinition identifier
     * @param action Action to be added to the PlanDefinition
     * @return The MethodOutcome return from the FHIR Server
     * @throws NullPointerException Thrown when there exists no processTask ID
     */
    public MethodOutcome addActionToPlan(String planID, PlanDefinitionActionComponent action) throws NullPointerException {
        return fhirDataResources.addActionToPlanDefinition(planID, action);
    }


    /**
     * Adds a list of ProcessTasks as actions to a PlanDefinition.
     *
     * @param planID     PlanDefinition identifier
     * @param actionList List of actions to be added to the PlanDefinition
     * @return List of MethodOutcome returns from the FHIR Server
     */
    public ArrayList<MethodOutcome> addAllActionToPlan(String planID, List<PlanDefinitionActionComponent> actionList) {
        ArrayList<MethodOutcome> outcomes = new ArrayList<>();
        for (PlanDefinitionActionComponent action : actionList) {
            outcomes.add(this.addActionToPlan(planID, action));
        }
        return outcomes;
    }

    public FhirDataResources getFhirDataResources() {
        return fhirDataResources;
    }
}
