package com.philips.healthsuite.workflowcapability.core.identification;

import java.util.UUID;


/**
 * This class creates random and unique strings to be used as Patient IDs.
 */
public class PatientIdAutogenerator {

    private PatientIdAutogenerator() {
        throw new AssertionError("Do not instantiate");
    }

    public static final String generatePatientId() {
        final String id = "Patient_" + UUID.randomUUID().toString();
        return id;
    }
}