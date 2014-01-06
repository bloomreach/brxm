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
package org.hippoecm.repository.updater;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.impl.NodeDecorator;
import org.onehippo.repository.security.User;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

final public class UpdaterSession implements HippoSession {

    static final Logger log = LoggerFactory.getLogger(UpdaterSession.class);

    Session upstream;
    ValueFactory valueFactory;
    UpdaterNode root;
    UpdaterWorkspace workspace;
    String namespaceTransition = null;
    private Map<String, List<UpdaterProperty>> references;

    public UpdaterSession(Session session) throws UnsupportedRepositoryOperationException, RepositoryException {
        this.upstream = session;
        this.valueFactory = session.getValueFactory();
        this.workspace = new UpdaterWorkspace(this, session.getWorkspace());
        this.references = new HashMap<String, List<UpdaterProperty>>();
        flush();
    }

    public void flush() throws RepositoryException {
        root = new UpdaterNode(this, upstream.getRootNode(), null);
        this.references = new HashMap<String, List<UpdaterProperty>>();
    }

    public NodeType getNewType(String type) throws NoSuchNodeTypeException, RepositoryException {
        return upstream.getWorkspace().getNodeTypeManager().getNodeType(type);
    }

    void relink(Node source, Node target) throws RepositoryException {
        if(source.isNodeType("mix:referenceable")) {
            // deal with Reference properties
            String sourceUUID = source.getUUID();
            Node targetNode = (target.isNodeType("mix:referenceable") ? target : upstream.getRootNode());
            String targetUUID = targetNode.getUUID();
            if (log.isDebugEnabled()) {
                log.debug("remap " + source.getPath() + " from " + sourceUUID + " to " + targetUUID + " (" + targetNode.getPath() + ")");
            }
            Value targetValue = valueFactory.createValue(targetUUID, PropertyType.REFERENCE);
            for(PropertyIterator iter = source.getReferences(); iter.hasNext(); ) {
                Property reference = iter.nextProperty();
                try {
                    if (reference.getParent().isNodeType("mix:versionable")) {
                        reference.getParent().checkout();
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("setting " + reference.getPath() + " from " + sourceUUID + " to " + targetUUID);
                    }
                    if(reference.getDefinition().isMultiple()) {
                        Value[] values = reference.getValues();
                        for(int i=0; i<values.length; i++) {
                            if(values[i].getString().equals(sourceUUID))
                                values[i] = targetValue;
                        }
                        reference.setValue(values);
                    } else {
                        reference.setValue(targetValue);
                    }
                } catch (RepositoryException ex) {
                    log.warn("failed to relink reference", ex);
                }
            }

            // docbases that come after the source node in the traversal
            QueryManager queryMgr = upstream.getWorkspace().getQueryManager();
            Query query = queryMgr.createQuery("//*[(hippo:docbase='" + sourceUUID + "')]", Query.XPATH);
            QueryResult result = query.execute();
            for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                Node reference = iter.nextNode();
                // ignore version storage
                if (reference.getPath().startsWith("/jcr:system")) {
                    continue;
                }
                if (reference.isNodeType("mix:versionable")) {
                    reference.checkout();
                }
                Property property = reference.getProperty("hippo:docbase");
                if (log.isDebugEnabled()) {
                    log.debug("setting " + property.getPath() + " from " + sourceUUID + " to " + targetUUID);
                }
                property.setValue(targetUUID);
            }

            // docbases that come after the source node in the traversal
            if (namespaceTransition != null) {
                query = queryMgr.createQuery("//*[("+namespaceTransition+":docbase='" + sourceUUID + "')]", Query.XPATH);
                result = query.execute();
                for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                    Node reference = iter.nextNode();
                    // ignore version storage
                    if (reference.getPath().startsWith("/jcr:system")) {
                        continue;
                    }
                    if (reference.isNodeType("mix:versionable")) {
                        reference.checkout();
                    }
                    Property property = reference.getProperty(namespaceTransition+":docbase");
                    if (log.isDebugEnabled()) {
                        log.debug("setting " + property.getPath() + " from " + sourceUUID + " to " + targetUUID);
                    }
                    property.setValue(targetUUID);
                }
            }

            // update values of reference properties
            List<UpdaterProperty> properties = references.get(sourceUUID);
            if (properties != null) {
                for (UpdaterProperty property : new ArrayList<UpdaterProperty>(properties)) {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("setting " + property.getPath() + " from " + sourceUUID + " to " + targetUUID);
                        }
                        if (property.isStrongReference()) {
                            // For committed properties, bring their value in line with the persisted value.
                            // Uncommitted properties receive the correct value to commit.
                            if(property.isMultiple()) {
                                Value[] values = property.getValues();
                                for(int i=0; i<values.length; i++) {
                                    if(values[i].getString().equals(sourceUUID))
                                        values[i] = targetValue;
                                }
                                property.setValue(values);
                                ((Property) property.origin).setValue(values);
                            } else {
                                property.setValue(targetValue);
                                ((Property) property.origin).setValue(targetValue);
                            }
                        } else {
                            property.setValue(targetValue);
                            ((Property) property.origin).setValue(targetValue);
                        }
                    } catch (RepositoryException ex) {
                        log.warn("failed to relink reference", ex);
                    }
                }
                assert (!references.containsKey(sourceUUID));
            }
        }
    }

    void addReference(UpdaterProperty property, String uuid) throws RepositoryException {
        if (!uuid.startsWith("cafebabe-") && !property.getName().startsWith("jcr:")) {
            if (log.isDebugEnabled()) {
                log.debug("adding " + property.getPath() + " to docbases for " + uuid);
            }
            if (!references.containsKey(uuid)) {
                references.put(uuid, new LinkedList<UpdaterProperty>());
            }
            List<UpdaterProperty> properties = references.get(uuid);
            properties.add(property);
        }
    }
    
    void removeReference(UpdaterProperty property, String uuid) throws RepositoryException {
        if (!uuid.startsWith("cafebabe-") && !property.getName().startsWith("jcr:")) {
            if (log.isDebugEnabled()) {
                log.debug("removing " + property.getPath() + " from docbases for " + uuid);
            }
            if (!references.containsKey(uuid)) {
                log.warn("Property " + property.getPath() + " was not found");
                return;
            }
            List<UpdaterProperty> properties = references.get(uuid);
            if (!properties.contains(property)) {
                log.warn("Property " + property.getPath() + " was not found");
                return;
            }
            properties.remove(property);
            if (properties.size() == 0) {
                references.remove(uuid);
            }
        }
    }

    public void commit() throws RepositoryException {
        root.commit();
        this.root = new UpdaterNode(this, upstream.getRootNode(), null);
    }

    // interface javax.jcr.Session

    @Deprecated
    public Repository getRepository() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String getUserID() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Object getAttribute(String name) {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String[] getAttributeNames() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Workspace getWorkspace() {
        return workspace;
    }

    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Node getRootNode() throws RepositoryException {
        return root;
    }

    @Deprecated
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        Node node = getRootNode();
        for (StringTokenizer iter = new StringTokenizer(absPath.substring(1), "/"); iter.hasMoreTokens();) {
            node = node.getNode(iter.nextToken());
        }
        return node;
    }

    public boolean itemExists(String absPath) throws RepositoryException {
        try {
            getItem(absPath);
            return true;
        } catch (PathNotFoundException ex) {
            return false;
        }
    }

    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        UpdaterNode node = (UpdaterNode) getItem(srcAbsPath);
        UpdaterNode destination = (UpdaterNode) getItem(destAbsPath.substring(0, destAbsPath.lastIndexOf("/")));
        if (node.predecessor == null) {
            node.parent.head = node.successor;
        } else {
            node.predecessor.successor = node.successor;
        }
        if (node.successor == null) {
            node.parent.tail = node.predecessor;
        } else {
            node.successor.predecessor = node.predecessor;
        }
        node.predecessor = destination.tail;
        node.successor = null;
        if (node.predecessor != null) {
            node.predecessor.successor = node;
        }
        destination.tail = node;
        node.parent = destination;
        node.setName(destAbsPath.substring(destAbsPath.lastIndexOf("/")));
    }

    @Deprecated
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void refresh(boolean keepChanges) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean hasPendingChanges() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return valueFactory;
    }

    @Deprecated
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void setNamespacePrefix(String newPrefix, String existingUri) throws NamespaceException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public String[] getNamespacePrefixes() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void logout() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isLive() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void addLockToken(String lt) {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String[] getLockTokens() {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void removeLockToken(String lt) {
        throw new UpdaterException("illegal method");
    }

    public Node copy(Node srcNode, String destAbsNodePath) throws PathNotFoundException, ItemExistsException,
            LockException, VersionException, RepositoryException {
        if (destAbsNodePath.startsWith(srcNode.getPath()+"/")) {
            String msg = srcNode.getPath() + ": Invalid destination path (cannot be descendant of source path)";
            throw new RepositoryException(msg);
        }

        while (destAbsNodePath.startsWith("/")) {
            destAbsNodePath = destAbsNodePath.substring(1);
        }
        Node destNode = getRootNode();
        int p = destAbsNodePath.lastIndexOf("/");
        if (p > 0) {
            destNode = destNode.getNode(destAbsNodePath.substring(0, p));
            destAbsNodePath = destAbsNodePath.substring(p + 1);
        }
        try {
            destNode = destNode.addNode(destAbsNodePath, srcNode.getPrimaryNodeType().getName());
            copy(srcNode, destNode);
            return destNode;
        } catch (ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }

    public static void copy(Node srcNode, Node destNode) throws ItemExistsException, LockException, RepositoryException {
        try {
            Node canonical;
            boolean copyChildren = true;
            if (srcNode instanceof HippoNode && ((HippoNode) srcNode).isVirtual()) {
                copyChildren = false;
            }

            NodeType[] mixinNodeTypes = srcNode.getMixinNodeTypes();
            for (final NodeType mixinNodeType : mixinNodeTypes) {
                destNode.addMixin(mixinNodeType.getName());
            }

            for (PropertyIterator iter = NodeDecorator.unwrap(srcNode).getProperties(); iter.hasNext();) {
                Property property = iter.nextProperty();
                if (property instanceof UpdaterProperty) {
                    if (((UpdaterProperty)property).isMultiple()) {
                        destNode.setProperty(property.getName(), property.getValues(), property.getType());
                    } else {
                        destNode.setProperty(property.getName(), property.getValue());
                    }
                } else {
                    PropertyDefinition definition = property.getDefinition();
                    if (!definition.isProtected()) {
                        if (definition.isMultiple())
                            destNode.setProperty(property.getName(), property.getValues(), property.getType());
                        else
                            destNode.setProperty(property.getName(), property.getValue());
                    }
                }
            }

            /* Do not copy virtual nodes.  Partial virtual nodes like
             * HippoNodeType.NT_FACETSELECT and HippoNodeType.NT_FACETSEARCH
             * should be copied except for their children, even if though they
             * are half virtual. On save(), the virtual part will be
             * ignored.
             */
            if (copyChildren) {
                for (NodeIterator iter = srcNode.getNodes(); iter.hasNext();) {
                    Node node = iter.nextNode();
                    if (!(node instanceof HippoNode) || ((canonical = ((HippoNode) node).getCanonicalNode()) != null && canonical.isSame(node))) {
                        Node child;
                        // check if the subnode is autocreated
                        if (!(node instanceof UpdaterNode) && node.getDefinition().isAutoCreated() && destNode.hasNode(node.getName())) {
                            child = destNode.getNode(node.getName());
                        } else {
                            child = destNode.addNode(node.getName(), node.getPrimaryNodeType().getName());
                        }
                        copy(node, child);
                    }
                }
            }
        } catch (PathNotFoundException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (VersionException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (ValueFormatException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (ConstraintViolationException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        } catch (NoSuchNodeTypeException ex) {
            throw new RepositoryException("Internal error", ex); // this cannot happen
        }
    }


    @Deprecated
    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public NodeIterator pendingChanges() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void exportDereferencedView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Override
    public void exportDereferencedView(final String absPath, final ContentHandler contentHandler, final boolean binaryAsLink, final boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void importDereferencedXML(String parentAbsPath, InputStream in, int uuidBehavior, int referenceBehavior, int mergeBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Override
    public void importEnhancedSystemViewPackage(final String parentAbsPath, final File pckg, final int uuidBehaviour, final int referenceBehaviour, final int mergeBehaviour) throws IOException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Override
    public File exportEnhancedSystemViewPackage(final String parentAbsPath, final boolean recurse) throws IOException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public ClassLoader getSessionClassLoader() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Override
    public User getUser() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public XAResource getXAResource() {
        throw new UpdaterException("illegal method");
    }

    public Node getNodeByIdentifier(String id) throws ItemNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Node getNode(String absPath) throws PathNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Property getProperty(String absPath) throws PathNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public boolean nodeExists(String absPath) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public boolean propertyExists(String absPath) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void removeItem(String absPath) throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public boolean hasPermission(String absPath, String actions) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public boolean hasCapability(String methodName, Object target, Object[] arguments) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public AccessControlManager getAccessControlManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public RetentionManager getRetentionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void registerSessionCloseCallback(CloseCallback callback) {
        throw new UpdaterException("illegal method");
    }

    @Override
    public Session createSecurityDelegate(final Session session, DomainRuleExtension... domainExtensions) {
        throw new UpdaterException("illegal method");
    }

    @Override
    public void localRefresh() {
        throw new UpdaterException("illegal method");
    }

    @Override
    public void disableVirtualLayers() {
        throw new UpdaterException("illegal method");
    }
}
