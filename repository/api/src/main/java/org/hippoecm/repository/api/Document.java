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
package org.hippoecm.repository.api;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;

/**
 * A Plain Old Java Object (POJO) representing a document in a JCR repository.
 * Instances of this object can be returned by workflow calls to indicate to the callee which document has been created or otherwise affected.
 */
public class Document implements Serializable {

    private String identity = null;

    private transient Node node;

    /** 
     * Constructor that should be considered to have a protected signature rather than public and may be used for extending classes to
     * create a new Document.
     */
    public Document() {
    }

    /**
     * Lightweight constructor of a Document only providing a identity.
     * @param identity the identifier of a backing {@link javax.jcr.Node} in the repository that this document instance represents.
     */
    public Document(String identity) {
        this.identity = identity;
    }

    /**
     * Copy constructor which allows to pass on a lightweight Document using its internal backing Node
     * @param document source document to copy the identity and possible the internal backing Node from
     */
    public Document(Document document) {
        this.identity = document.identity;
        this.node = document.node;
    }

    /**
     * Extended classes <b>must</b> honor this constructor!
     * @param node the backing {@link javax.jcr.Node} in the repository that this document instance represents.
     */
    public Document(Node node) throws RepositoryException {
        initialize(node);
    }

    /**
     * Returns the backing Node of this Document, either directly if available and already tied to the provided Session
     * or else retrieved from the provided Session based on its {@link #getIdentity()}.
     * @param session The session for which to return the backing Node
     * @return the backing Node of this Document or null if this Document doesn't contain a identity
     */
    public Node getNode(Session session) throws RepositoryException {
        if (node != null && session == node.getSession()) {
            return node;
        }
        if (identity != null) {
            return session.getNodeByIdentifier(identity);
        }
        return null;
    }

    /**
     * Returns the ensured to be checked out backing Node of this Document, either directly if available and already
     * tied to the provided Session or else retrieved from the provided Session based on its {@link #getIdentity()}.
     * @param session The session for which to return the backing Node
     * @return the ensured to be checked out backing Node of this Document or null if this Document doesn't contain a identity
     */
    public Node getCheckedOutNode(Session session) throws RepositoryException {
        Node node = getNode(session);
        if (node != null) {
            JcrUtils.ensureIsCheckedOut(node);
        }
        return node;
    }

    /**
     * @return true if this document has a backing Node
     */
    public boolean hasNode() {
        return node != null;
    }

    /**
     * Obtain the identity, if known at this point, of a document.  The
     * identity of a Document is the identity of the primary {@link javax.jcr.Node}
     * used in persisting the data of the document.</p>
     * A Document returned for example by a workflow step can be accessed
     * using:
     * <pre>Node node = session.getNodeByIdentifier(document.getIdentity());</pre>
     * or even easier and possibly more efficient:
     * <pre>Node node = document.getNode(session);</pre>
     *
     * @return a string containing the UUID of the Node representing the Document.
     * or <code>null</code> if not available.
     */
    public final String getIdentity() {
        return identity;
    }

    protected Node getNode() {
        return node;
    }

    protected Node getCheckedOutNode() throws RepositoryException {
        JcrUtils.ensureIsCheckedOut(node);
        return node;
    }

    /**
     * Extended classes which need custom/extra initialization based on the backing Node should
     * use the {@link #initialized()} method to get wired into the initialization chain.
     * @param node the backing {@link javax.jcr.Node} in the repository that this document instance represents.
     */
    protected final void initialize(Node node) throws RepositoryException {
        this.node = node;
        this.identity = node.getIdentifier();
        initialized();
    }

    /**
     * Extended classes which need custom/extra initialization based on the backing Node can
     * use this method which will get called after {@link #initialize(javax.jcr.Node)} has been called.
     */
    protected void initialized() {}

    protected String getStringProperty(String relPath) throws RepositoryException {
        return hasNode() ? JcrUtils.getStringProperty(getNode(), relPath, null) : null;
    }

    protected void setStringProperty(String relPath, String value) throws RepositoryException {
        if (hasNode()) {
            Node node = getCheckedOutNode();
            if (value == null) {
                if (node.hasProperty(relPath)) {
                    node.getProperty(relPath).remove();
                }
            } else {
                node.setProperty(relPath, value);
            }
        }
    }

    protected String[] getStringsProperty(String relPath) throws RepositoryException {
        String[] result = null;
        if (hasNode() && getNode().hasProperty(relPath)) {
            Value[] values = getNode().getProperty(relPath).getValues();
            result = new String[values.length];
            int i = 0;
            for (Value v : values) {
                result[i++] = v.getString();
            }
        }
        return result;
    }

    protected void setStringsProperty(String relPath, String[] values) throws RepositoryException {
        if (hasNode()) {
            Node node = getCheckedOutNode();
            if (values == null) {
                if (node.hasProperty(relPath)) {
                    node.getProperty(relPath).remove();
                }
            } else {
                node.setProperty(relPath, values);
            }
        }
    }

    protected Node getNodeProperty(String relPath) throws RepositoryException {
        return hasNode() ? JcrUtils.getNodeProperty(getNode(), relPath, null) : null;
    }

    protected void setNodeProperty(String relPath, Node nodeValue) throws RepositoryException {
        if (hasNode()) {
            getCheckedOutNode().setProperty(relPath, nodeValue);
        }
    }

    protected Date getDateProperty(String relPath) throws RepositoryException {
        Calendar cal = null;
        if (hasNode()) {
            cal = JcrUtils.getDateProperty(getNode(), relPath, null);
        }
        return cal != null ? cal.getTime() : null;
    }

    protected void setDateProperty(String relPath, Date date) throws RepositoryException {
        if (hasNode()) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            getCheckedOutNode().setProperty(relPath, cal);
        }
    }

    protected Long getLongProperty(String relPath) throws RepositoryException {
        return hasNode() ? JcrUtils.getLongProperty(getNode(), relPath, null) : null;
    }

    protected void setLongProperty(String relPath, Long newValue) throws RepositoryException {
        if (hasNode()) {
            getCheckedOutNode().setProperty(relPath, newValue);
        }
    }

    protected Boolean getBooleanProperty(String relPath) throws RepositoryException {
        return hasNode() ? JcrUtils.getBooleanProperty(getNode(), relPath, null) : null;
    }

    protected void setBooleanProperty(String relPath, Boolean newValue) throws RepositoryException {
        if (hasNode()) {
            getCheckedOutNode().setProperty(relPath, newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("[");
        if (identity != null) {
            sb.append("uuid=");
            sb.append(identity);
        } else {
            sb.append("new");
        }
        sb.append("]");
        return new String(sb);
    }
}
