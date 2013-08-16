package org.hippoecm.repository.api;

import javax.jcr.observation.Event;

public interface RevisionEvent extends Event {

    public long getRevision();

}
