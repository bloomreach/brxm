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
package org.hippoecm.frontend.model;

import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.PrependingStringBuffer;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for JCR {@link Item}s.  The model tracks the Item as well as it can, using the
 * first referenceable ancestor plus a relative path as the identification/retrieval method.
 * When the Item (or one of its ancestors) is moved, this is transparent.
 * <p>
 * In development, when the model is serialized, it checks whether it has been detached properly.
 */
public class JcrItemModel<T extends Item> extends LoadableDetachableModel<T> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrItemModel.class);

    // the leading id of the item is the (uuid,relPath) tuple.
    private String uuid;
    private String relPath;
    private int hash;
    private boolean property;
    private String userId;

    // the path of the item, used to retrieve the item when the uuid has not been
    // determined yet or the uuid cannot be resolved.
    private String absPath = null;

    // recursion detection
    private transient boolean detaching = false;

    // constructors

    public JcrItemModel(T item) {
        super(item);
        setUserId();
        relPath = null;
        uuid = null;
        if (item != null) {
            TraceMonitor.track(item);
            property = !item.isNode();
            doSave();
        }
    }

    @Deprecated
    public JcrItemModel(String path) {
        setUserId();
        absPath = path;
        try {
            final Item item = UserSession.get().getJcrSession().getItem(path);
            TraceMonitor.track(item);
            property = !item.isNode();
        } catch (RepositoryException e) {
            log.warn("Instantiation of item model by path failed: " + e);
        }
    }

    public JcrItemModel(String path, boolean property) {
        setUserId();
        uuid = null;
        absPath = path;
        this.property = property;
    }

    /**
     * Retrieve the identifier (UUID) of the first referencable ancestor node
     *
     * @return the UUID
     */
    public String getUuid() {
        save();
        return uuid;
    }

    /**
     * Retrieve the JCR path for the Item, relative to the first referencable ancestor
     *
     * @return the relative path
     */
    public String getRelativePath() {
        save();
        return relPath;
    }

    /**
     * The absolute JCR path for the Item.
     *
     * @return the absolute path
     */
    public String getPath() {
        Item item = getObject();
        if (item != null) {
            try {
                absPath = item.getPath();
                return absPath;
            } catch (InvalidItemStateException e) {
                // ignore, item has been removed
                log.debug("Item " + absPath + " no longer exists", e);
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
        return absPath;
    }

    /**
     * Determine whether the Item exists.  This will retrieve the Item from the repository.
     * If the Item has been loaded in this request cycle (e.g. using {@link IModel#getObject}), but has since
     * been removed, the returned information may be incorrect.
     *
     * @return true when the Item exists
     */
    public boolean exists() {
        return getObject() != null;
    }

    /**
     * Retrieve the JcrItemModel for the parent {@link Node}.
     *
     * @return the parent JcrItemModel
     */
    public JcrItemModel<Node> getParentModel() {
        String path = getPath();
        if (path != null) {
            int idx = path.lastIndexOf('/');
            if (idx > 0) {
                String parent = path.substring(0, path.lastIndexOf('/'));
                return new JcrItemModel<Node>(parent, false);
            } else if (idx == 0) {
                if (path.equals("/")) {
                    return null;
                }
                return new JcrItemModel<Node>("/", false);
            } else {
                log.error("Unrecognised path " + path);
            }
        }
        return null;
    }

    // LoadableDetachableModel

    @SuppressWarnings("unchecked")
    @Override
    protected T load() {
        T object = loadModel();
        if (object != null) {
            TraceMonitor.track(object);
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    protected T loadModel() {
        try {
            javax.jcr.Session session = UserSession.get().getJcrSession();
            if (!session.isLive()) {
                log.warn("session no longer exists");
                return null;
            }
            if (uuid != null) {
                Node node;
                try {
                    node = session.getNodeByIdentifier(uuid);
                    if (relPath == null) {
                        absPath = node.getPath();
                        return (T) node;
                    }
                    if (node.isSame(session.getRootNode())) {
                        absPath = "/" + relPath;
                    } else {
                        absPath = node.getPath() + "/" + relPath;
                    }
                    if (property) {
                        return (T) session.getProperty(absPath);
                    } else {
                        return (T) session.getNode(absPath);
                    }
                } catch (InvalidItemStateException ex) {
                   if (absPath != null) {
                       uuid = null;
                       relPath = null;
                       if (property) {
                           return (T) session.getProperty(absPath);
                       } else {
                           return (T) session.getNode(absPath);
                       }
                    } else {
                        throw ex;
                    }
                } catch (ItemNotFoundException ex) {
                    if (absPath != null) {
                        uuid = null;
                        relPath = null;
                        if (property) {
                            return (T) session.getProperty(absPath);
                        } else {
                            return (T) session.getNode(absPath);
                        }
                    } else {
                        throw ex;
                    }
                }
            } else if (absPath != null && !absPath.isEmpty()) {
                if (property) {
                    return (T) session.getProperty(absPath);
                } else {
                    return (T) session.getNode(absPath);
                }
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
        T object = this.getObject();
        if (object != null) {
            TraceMonitor.release(object);
        }
        detaching = true;
        save();
        super.detach();
        detaching = false;
    }

    private void save() {
        if (uuid == null) {
            doSave();
        }
    }

    private void doSave() {
        if (!isValidSession()) {
            return;
        }
        try {
            relPath = null;
            Node node = null;
            PrependingStringBuffer spb = new PrependingStringBuffer();

            // if we have an item, use it to update the path
            Item item = getObject();
            if (item != null) {
                try {
                    absPath = item.getPath();
                    if (item.isNode()) {
                        node = (Node) item;
                    } else {
                        node = item.getParent();
                        spb.prepend(item.getName());
                        spb.prepend('/');
                    }
                } catch (InvalidItemStateException ex) {
                    // ignore; item doesn't exist anymore
                    super.detach();
                }
            }

            // no node was found, use path to resolve an ancestor
            if (node == null) {
                if (absPath != null) {
                    Session session = UserSession.get().getJcrSession();
                    String path = absPath;
                    while (path.lastIndexOf('/') > 0) {
                        spb.prepend(path.substring(path.lastIndexOf('/')));
                        path = path.substring(0, path.lastIndexOf('/'));
                        try {
                            node = (Node) session.getItem(path);
                            break;
                        } catch (PathNotFoundException ignored) {
                        }
                    }
                } else {
                    log.debug("Neither path nor uuid present");
                    return;
                }
            }

            while (node != null && JcrHelper.isVirtualNode(node)) {
                if (node.getIndex() > 1) {
                    spb.prepend(']');
                    spb.prepend(Integer.toString(node.getIndex()));
                    spb.prepend('[');
                }
                spb.prepend(node.getName());
                spb.prepend('/');
                node = node.getParent();
            }

            if (node != null) {
                uuid = node.getIdentifier();
                if (spb.length() > 1) {
                    relPath = spb.toString().substring(1);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.toString());
        }
    }


    private void writeObject(ObjectOutputStream output) throws IOException {
        if (isAttached()) {
            log.warn("Undetached JcrItemModel "+getPath());
            T object = this.getObject();
            if (object != null) {
                TraceMonitor.trace(object);
            }
            if (RuntimeConfigurationType.DEPLOYMENT.equals(Application.get().getConfigurationType())) {
                detach();
            }
        }
        output.defaultWriteObject();
    }

    // override Object

    @Override
    public String toString() {
        if (!detaching) {
            boolean isAttached = isAttached();
            String string = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("path", getPath()).toString();
            if (!isAttached) {
                detach();
            }
            return string;
        } else {
            return super.toString();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof JcrItemModel)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        JcrItemModel that = (JcrItemModel) object;
        
        // Two Objects that compare as equals must generate the same hash code,
        // but two Objects with the same hash code do not have to be equal.
        // this implicitly calls the save method when needed
        if (this.hashCode() != that.hashCode()) {
            return false;
        } 

        if (this.uuid != null && !this.uuid.equals(that.uuid)) {
            return false;
        }

        if (this.relPath == null && that.relPath == null) {
            return true;
        } else {
            return this.relPath != null && this.relPath.equals(that.relPath);
        }
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            if (uuid == null) {
                // try to retrieve uuid
                save();
            }
            // prefer uuid over path
            if (uuid != null) {
                if (relPath == null) {
                    hash = uuid.hashCode();
                } else {
                    hash = uuid.hashCode() + relPath.hashCode();
                }
            } else {
                // no node found
                hash = -1;
            }
        }
        return hash;
    }

    private boolean isValidSession() {
        final Session session = UserSession.get().getJcrSession();
        return session.getUserID().equals(userId);
    }

    private void setUserId() {
        userId = UserSession.get().getJcrSession().getUserID();
    }
}
