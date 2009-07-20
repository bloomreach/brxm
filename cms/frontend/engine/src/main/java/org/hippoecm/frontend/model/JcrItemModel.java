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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.PrependingStringBuffer;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrItemModel extends LoadableDetachableModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrItemModel.class);

    private String uuid;
    private String relPath;
    private String absPath = null;

    // constructors

    public JcrItemModel(Item item) {
        super(item);
        relPath = null;
        uuid = null;
    }

    public JcrItemModel(String path) {
        super();
        this.absPath = path;
    }

    public String getUuid() {
        save();
        return uuid;
    }

    public String getRelativePath() {
        save();
        return relPath;
    }

    public String getPath() {
        if (absPath == null) {
            if (isAttached() && getObject() != null) {
                Item item = (Item) getObject();
                try {
                    absPath = item.getPath();
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            } else if (uuid != null) {
                javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                try {
                    Node node = session.getNodeByUUID(uuid);
                    if (relPath == null) {
                        return node.getPath();
                    } else if (node.isSame(session.getRootNode())) {
                        return "/" + relPath;
                    } else {
                        return node.getPath() + "/" + relPath;
                    }
                } catch (ItemNotFoundException e) {
                    log.warn("Node not found with uuid: " + uuid);
                } catch (RepositoryException e) {
                    log.error("Error while fetching node with uuid: "+ uuid, e);
                }
            }
        }
        return absPath;
    }

    public boolean exists() {
        String path = getPath();
        if (path != null) {
            try {
                UserSession sessionProvider = (UserSession) Session.get();
                return sessionProvider.getJcrSession().itemExists(path);
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return false;
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
        try {
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            if (uuid != null) {
                Node node = null;
                try {
                    node = session.getNodeByUUID(uuid);
                    if (relPath == null) {
                        return node;
                    }
                    if (node.isSame(session.getRootNode())) {
                        return session.getItem("/" + relPath);
                    } else {
                        return session.getItem(node.getPath() + "/" + relPath);
                    }
                } catch (ItemNotFoundException ex) {
                    if (absPath != null) {
                        uuid = null;
                        relPath = null;
                        return session.getItem(absPath);
                    } else { 
                        throw ex;
                    }
                }
            } else if (absPath != null) {
                return session.getItem(absPath);
            } else {
                log.debug("Neither path nor uuid present for item model, returning null");
            }
        } catch (ItemNotFoundException e) {
            log.info("ItemNotFoundException while loading JcrItemModel for uuid: {}", uuid);
        } catch (PathNotFoundException e) {
            log.info("PathNotFoundException while loading JcrItemModel: {}", e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Failed to load JcrItemModel", e);
        }
        return null;
    }
    
    @Override
    public void detach() {
        save();
        super.detach();
    }

    private void save() {
        if (uuid == null) {
            try {
                relPath = null;
                Node node = null;
                PrependingStringBuffer spb = new PrependingStringBuffer();

                Item item = (Item) getObject();
                // determine uuid + relative path for attached item
                if (item != null) {
                    if (item.isNode()) {
                        node = (Node) item;
                    } else {
                        node = item.getParent();
                        spb.prepend(item.getName());
                        spb.prepend('/');
                    }
                } else if (absPath != null) {
                    javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                    String path = absPath;
                    while (path.lastIndexOf('/') > 0) {
                        spb.prepend(path.substring(path.lastIndexOf('/')));
                        path = path.substring(0, path.lastIndexOf('/'));
                        try {
                            node = (Node) session.getItem(path);
                            break;
                        } catch (PathNotFoundException ex) {
                            continue;
                        }
                    }
                } else {
                    log.debug("Neither path nor uuid present");
                    return;
                }

                while (node != null && !node.isNodeType("mix:referenceable")) {
                    if(node.getIndex() > 1) {
                        spb.prepend(']');
                        spb.prepend(new Integer(node.getIndex()).toString());
                        spb.prepend('[');
                    }
                    spb.prepend(node.getName());
                    spb.prepend('/');
                    node = node.getParent();
                }

                if (node == null) {
                    throw new IllegalStateException("No referenceable parent node was found");
                }

                uuid = node.getUUID();
                if (spb.length() > 1) {
                    relPath = spb.toString().substring(1);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
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
        JcrItemModel that = (JcrItemModel) object;
        save();
        that.save();
        return new EqualsBuilder().append(uuid, that.uuid).append(relPath, that.relPath).isEquals();
    }

    @Override
    public int hashCode() {
        save();
        return new HashCodeBuilder(177, 3).append(uuid).append(relPath).toHashCode();
    }

}
