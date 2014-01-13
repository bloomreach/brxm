/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock.observation;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

/**
 * Mock version of a {@link Event}.
 */
public class MockEvent implements Event {

    private final int type;
    private final String path;
    private final String userID;
    private final String identifier;
    private final long timestamp;
    private final String userData;
    private final Map<String, String> info = new HashMap<String, String>();

    /**
     * Constructor
     * 
     * @param session JCR session from which the <code>userID</code> is read.
     * @param type JCR observation event type. If an invalid type is given, an <code>IllegalArgumentException</code> will be thrown.
     * @param path JCR observation event node path.
     * @param identifier JCR observation event node identifier.
     * @param userData JCR observation event userData.
     * @param timestamp JCR observation event date timestamp.
     */
    public MockEvent(final Session session, final int type, final String path, final String identifier, final String userData, long timestamp) {
        if (type < NODE_ADDED || type > PERSIST) {
            throw new IllegalArgumentException("Invalid event type: " + type);
        }

        this.type = type;
        this.path = path;
        this.userID = session.getUserID();
        this.identifier = identifier;
        this.userData = userData;
        this.timestamp = timestamp;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getPath() throws RepositoryException {
        return path;
    }

    @Override
    public String getUserID() {
        return userID;
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return identifier;
    }

    /**
     * Returns mutable {@link Map} in order to allow callers to manipulate mock infos easily.
     */
    @Override
    public Map getInfo() throws RepositoryException {
        return info;
    }

    @Override
    public String getUserData() throws RepositoryException {
        return userData;
    }

    @Override
    public long getDate() throws RepositoryException {
        return timestamp;
    }

}
