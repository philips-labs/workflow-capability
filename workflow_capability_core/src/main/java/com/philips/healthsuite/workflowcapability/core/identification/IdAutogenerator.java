package com.philips.healthsuite.workflowcapability.core.identification;

import java.util.UUID;


/**
 * This class creates random and unique ID strings.
 */
public final class IdAutogenerator {

    private IdAutogenerator() {
        throw new AssertionError("Do not instantiate");
    }

    public static final String generateId() {
        final String id = UUID.randomUUID().toString();
        return id;
    }
}
