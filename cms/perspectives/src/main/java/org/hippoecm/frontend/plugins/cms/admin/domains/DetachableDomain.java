/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DetachableDomain extends LoadableDetachableModel<Domain> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DetachableUser.class);

    private final String path;

    protected Node getRootNode() throws RepositoryException {
        return UserSession.get().getJcrSession().getRootNode();
    }

    /**
     * @param domain the Domain to wrap
     */
    public DetachableDomain(final Domain domain) {
        this(domain.getPath());
    }

    /**
     * @param path the path to the Domain
     */
    public DetachableDomain(final String path) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.path = path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * used for dataview with ReuseIfModelsEqualStrategy item reuse strategy
     *
     * @see org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof DetachableDomain) {
            DetachableDomain other = (DetachableDomain) obj;
            return path.equals(other.path);
        }
        return false;
    }

    /**
     * @see org.apache.wicket.model.LoadableDetachableModel#load()
     */
    @Override
    protected Domain load() {
        // loads contact from jcr
        try {
            return new Domain(getRootNode().getNode(path));
        } catch (RepositoryException e) {
            log.error("Unable to load domain, returning null", e);
            return null;
        }
    }
}
