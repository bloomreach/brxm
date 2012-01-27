/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.behavioral.rest.services;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hippoecm.hst.behavioral.rest.beans.HttpSessionInfo;

/**
 * JAX-RS representation of an {@link HttpSession}.
 */
public class HttpSessionRepresentation implements HttpSessionInfo {

    private static final int HASH_CODE_INITIAL_NON_ZERO_ODD_NUMBER = 47;
    private static final int HASH_CODE_MULTIPLIER_NON_ZERO_ODD_NUMBER = 233;

    private String id;
    private long creationTime;
    private long lastAccessedTime;
    
    public HttpSessionRepresentation() {
    }

    public HttpSessionRepresentation represent(HttpSession session) {
        HttpSessionRepresentation result = new HttpSessionRepresentation();
        result.setId(session.getId());
        result.setCreationTime(session.getCreationTime());
        result.setLastAccessedTime(session.getLastAccessedTime());
        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(final long creationTime) {
        this.creationTime = creationTime;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(final long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof HttpSessionRepresentation) {
            HttpSessionRepresentation other = (HttpSessionRepresentation)o;
            return new EqualsBuilder()
                    .append(id, other.id)
                    .append(creationTime, other.creationTime)
                    .append(lastAccessedTime, other.lastAccessedTime)
                    .isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_CODE_INITIAL_NON_ZERO_ODD_NUMBER, HASH_CODE_MULTIPLIER_NON_ZERO_ODD_NUMBER)
                    .append(id).append(creationTime).append(lastAccessedTime).hashCode();
    }
    
    @Override
    public String toString() {
        return "HttpSessionRepresentation [id=" + id + ", creationTime=" + creationTime + ", lastAccessedTime="
                + lastAccessedTime + "]";
    }

}
