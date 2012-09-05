/*
 *  Copyright 2011 Hippo.
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

import org.apache.jackrabbit.core.observation.EventState;

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
            sb.append(EventState.valueOf(getType()));
            stringValue = sb.toString();
        }
        return stringValue;
    }

}
