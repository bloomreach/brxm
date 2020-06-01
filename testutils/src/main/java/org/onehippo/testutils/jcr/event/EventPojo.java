/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.testutils.jcr.event;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.commons.lang3.StringUtils;

public class EventPojo {

    private final int eventType;
    private final String path;
    private final String srcChildRelPath;
    private final String destChildRelPath;

    EventPojo(final int eventType, final String path) {
        this(eventType, path, null ,null);
    }

    EventPojo(final int eventType, final String path, final String srcChildRelPath, final String destChildRelPath) {
        this.eventType = eventType;
        this.path = path;
        this.srcChildRelPath = srcChildRelPath;
        this.destChildRelPath = destChildRelPath;
    }

    static EventPojo from(Event jcrEvent) throws RepositoryException {
        return new EventPojo(
                jcrEvent.getType(),
                jcrEvent.getPath(),
                (String) jcrEvent.getInfo().get("srcChildRelPath"),
                (String) jcrEvent.getInfo().get("destChildRelPath"));
    }

    public int getEventType() {
        return eventType;
    }

    public String getPath() {
        return path;
    }

    public String getSrcChildRelPath() {
        return srcChildRelPath;
    }

    public String getDestChildRelPath() {
        return destChildRelPath;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EventPojo)) return false;
        final EventPojo other = (EventPojo) object;
        return eventType == other.eventType
                && StringUtils.equals(path, other.path)
                && StringUtils.equals(srcChildRelPath, other.srcChildRelPath)
                && StringUtils.equals(destChildRelPath, other.destChildRelPath);
    }

    @Override
    public String toString() {
        String string = "Event: path=" + path;
        switch (eventType) {
            case Event.NODE_ADDED:
                string += " type:NODE_ADDED";
                break;
            case Event.NODE_MOVED:
                string += " type:NODE_MOVED";
                break;
            case Event.NODE_REMOVED:
                string += " type:NODE_REMOVED";
                break;
            case Event.PROPERTY_ADDED:
                string += " type:PROPERTY_ADDED";
                break;
            case Event.PROPERTY_CHANGED:
                string += " type:PROPERTY_CHANGED";
                break;
            case Event.PROPERTY_REMOVED:
                string += " type:PROPERTY_REMOVED";
                break;
        }
        if (srcChildRelPath != null) {
            string += " srcChildRelPath:" + srcChildRelPath;
        }
        if (destChildRelPath != null) {
            string += " destChildRelPath:" + destChildRelPath;
        }
        return string;
    }

}
