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
package org.hippoecm.frontend.model;

import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrItemModel extends LoadableDetachableModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrItemModel.class);

    private String uuid;
    private String path;

    // constructors

    public JcrItemModel(Item item) {
        super(item);
        path = null;
        uuid = null;
    }

    public JcrItemModel(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        if (path == null && isAttached()) {
            Item item = (Item) getObject();
            if (item != null) {
                try {
                    path = item.getPath();
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
        }
        return path;
    }

    public boolean exists() {
        if (path == null) {
            if (isAttached()) {
                return getObject() != null;
            } else {
                return false;
            }
        } else {
            boolean result = false;
            try {
                UserSession sessionProvider = (UserSession) Session.get();
                result = sessionProvider.getJcrSession().itemExists(path);
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            return result;
        }
    }

    public JcrItemModel getParentModel() {
        String path = getPath();
        if (path != null) {
            int idx = path.lastIndexOf('/');
            if (idx > 0) {
                String parent = path.substring(0, path.lastIndexOf('/'));
                return new JcrItemModel(parent);
            } else if (idx == 0) {
                if (path.equals("/")) {
                    return null;
                }
                return new JcrItemModel("/");
            } else {
                log.error("Unrecognised path " + path);
            }
        }
        return null;
    }

    public boolean hasAncestor(JcrItemModel model) {
        if (getPath() != null) {
            if (model.getPath() != null) {
                return getPath().startsWith(model.getPath());
            }
        }
        return false;
    }

    // LoadableDetachableModel

    @Override
    protected Object load() {
        Item result = null;
        if (uuid != null) {
            try {
                UserSession sessionProvider = (UserSession) Session.get();
                result = sessionProvider.getJcrSession().getNodeByUUID(uuid);
            } catch (RepositoryException e) {
                log.warn("failed to load " + e.getMessage());
            }
        } else if (path != null) {
            try {
                UserSession sessionProvider = (UserSession) Session.get();
                result = sessionProvider.getJcrSession().getItem(path);
                if(result != null && result.isNode()) {
                    Node node = (Node) result;
                    if (node.isNodeType("mix:referenceable")) {
                        uuid = node.getUUID();
                    }
                }
            } catch (RepositoryException e) {
                log.warn("failed to load " + e.getMessage());
            }
        } else {
            log.error("No path info present");
        }
        return result;
    }

    @Override
    public void detach() {
        if (isAttached()) {
            // if we have a uuid we're done
            if (uuid == null) {
                if (path != null) {
                    // we have a path but not a uuid, try to find the uuid if possible
                    UserSession sessionProvider = (UserSession) Session.get();
                    try {
                        Item item = sessionProvider.getJcrSession().getItem(path);
                        if (item != null && item.isNode()) {
                            Node node = (Node) item;
                            if (node.isNodeType("mix:referenceable")) {
                                uuid = node.getUUID();
                            }
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                } else {
                    // we have neither a uuid nor a path, try the get the uuid, else get the path from the item
                    Item item = (Item) getObject();
                    if (item != null) {
                        try {
                            if (item.isNode()) {
                                Node node = (Node) item;
                                if (node.isNodeType("mix:referenceable")) {
                                    uuid = node.getUUID();
                                }
                            }
                            path = item.getPath();
                        } catch (RepositoryException ex) {
                            log.error(ex.getMessage());
                        }
                    }
                }
            }
        }
        super.detach();
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        if (isAttached()) {
            log.warn("Undetached JcrItemModel " + getPath());
            detach();
        }
        output.defaultWriteObject();
    }

    // override Object

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("path", getPath()).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof JcrItemModel == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrItemModel itemModel = (JcrItemModel) object;
        return new EqualsBuilder().append(normalizePath(getPath()), normalizePath(itemModel.getPath())).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(177, 3).append(getPath()).toHashCode();
    }

    private static String normalizePath(String path) {
        if (path != null && path.length() > 0) {
            if (path.charAt(path.length() - 1) == ']') {
                return path;
            }
            return path + "[1]";
        }
        return path;
    }
}
