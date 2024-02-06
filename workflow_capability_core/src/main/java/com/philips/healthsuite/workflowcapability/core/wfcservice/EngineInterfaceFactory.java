package com.philips.healthsuite.workflowcapability.core.wfcservice;

import java.io.IOException;

public class EngineInterfaceFactory {

    public EngineInterface getEngineInterface(String engineType) throws IOException {
        if (engineType == "CAMUNDA") {
            return new CamundaInterface();
        }
        
        return null;
    }

}
