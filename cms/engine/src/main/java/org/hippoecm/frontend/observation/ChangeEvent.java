package org.hippoecm.frontend.observation;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

class ChangeEvent implements Event {

    private final String userID;
    private final String nodePath;

    ChangeEvent(final String nodePath, final String userID) {
        this.nodePath = nodePath;
        this.userID = userID;
    }

    public String getPath() throws RepositoryException {
        return nodePath;
    }

    public int getType() {
        return 0;
    }

    public String getUserID() {
        return userID;
    }

    public String getIdentifier() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Map getInfo() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getUserData() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getDate() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}