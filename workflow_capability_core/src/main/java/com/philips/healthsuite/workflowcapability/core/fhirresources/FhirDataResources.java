package com.philips.healthsuite.workflowcapability.core.fhirresources;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.*;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.philips.healthsuite.workflowcapability.core.utilities.DateTimeUtil;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CarePlan.CarePlanActivityComponent;
import org.hl7.fhir.r4.model.CarePlan.CarePlanStatus;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.Task.TaskIntent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * Provides queries to FHIR server.
 */
public class FhirDataResources {
    private final FhirContext ctx = FhirContext.forR4();
    private final String fhirServerUrl;
    private final IGenericClient fhirClient;


    public FhirDataResources(String baseURL) {
        this.fhirServerUrl = baseURL;
        this.fhirClient = this.ctx.newRestfulGenericClient(this.fhirServerUrl);
    }


    /**
     * Returns the first found Patient by Id
     *
     * @param id ID of the Patient that should be queried
     * @return FHIR Patient Resource
     */
    public Patient getPatientById(String id) {
        Bundle patientBundle = fhirClient.search()
                .forResource(Patient.class)
                .where(Patient.RES_ID.exactly().code(id))
                .returnBundle(Bundle.class)
                .execute();
        if (patientBundle.getTotal() != 0) {
            return (Patient) this.getFirstBundleEntry(patientBundle);
        }
        return null;
    }


    /**
     * Returns the first resource from a bundle
     *
     * @param bundle A FHIR Bundle containing BundleEntryComponents
     * @return first FHIR Resource from bundle
     */
    public Resource getFirstBundleEntry(Bundle bundle) {
        for (BundleEntryComponent entry : bundle.getEntry()) {
            Resource res = entry.getResource();
            return res;
        }
        return null;
    }


    /**
     * Returns the resource with the most recent "Last Updated" value from a bundle
     *
     * @param bundle A FHIR Bundle containing BundleEntryComponents
     * @return Most Recent Resource from bundle
     */
    public Resource getMostRecentBundleEntry(Bundle bundle) {
        Date lastUpdated = new Date(0);
        Resource mostRecent = null;
        for (BundleEntryComponent entry : bundle.getEntry()) {
            Resource res = entry.getResource();
            Date updated = res.getMeta().getLastUpdated();
            if (updated.after(lastUpdated)) {
                mostRecent = res;
                lastUpdated = updated;
            }
        }
        return mostRecent;
    }


    /**
     * Returns a CarePlan from a Bundle based on an instanceID
     *
     * @param carePlanBundle Bundle with at least 1 CarePlan Resource
     * @param instanceID     InstanceID of the CarePlan to be returned
     * @return CarePlan or None if no careplan with said instanceID is found
     */
    private CarePlan getCarePlanByIdentifier(Bundle carePlanBundle, String instanceID) {
//        System.out.println(">> This is an instanceID: " + instanceID);
        for (int i = 0; i < carePlanBundle.getEntry().size(); i++) {
            CarePlan carePlan = (CarePlan) carePlanBundle.getEntry().get(i).getResource();
            List<Identifier> identifierList = carePlan.getIdentifier();
            for (Identifier identifier : identifierList) {
                if (identifier.getValue().equals(instanceID)) {
                    return carePlan;
                }
            }
        }
        return null;
    }


    /**
     * Creates a Task and attaches it to the given CarePlan as an Activity.
     *
     * @param taskIdentifier     Workflow Engine identifier of the Task
     * @param carePlanInstanceID ID of the CarePlan the Task should be appended to
     * @param actionID           ID of the action in the PlanDefinition with the Task's information
     * @param status             The current status of the task
     * @return MethodOutcome from the POST action to the FHIR Store
     */
    public MethodOutcome createTask(String taskIdentifier, String carePlanInstanceID, String actionID, String status, String description) throws InterruptedException {
        Objects.requireNonNull(carePlanInstanceID);
        Objects.requireNonNull(actionID);
        Objects.requireNonNull(status);


        CarePlan carePlan = null;
        int numberOfRetries = 10;
        int timeBetweenRetriesMillis = 500;
        synchronized (this) {
            while (numberOfRetries > 0) {
                // Get CarePlan to add the Task to
                Bundle carePlanBundle = fhirClient.search()
                        .forResource(CarePlan.class)
                        .where(CarePlan.IDENTIFIER.hasSystemWithAnyCode("wfEngine"))
                        .returnBundle(Bundle.class)
                        .cacheControl(CacheControlDirective.noCache())
                        .execute();
                Objects.requireNonNull(carePlanBundle);
                carePlan = getCarePlanByIdentifier(carePlanBundle, carePlanInstanceID);
                if (carePlan != null) {
                    break;
                }
                Thread.sleep(timeBetweenRetriesMillis);
                numberOfRetries--;
            }


            Objects.requireNonNull(carePlan, "No CarePlan found for instanceId: " + carePlanInstanceID);

            // Get corresponding PlanDefinition /w actions
            PlanDefinition planDefinition = getPlanDefinitionById(carePlan.getInstantiatesCanonical().get(0).getValue());
            PlanDefinitionActionComponent currentAction = null;
            for (PlanDefinitionActionComponent action : planDefinition.getAction()) {
                if (action.getId().equals(actionID)) {
                    currentAction = action;
                }
            }
            Task task = new Task();
            switch (status) {
                case "ready":
                    task.setStatus(TaskStatus.READY);
                    break;
                case "accepted":
                    task.setStatus(TaskStatus.ACCEPTED);
                    break;
                case "received":
                    task.setStatus(TaskStatus.RECEIVED);
                    break;
                default:
                    task.setStatus(TaskStatus.READY);
            }
            task.setIntent(TaskIntent.UNKNOWN);
			task.setExecutionPeriod(task.getExecutionPeriod().setStart(DateTimeUtil.getCurrentDateWithTimezone()));
            task.addIdentifier().setSystem("camundaIdentifier").setValue(taskIdentifier);
            if (currentAction != null) {
                if (currentAction.getTitle() != null) {
                    task.addIdentifier().setSystem("taskName").setValue(currentAction.getTitle());
                }
                if(description != null && !description.equals("null") && !description.equals("")){
                    task.setDescription(description);
                }
                else if (currentAction.getDescription() != null) {
                    task.setDescription(currentAction.getDescription());
                }
            }
            MethodOutcome outcome = this.addResource(task);

            // Set correct reference in CarePlan to the created Task
            CarePlanActivityComponent activity = new CarePlanActivityComponent();
            activity.setReference(new Reference("Task/" + outcome.getId().getIdPart()));
            List<CarePlanActivityComponent> activityList = carePlan.getActivity();
            activityList.add(activity);
            carePlan.setActivity(activityList);

            return fhirClient.update().resource(carePlan).execute();
        }
    }


    /**
     * Adds a PlanDefinitionActionComponent to an already existing PlanDefinition in the FHIR Store
     *
     * @param planID ID of the PlanDefinition in the FHIR Store
     * @param action PlanDefinitionActionComponent to be added to the PlanDefinition
     * @return The MethodOutcome return from the FHIR Server
     */
    public MethodOutcome addActionToPlanDefinition(String planID, PlanDefinitionActionComponent action) throws NullPointerException {
        // Add an action to a Plan Definition
        try {
            Bundle planDefinitionBundle = fhirClient.search()
                    .forResource(PlanDefinition.class)
                    .where(PlanDefinition.RES_ID.exactly().code(planID))
                    .returnBundle(Bundle.class)
                    .execute();

            PlanDefinition planDefinition = (PlanDefinition) getMostRecentBundleEntry(planDefinitionBundle);
            List<PlanDefinitionActionComponent> actionList = planDefinition.getAction();
            actionList.add(action);
            planDefinition.setAction(actionList);

            MethodOutcome outcome = fhirClient.update().resource(planDefinition).execute();

            return outcome;
        } catch (Exception e) {
            throw new NullPointerException("Can't find the current CarePlan");
        }
    }


    /**
     * Creates a FHIR PlanDefinition in the FHIR Store
     *
     * @param planDefinition The PlanDefinition FHIR Resource which should be communicated to FHIR
     * @return The MethodOutcome return from the FHIR Server
     */
    public MethodOutcome createPlanDefinition(Resource planDefinition) {

        MethodOutcome outcome = fhirClient.create()
                .resource(planDefinition)
                .prettyPrint()
                .encodedJson()
                .execute();
        return outcome;
    }


    /**
     * Adds an arbitrary FHIR Resource to the FHIR Store
     *
     * @param resource FHIR Resource to be added to the FHIR Store
     * @return The MethodOutcome return from the FHIR Server
     */
    public MethodOutcome addResource(Resource resource) {
        MethodOutcome outcome = fhirClient.create()
                .resource(resource)
                .prettyPrint()
                .encodedJson()
                .execute();
        return outcome;
    }


    /**
     * Queries PlanDefinition from the FHIR Store based on their ID
     *
     * @param id ID of the PlanDefinition to be queried
     * @return Queried PlanDefinition
     */
    public PlanDefinition getPlanDefinitionById(String id) {
        PlanDefinition obs = fhirClient.read().resource(PlanDefinition.class).withId(id).execute();
        return obs;
    }


    /**
     * Queries CarePlan from the FHIR Store based on their ID
     *
     * @param id ID of the CarePlan to be queried
     * @return Queried CarePlan
     */
    public CarePlan getCarePlanById(String id) {
        CarePlan obs = fhirClient.read().resource(CarePlan.class).withId(id).execute();
        return obs;
    }


    /**
     * Queries Task from the FHIR Store based on their ID
     *
     * @param id ID of the Task to be queried
     * @return Queried Task
     */
    public Task getTaskById(String id) {
        Task obs = fhirClient.read().resource(Task.class).withId(id).execute();
        return obs;
    }


    /**
     * Removes FHIR Resource from the FHIR Store
     *
     * @param resource Resource which should be removed
     * @return The MethodOutcome return from the FHIR Server
     */
    public MethodOutcome removeResource(Resource resource) {
        MethodOutcome outcome = fhirClient.delete()
                .resource(resource)
                .prettyPrint()
                .encodedJson()
                .execute();

        return outcome;
    }


    /**
     * Gets all Currently Active CarePlans in the FHIR Store
     *
     * @return List of CarePlans
     */
    public List<CarePlan> getNewCarePlans() {
        List<CarePlan> newCarePlans = new ArrayList<>();
        Bundle carePlanBundle = fhirClient.search()
                .forResource(CarePlan.class)
                .where(CarePlan.CATEGORY.exactly().code("WorkflowCapability"))
                .and(CarePlan.STATUS.exactly().code("draft"))
                .returnBundle(Bundle.class)
                .cacheControl(CacheControlDirective.noCache())
                .execute();
        for (BundleEntryComponent bundleElement : carePlanBundle.getEntry()) {
            newCarePlans.add((CarePlan) bundleElement.getResource());
        }
        return newCarePlans;
    }


    /**
     * Starts a CarePlan in the Workflow Engine based on the instanceID given
     *
     * @param carePlan
     * @param instanceID
     * @return
     */
    public MethodOutcome startCarePlan(@NotNull CarePlan carePlan, String instanceID) {
        List<Identifier> carePlanIdentifier = carePlan.getIdentifier();
        Identifier instanceIdentifier = new Identifier();
        carePlanIdentifier.add(instanceIdentifier.setSystem("wfEngine").setValue(instanceID));
        carePlan.setStatus(CarePlanStatus.ACTIVE);
        carePlan.setPeriod(carePlan.getPeriod().setStart(DateTimeUtil.getCurrentDateWithTimezone()));
        MethodOutcome outcome = fhirClient.update().resource(carePlan).execute();
        return outcome;
    }


    /**
     * Returns latest 5000 Tasks with status completed.
     *
     * @return List of completed Tasks
     */
    public List<String> getRecentlyCompletedTasksIds() {
        List<String> newTasks = new ArrayList<>();
        Bundle taskBundle = fhirClient.search()
                .forResource(Task.class)
                .where(Task.STATUS.exactly().code("completed"))
                .sort(new SortSpec("_id", SortOrderEnum.DESC))
                .returnBundle(Bundle.class)
                .cacheControl(CacheControlDirective.noCache())
                .count(5000)
                .execute();
        for (BundleEntryComponent bundleElement : taskBundle.getEntry()) {
            newTasks.add(((Task) bundleElement.getResource()).getIdentifier().get(0).getValue());
        }
        return newTasks;
    }


    /**
     * Removes a Task and references to a Task from the FHIR Store
     *
     * @param task FHIR Task to be removed
     */
    public void deleteTaskFromFhirStore(@NotNull Task task) {
        Bundle carePlanBundle = fhirClient.search()
                .forResource(CarePlan.class)
                .where(CarePlan.ACTIVITY_REFERENCE.hasId(task.getIdElement().getIdPart()))
                .returnBundle(Bundle.class)
                .cacheControl(CacheControlDirective.noCache())
                .execute();
        for (BundleEntryComponent bundleElement : carePlanBundle.getEntry()) {
            CarePlan carePlan = (CarePlan) bundleElement.getResource();
            List<CarePlanActivityComponent> carePlanActivities = carePlan.getActivity();
            List<CarePlanActivityComponent> activitiesToRemove = new ArrayList<>();
            for (CarePlanActivityComponent activity : carePlanActivities) {
                if (activity.getReference().getReference().equals("Task/" + task.getIdElement().getIdPart())) {
                    activitiesToRemove.add(activity);
                }
            }
            carePlanActivities.removeAll(activitiesToRemove);
            MethodOutcome outcome = fhirClient.update().resource(carePlan).execute();
        }
        DeleteCascadeModeEnum deleteCascadeModeEnum = DeleteCascadeModeEnum.DELETE;
        fhirClient.delete().resource(task).cascade(deleteCascadeModeEnum).execute();
    }


    /**
     * Gets a FHIR Resource based on its FHIR ID
     *
     * @param id   FHIR ID of the Resource
     * @param type FHIR Type of the Resource
     * @return Queried FHIR IBaseResource
     */
    public IBaseResource getResourceById(String id, String type) {
        return fhirClient.read().resource(type).withId(id).execute();
    }


    /**
     * Get a CarePlan from the FHIR Store based on its Workflow Engine ID
     *
     * @param engineId Workflow Engine ID of the currently running CarePlan
     * @return Queried CarePlan Resource
     */
    public CarePlan getCarePlanByEngineId(String engineId) {
        Bundle carePlanBundle = fhirClient.search()
                .forResource(CarePlan.class)
                .where(CarePlan.IDENTIFIER.hasSystemWithAnyCode("wfEngine"))
                .returnBundle(Bundle.class)
                .cacheControl(CacheControlDirective.noCache())
                .execute();

        return getCarePlanByIdentifier(carePlanBundle, engineId);
    }


    /**
     * Method for marking a FHIR CarePlan as completed.
     *
     * @param carePlanInstanceID
     */
    public void markCarePlanAsCompleted(String carePlanInstanceID) {
        Strings.isNotBlank(carePlanInstanceID);
        Bundle carePlanBundle = fhirClient.search()
                .forResource(CarePlan.class)
                .where(CarePlan.IDENTIFIER.hasSystemWithAnyCode("wfEngine"))
                .returnBundle(Bundle.class)
                .cacheControl(CacheControlDirective.noCache())
                .execute();

        Objects.requireNonNull(carePlanBundle);
        CarePlan carePlan = getCarePlanByIdentifier(carePlanBundle, carePlanInstanceID);
        Objects.requireNonNull(carePlan, "CarePlan not found for ID: " + carePlanInstanceID);

        carePlan.setStatus(CarePlanStatus.COMPLETED);
        carePlan.setPeriod(carePlan.getPeriod().setEnd(DateTimeUtil.getCurrentDateWithTimezone()));

        fhirClient.update().resource(carePlan).execute();
    }

}
