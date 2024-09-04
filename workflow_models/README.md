# Medical Protocol models
This folder contains BPMN and DMN models describing medical protocols of Blood Loss and Sepsis.

## Description  
There are three models available to execute through this application. 
1) Blood Loss: This is a model for the Blood Loss protocol, created by Juan (the trainee who created the first prototype of Workflow Capability). 
This model can be demonstrated with demo patients 1,2,3,4.  
2) Sepsis v1: This is the first model we created for Sepsis protocol. It is the first full version and it utilizes the FHIR observations previously created for 
the Blood Loss protocol. This model can be demonstrated with demo patients 1,2,3,4.  
3) Sepsis v2: This is the second model we created for Sepsis protocol. It is the second full version and it utilizes custom FHIR observations specifically created for this
treatment plan. This model can be demonstrated with demo patients 5,6,7,8,9,10.  
4) Sepsis v3: This is modified version model that supports both the previous version and newly added alternative version(Message Boundary Message Event) with Time Query and without Time Query
## Structure  
This is the folder/files structure of the models:  

|   README.md
|   
+---blood-loss
|       blood-loss-value.dmn
|       blood-loss.bpmn
|       coagulation-table.dmn
|       
\---sepsis
    +---v1_old_tables_and_observations
    |       Sepsis_Protocol.bpmn
    |       Sepsis_Protocol_usertasks.bpmn
    |       size_infection.dmn
    |       sober.dmn
    |       symptoms.dmn
    |       
    \---v2
            InfectionSize_Decision.dmn
            Sepsis_Protocol.bpmn
            Soberity_Decision.dmn
            Symptoms_Decision.dmn
    \---v3
            BPMN
                CLOSED-LOOP
                    closed-loop-interrupting.bpmn
                    closed-loop-non-interrupting.bpmn
                FETCH
                    Sepsis_Protocol_Message_boundary_event.bpmn
                    Sepsis_Protocol_Message_Boundary_Event_Time_query_MOMENT.bpmn
                    Sepsis_Protocol_Message_Boundary_Event_Time_query_NOW.bpmn
                    Sepsis_Protocol_Receive_Task.bpmn
                    Sepsis_Protocol_Receive_Task_Time_query_MOMENT.bpmn
                    Sepsis_Protocol_Receive_Task_Time_query_NOW.bpmn
                GET
                    Sepsis_Protocol_Message_boundary_event.bpmn
                    Sepsis_Protocol_Message_Boundary_Event_Time_query_MOMENT.bpmn
                    Sepsis_Protocol_Message_Boundary_Event_Time_query_NOW.bpmn
                    Sepsis_Protocol_Receive_Task.bpmn
                    Sepsis_Protocol_Receive_Task_Time_query_MOMENT.bpmn
                    Sepsis_Protocol_Receive_Task_Time_query_NOW.bpmn
                SUBSCRIBE
                    Sepsis_Protocol_Message_boundary_event.bpmn
                    Sepsis_Protocol_Message_Boundary_Event_Time_query_MOMENT.bpmn
                    Sepsis_Protocol_Message_Boundary_Event_Time_query_NOW.bpmn
                    Sepsis_Protocol_Receive_Task.bpmn
                    Sepsis_Protocol_Receive_Task_Time_query_MOMENT.bpmn
                    Sepsis_Protocol_Receive_Task_Time_query_NOW.bpmn
            DMN
                respiratory-check.dmn
                InfectionSize_Decision.dmn
                Soberity_Decision.dmn
                Symptoms_Decision.dmn