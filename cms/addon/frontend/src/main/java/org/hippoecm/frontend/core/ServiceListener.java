package org.hippoecm.frontend.core;

import java.io.Serializable;
import java.util.EventListener;

public interface ServiceListener extends EventListener {

    int ADDED = 1;
    int CHANGED = 2;
    int REMOVED = 3;

    void processEvent(int type, String name, Serializable service);
}
