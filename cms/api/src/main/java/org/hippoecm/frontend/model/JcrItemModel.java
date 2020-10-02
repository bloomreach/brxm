/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.wicket.Application;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.PrependingStringBuffer;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model for JCR {@link Item}s.  The model tracks the Item as well as it can, using the first referencable ancestor
 * plus a relative path as the identification/retrieval method. When the Item (or one of its ancestors) is moved, this
 * is transparent.
 * <p>
 * In development, when the model is serialized, it checks whether it has been detached properly.
 */
public class JcrItemModel<T extends Item> extends LoadableDetachableModel<T> {

    static final Logger log = LoggerFactory.getLogger(JcrItemModel.class);

    // the leading id of the item is the (uuid,relPath) tuple.
    private String uuid;
    private String relPath;
    // the path of the item, used to retrieve the item when the uuid has not been
    // determined yet or the uuid cannot be resolved.
    private String absPath;
    private boolean isProperty;
    private int hash;

    private final String userId;

    // recursion detection
    private transient boolean detaching = false;

    public JcrItemModel(final T item) {
        super(item);

        userId = UserSession.get().getJcrSession().getUserID();

        if (item != null) {
            TraceMonitor.track(item);
            isProperty = !item.isNode();
            doSave();
        }
    }

    public JcrItemModel(final String path, final boolean property) {
        absPath = path;
        isProperty = property;
        userId = UserSession.get().getJcrSession().getUserID();
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
        checkLiveJcrSession();

        final Item item = getObject();
        if (item != null) {
            try {
                absPath = item.getPath();
                return absPath;
            } catch (final InvalidItemStateException e) {
                // ignore, item has been removed
                log.debug("Item " + absPath + " no longer exists", e);
            } catch (final RepositoryException e) {
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
        checkLiveJcrSession();
        return getObject() != null;
    }

    /**
     * Retrieve the JcrItemModel for the parent {@link Node}.
     *
     * @return the parent JcrItemModel
     */
    public JcrItemModel<Node> getParentModel() {
        final String path = getPath();
        if (path == null) {
            return null;
        }

        if (path.equals("/")) {
            return null;
        }

        final int indexOfLastSlash = path.lastIndexOf('/');
        if (indexOfLastSlash == -1) {
            log.error("Unrecognised path " + path);
            return null;
        }

        if (indexOfLastSlash == 0) {
            return new JcrItemModel<>("/", false);
        }

        final String parentPath = path.substring(0, indexOfLastSlash);
        return new JcrItemModel<>(parentPath, false);
    }

    @Override
    protected T load() {
        final T object = loadModel();
        if (object != null) {
            TraceMonitor.track(object);
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    protected T loadModel() {
        final javax.jcr.Session session = UserSession.get().getJcrSession();
        if (!session.isLive()) {
            log.warn("session no longer exists");
            return null;
        }

        if (uuid == null && (absPath == null || absPath.isEmpty())) {
            log.debug("Neither path nor uuid present for item model, returning null");
            return null;
        }

        try {
            if (uuid != null) {
                try {
                    final Node node = session.getNodeByIdentifier(uuid);
                    if (relPath == null) {
                        absPath = node.getPath();
                    } else if (node.isSame(session.getRootNode())) {
                        absPath = "/" + relPath;
                    } else {
                        absPath = node.getPath() + "/" + relPath;
                    }

                    return isProperty
                            ? (T) session.getProperty(absPath)
                            : (T) session.getNode(absPath);

                } catch (final InvalidItemStateException | ItemNotFoundException ex) {
                    if (absPath == null) {
                        throw ex;
                    }

                    uuid = null;
                    relPath = null;
                }
            }

            return isProperty
                    ? (T) session.getProperty(absPath)
                    : (T) session.getNode(absPath);
        } catch (final ItemNotFoundException e) {
            log.info("ItemNotFoundException while loading JcrItemModel for uuid: {}", uuid);
        } catch (final PathNotFoundException e) {
            log.info("PathNotFoundException while loading JcrItemModel: {}", e.getMessage());
        } catch (final RepositoryException e) {
            log.warn("Failed to load JcrItemModel", e);
        }
        return null;
    }

    @Override
    public void detach() {
        if (isAttached()) {
            final T object = this.getObject();
            if (object != null) {
                TraceMonitor.release(object);
            }
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
            final PrependingStringBuffer spb = new PrependingStringBuffer();

            // if we have an item, use it to update the path
            final Item item = getObject();
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
                } catch (final InvalidItemStateException ex) {
                    // ignore; item doesn't exist anymore
                    super.detach();
                }
            }

            // no node was found, use path to resolve an ancestor
            if (node == null) {
                if (absPath == null) {
                    log.debug("Neither path nor uuid present");
                    return;
                }

                final Session session = UserSession.get().getJcrSession();
                String path = absPath;
                while (path.lastIndexOf('/') > 0) {
                    spb.prepend(path.substring(path.lastIndexOf('/')));
                    path = path.substring(0, path.lastIndexOf('/'));
                    try {
                        node = (Node) session.getItem(path);
                        break;
                    } catch (final PathNotFoundException ignored) {
                    }
                }
            }

            while (JcrHelper.isVirtualNode(node)) {
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
        } catch (final RepositoryException ex) {
            log.error(ex.toString());
        }
    }


    private void writeObject(final ObjectOutputStream output) throws IOException {
        if (isAttached()) {
            log.warn("Undetached JcrItemModel {}", this);
            final T object = this.getObject();
            if (object != null) {
                TraceMonitor.trace(object);
            }
            detach();
        }
        output.defaultWriteObject();
    }

    @Override
    public String toString() {
        if (detaching) {
            return super.toString();
        }

        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("attached", true)
                .append("uuid", uuid)
                .append("relativePath", relPath)
                .append("absolutePath", absPath)
                .build();
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof JcrItemModel)) {
            return false;
        }

        if (this == object) {
            return true;
        }

        @SuppressWarnings("unchecked") final JcrItemModel<T> that = (JcrItemModel<T>) object;
        // Two Objects that compare as equals must generate the same hash code,
        // but two Objects with the same hash code do not have to be equal.
        // this implicitly calls the save method when needed
        if (hashCode() != that.hashCode()) {
            return false;
        }

        if (uuid != null && !uuid.equals(that.uuid)) {
            return false;
        }

        if (relPath == null && that.relPath == null) {
            return true;
        }

        return relPath != null && relPath.equals(that.relPath);
    }

    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }

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
        return hash;
    }

    private boolean isValidSession() {
        if (!Application.exists()) {
            return false;
        }
        final Session session = UserSession.get().getJcrSession();
        return session.getUserID().equals(userId);
    }

    private void checkLiveJcrSession() {
        // method below will throw runtime exception in case of non live session
        UserSession.get().getJcrSession();
    }
}
