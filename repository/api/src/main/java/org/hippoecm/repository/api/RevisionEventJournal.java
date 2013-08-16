package org.hippoecm.repository.api;

import javax.jcr.observation.EventJournal;

public interface RevisionEventJournal extends EventJournal {

    public void skipToRevision(long revision);

    @Override
    public RevisionEvent nextEvent();

}
