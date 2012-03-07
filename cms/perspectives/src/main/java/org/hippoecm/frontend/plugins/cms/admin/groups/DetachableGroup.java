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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DetachableGroup extends LoadableDetachableModel<Group> {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DetachableGroup.class);

    private String path;

    protected Node getRootNode() throws RepositoryException {
        return ((UserSession) Session.get()).getJcrSession().getRootNode();
    }

    public DetachableGroup() {
    }

    public DetachableGroup(final Group group) {
        this(group.getPath());
    }

    public DetachableGroup(final String path) {
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
        if (path == null) {
            return super.hashCode();
        }
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
        if (obj == null) {
            return false;
        }

        boolean objIsDetachableGroup = obj instanceof DetachableGroup;
        if (!objIsDetachableGroup) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        DetachableGroup other = (DetachableGroup) obj;
        return (path != null) && (other.path != null) && path.equals(other.path);
    }

    /**
     * @see org.apache.wicket.model.LoadableDetachableModel#load()
     */
    @Override
    protected Group load() {
        if (path == null) {
            return new Group();
        }
        // loads group from jcr
        try {
            return new Group(getRootNode().getNode(path));
        } catch (RepositoryException e) {
            log.error("Unable to re-attach user with path '{}'", path, e);
            return null;
        }
    }

    /**
     * Remove after upgrade to wicket 1.4, which has generics. This is just an alias for (Group) getObject().
     *
     * @return the Group that this Model wraps
     */
    public Group getGroup() {
        return getObject();
    }

}
