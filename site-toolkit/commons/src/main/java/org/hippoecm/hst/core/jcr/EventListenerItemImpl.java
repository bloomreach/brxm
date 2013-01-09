/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.jcr;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

public class EventListenerItemImpl implements EventListenerItem {
    
    protected int eventTypes;
    protected String absolutePath;
    protected boolean deep;
    protected String [] uuids;
    protected String [] nodeTypeNames;
    protected boolean noLocal;
    protected EventListener eventListener;
    
    public int getEventTypes() {
        return eventTypes;
    }
    
    public void setEventTypes(int eventTypes) {
        this.eventTypes = eventTypes;
    }
    
    public boolean isNodeAddedEnabled() {
        return (Event.NODE_ADDED == (Event.NODE_ADDED & this.eventTypes));
    }

    public void setNodeAddedEnabled(boolean nodeAddedEnabled) {
        if(nodeAddedEnabled) {
            this.eventTypes |= Event.NODE_ADDED;
        } else {
            // flip the bit 
            this.eventTypes &=  (0xFF^Event.NODE_ADDED); 
        }
    }

    public boolean isNodeRemovedEnabled() {
        return (Event.NODE_REMOVED == (Event.NODE_REMOVED & this.eventTypes));
    }

    public void setNodeRemovedEnabled(boolean nodeRemovedEnabled) {
        if(nodeRemovedEnabled) {
            this.eventTypes |= Event.NODE_REMOVED;
        } else {
            // flip the bit 
            this.eventTypes &=  (0xFF^Event.NODE_REMOVED); 
        }
    }

    public boolean isPropertyAddedEnabled() {
        return (Event.PROPERTY_ADDED == (Event.PROPERTY_ADDED & this.eventTypes));
    }

    public void setPropertyAddedEnabled(boolean propertyAddedEnabled) {
        if(propertyAddedEnabled) {
            this.eventTypes |= Event.PROPERTY_ADDED;
        } else {
            // flip the bit 
            this.eventTypes &=  (0xFF^Event.PROPERTY_ADDED); 
        }
    }

    public boolean isPropertyChangedEnabled() {
        return (Event.PROPERTY_CHANGED == (Event.PROPERTY_CHANGED & this.eventTypes));
    }

    public void setPropertyChangedEnabled(boolean propertyChangedEnabled) {
        if(propertyChangedEnabled) {
            this.eventTypes |= Event.PROPERTY_CHANGED;
        } else {
            // flip the bit 
            this.eventTypes &=  (0xFF^Event.PROPERTY_CHANGED); 
        }
    }

    public boolean isPropertyRemovedEnabled() {
        return (Event.PROPERTY_REMOVED == (Event.PROPERTY_REMOVED & this.eventTypes));
    }

    public void setPropertyRemovedEnabled(boolean propertyRemovedEnabled) {
        if(propertyRemovedEnabled) {
            this.eventTypes |= Event.PROPERTY_REMOVED;
        } else {
            // flip the bit 
            this.eventTypes &=  (0xFF^Event.PROPERTY_REMOVED); 
        }
    }

    public String getAbsolutePath() {
        return absolutePath;
    }
    
    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }
    
    public boolean isDeep() {
        return deep;
    }
    
    public void setDeep(boolean deep) {
        this.deep = deep;
    }
    
    public String[] getUuids() {
        if (uuids == null) {
            return null;
        }
        
        String [] cloned = new String[uuids.length];
        System.arraycopy(uuids, 0, cloned, 0, uuids.length);
        return cloned;
    }
    
    public void setUuids(String[] uuids) {
        if (uuids == null) {
            this.uuids = null;
        } else {
            this.uuids = new String[uuids.length];
            System.arraycopy(uuids, 0, this.uuids, 0, uuids.length);
        }
    }
    
    public String[] getNodeTypeNames() {
        if (nodeTypeNames == null) {
            return null;
        }
        
        String [] cloned = new String[nodeTypeNames.length];
        System.arraycopy(nodeTypeNames, 0, cloned, 0, nodeTypeNames.length);
        return cloned;
    }
    
    public void setNodeTypeNames(String[] nodeTypeNames) {
        if (nodeTypeNames == null) {
            this.nodeTypeNames = null;
        } else {
            this.nodeTypeNames = new String[nodeTypeNames.length];
            System.arraycopy(nodeTypeNames, 0, this.nodeTypeNames, 0, nodeTypeNames.length);
        }
    }
    
    public boolean isNoLocal() {
        return noLocal;
    }
    
    public void setNoLocal(boolean noLocal) {
        this.noLocal = noLocal;
    }
    
    public EventListener getEventListener() {
        return eventListener;
    }
    
    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

}
