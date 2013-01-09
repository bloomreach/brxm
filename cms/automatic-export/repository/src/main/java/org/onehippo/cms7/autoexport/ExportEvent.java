/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.autoexport;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

class ExportEvent implements Event {

    private final int type;
    private final String path;
    
    private String stringValue;
    
    ExportEvent(int type, String path) {
        this.type = type;
        this.path = path;
    }
    
    ExportEvent(Event event) throws RepositoryException {
        this.type = event.getType();
        this.path = event.getPath();
    }
    
    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getUserID() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map getInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUserData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getDate() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExportEvent)) {
            return false;
        }
        ExportEvent event = (ExportEvent) o;
        if (event.getType() != type) {
            return false;
        }
        if (!event.getPath().equals(path)) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = 37;
        result = 31 * result + type;
        result = 31 * result + path.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        if (stringValue == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Event: Path: ");
            sb.append(getPath());
            sb.append(", Type: ");
            sb.append(valueOf(getType()));
            stringValue = sb.toString();
        }
        return stringValue;
    }

    /**
     * Returns a String representation of <code>eventType</code>.
     *
     * @param eventType an event type defined by {@link Event}.
     * @return a String representation of <code>eventType</code>.
     */
    public static String valueOf(int eventType) {
        if (eventType == Event.NODE_ADDED) {
            return "NodeAdded";
        } else if (eventType == Event.NODE_MOVED) {
            return "NodeMoved";
        } else if (eventType == Event.NODE_REMOVED) {
            return "NodeRemoved";
        } else if (eventType == Event.PROPERTY_ADDED) {
            return "PropertyAdded";
        } else if (eventType == Event.PROPERTY_CHANGED) {
            return "PropertyChanged";
        } else if (eventType == Event.PROPERTY_REMOVED) {
            return "PropertyRemoved";
        } else {
            return "UnknownEventType";
        }
    }

}
