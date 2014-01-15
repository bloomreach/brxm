package org.hippoecm.hst.integration;

import org.hippoecm.hst.configuration.model.HstManagerImpl;

public class IntegrationHstManagerImpl extends HstManagerImpl {

    private int markStaleCounter = 0;

    @Override
    public synchronized void markStale() {
        markStaleCounter++;
        super.markStale();
    }

    public synchronized int getMarkStaleCounter() {
        return markStaleCounter;
    }

    public BuilderState getState() {
        return state;
    }
}
