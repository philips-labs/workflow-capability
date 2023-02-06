This page will describe how to use the HealthSuite Workflow Capability Prototype. We will firstly give an overview on all accessible elements in the prototype, then describe how to execute certain functionality. 

# Overview
This section describes an overview of the functionality of the HealthSuite Workflow Capbaility Prootype.

## Application
When accessing the application, by default ([https://localhost:5000](https://localhost:5000)). You are presented with the following landing page:
![Landing Page](https://i.imgur.com/HrG4sAx.png)

This landing page shows a menu at the top, leading to pages with the following functionality:
* **Home:** The landing page of the application
* **Patients:** Page to manage the current patients in the hospital and manage their workflows
* **Deploy Models:** From this page BPMN and DMN models can be deployed to the system
* **DevTools:** Used to clear all 'PlanDefinitions', 'CarePlans', and 'Tasks' **USE WITH CARE, SINCE THIS WILL EMPTY YOUR FHIR STORE**
* **Labview:** Used to enter Lab Results to the FHIR Store. Filled in values will end up being an Observation Resource in the FHIR Store

## Camunda Engine
The Camunda Engine provides a dashboard to see the status of all workflows. When accessing the Camunda Engine by default ([https://localhost:8080](https://localhost:8080)). You will be presented with the login screen, by default the login values are:

Username: admin <br>
Password: geheim

![Camunda Dash](https://imgur.com/dYI6WfE.png)

Above you can see the camunda dashboard, where you can see all deployed definitions (BPMN and DMN) and all instantiations currently running. 

# Instructions
In this section we will discuss how to have an end-to-end 

## Deploy Models

To deploy a model to the system, open the application interface (default [https://localhost:5000](https://localhost:5000)), click on the tab Deploy Models.

![Deploy](https://i.imgur.com/GUNhGd9.png)

Click Choose File, and choose the file (Raw XML) you want to upload, examples are given in the resources folder in wfc/src/main/resources. BPMN and DMN extensions are supported. Click "Submit" and the application window will return "Model sucessfully deployed" if all went well.

## Instantiate a model
Take care that it's assumed a FHIR Store is running and filled, and the code points toward the right port. If you have an empty FHIR Store, run the wfc application with preprocessing as described in the "Run Instructions". <br>
To instantiate a model on a Patient, open the application interface (default [https://localhost:5000](https://localhost:5000)), and click on the tab Patients.

![Patient View](https://imgur.com/sEmhbh0.png)

In this view you will see all Patients defined in the FHIR store, to instantiate a workflow for a certain patient, click on Show Workflows. <br>

![Patient Workflows](https://i.imgur.com/xCMw0u4.png)

This view shows all workflows currently running for the current Patient. Below the list, you can start a new workflow. Select the defined workflow you want to start and click Submit.

![workflow_success](https://imgur.com/DFpFEem.png)

If all went well, the application will show a success message like shown above. You have sucessfully instantiated a workflow for the patient.

## Interact with a model
In the Workflow Engine you can follow a workflow instance (default [https://localhost:8080](https://localhost:8080)), to interact with the workflow. Go to the Patient page and select the patient and workflow you want to interact with.

![Patient Overview](https://imgur.com/QvbqKHA.png)

In this view you can interact with the Tasks in the workflow by clicking done, in this way you can traverse through the model.

![Patient Overview](https://imgur.com/u8d5Aap.png)