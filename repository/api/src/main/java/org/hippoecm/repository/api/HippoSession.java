/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.transaction.xa.XAResource;

import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.security.User;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.xml.ContentResourceLoader;
import org.onehippo.repository.xml.ImportResult;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * An extension of a plain {@link javax.jcr.Session} based session.  Any JCR Session obtained from the Hippo Repository
 * also implements HippoSession allowing access to the extensions to the JCR API.
 */
public interface HippoSession extends Session {

    /**
     * Convenience function to copy a node to a destination path in the same workspace.  Unlike the copy method in the javax.jcr.Workspace class,
     * this copy method does not immediately persist (i.e. requires a save operation on the session, or ancestor node of the destination path) and it does
     * return the produced copy.  The latter makes this the preferred method to copy a node as the method on the Workspace makes it impossible to know for
     * sure which node is produced in case of same-name siblings.  It also provides a save method for copying node types that are extensions of the Hippo repository.
     * @param srcNode the node to copy
     * @param destAbsNodePath the absolute path of the to be created target
     * node which will be a copy of srcNode
     * @return the resulting copy
     * @throws RepositoryException a generic error while accessing the repository
     * @deprecated Use {@link org.hippoecm.repository.util.JcrUtils#copy(javax.jcr.Session, String, String)} instead
     */
    Node copy(Node srcNode, String destAbsNodePath) throws RepositoryException;

    /**
     * Obtains an iterator over the set of nodes that potentially contain
     * changes, starting (and not including) the indicated node.
     * Only nodes for which <code>Node.isNodeType(nodeType)</code> returns
     * true are included in the resulting set.  If the prune boolean value is
     * true, then the nodes matching in the hierarchy first are returned.  If
     * matching modified node exists beneath the nodes, these are not
     * included.
     *
     * @param node The starting node for which to look for changes, will not
     *             be included in result, may be null to indicate to search whole tree
     * @param nodeType Only nodes that are (derived) of this nodeType are
     *                 included in the result, may be null to indicate no filtering on nodeType
     * @param prune Wheter only to return the first matching modified node in
     *              a subtree (true), or provide a depth search for all modified
     *              nodes (false)
     * @throws NamespaceException an invalid nodeType was passed
     * @throws RepositoryException a generic error while accessing the repository
     * @throws NoSuchNodeTypeException an invalid nodeType was passed
     * @return A NodeIterator instance which iterates over all modified
     *         nodes, not including the passed node
     */
    NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
            NoSuchNodeTypeException, RepositoryException;

    /** Conveniance method for
     * <code>pendingChanges(node,nodeType,false)</code>
     *
     * @param node The starting node for which to look for changes, will not
     *             be included in result, may be null to indicate to search whole tree
     * @param nodeType Only nodes that are (derived) of this nodeType are
     *                 included in the result, may be null to indicate no filtering on nodeType
     * @throws NamespaceException an invalid nodeType was passed
     * @throws RepositoryException a generic error while accessing the repository
     * @throws NoSuchNodeTypeException an invalid nodeType was passed
     * @return A NodeIterator instance which iterates over all modified
     *         nodes, not including the passed node
     * @see #pendingChanges(Node,String,boolean)
     */
    NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException,
            NoSuchNodeTypeException, RepositoryException;


    /** Largely a conveniance method for
     * <code>pendingChanges(Session.getRootNode(), "nt:base", false)</code> however
     * will also return the root node if modified.
     *
     * @return A NodeIterator instance which iterates over all modified nodes, including the root
     * @throws RepositoryException
     * @see #pendingChanges(Node,String,boolean)
     */
    NodeIterator pendingChanges() throws RepositoryException;

    /**
     * Export a dereferenced view of a node.
     *
     * @param absPath the absolute path to the subtree to be exported
     * @param out the output stream to which the resulting XML should be outputted
     * @param binaryAsLink whether to include binaries
     * @param noRecurse whether to output just a single node or the whole subtree
     * @throws IOException in case of an error writing to the output stream
     * @throws RepositoryException a generic error while accessing the repository
     * @throws PathNotFoundException in case the absPath parameter does not point to a valid node
     * @see javax.jcr.Session#exportSystemView(String,OutputStream,boolean,boolean)
     */
    void exportDereferencedView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException;

    /**
     * Export a dereferenced view of a node.
     *
     * @param absPath the absolute path to the subtree to be exported
     * @param contentHandler The  <code>org.xml.sax.ContentHandler</code> to
     *                       which the SAX events representing the XML serialization of the subgraph
     *                       will be output.
     * @param binaryAsLink whether to include binaries
     * @param noRecurse whether to output just a single node or the whole subtree
     * @throws RepositoryException a generic error while accessing the repository
     * @throws PathNotFoundException in case the absPath parameter does not point to a valid node
     * @see javax.jcr.Session#exportSystemView(String,OutputStream,boolean,boolean)
     */
    void exportDereferencedView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException;

    /**
     * Import an enhanced system view xml file.
     *
     * @param parentAbsPath the parent node below which to in
     * @param in the input stream from which to read the XML
     * @param uuidBehavior how to handle deserialized UUIDs in the input stream {@link javax.jcr.ImportUUIDBehavior}
     * @param referenceBehavior an options flag containing one of the values of {@link org.hippoecm.repository.api.ImportReferenceBehavior} indicating how to handle references
     * @param referredResourceLoader the content resouce loader to load the referred imported content resources
     * @throws IOException if incoming stream is not a valid XML document.
     * @throws RepositoryException a generic error while accessing the repository
     * @see #exportDereferencedView(String,OutputStream,boolean,boolean)
     * @see javax.jcr.Session#importXML(java.lang.String, java.io.InputStream, int)
     * @see org.hippoecm.repository.api.ImportReferenceBehavior
     */
    ImportResult importEnhancedSystemViewXML(String parentAbsPath, InputStream in,
                                             int uuidBehavior, int referenceBehavior,
                                             ContentResourceLoader referredResourceLoader)
            throws IOException, RepositoryException;

    File exportEnhancedSystemViewPackage(String parentAbsPath, boolean recurse)
            throws IOException, RepositoryException;

    /**
     * Retrieves an {@link XAResource} object that the transaction manager
     * will use to manage this XASession object's participation in
     * a distributed transaction.
     *
     * @return the {@link XAResource} object.
     */
    XAResource getXAResource();

    /**
     * <b>This call is not (yet) part of the API, but under evaluation.</b>
     * Probably it will change into getSessionClassLoader(Node) or similar.
     * Get a classloader which uses the JCR  repository to load the classes from.
     * @return a classloader instance that will load class definitions stored in the JCR repository
     * @throws RepositoryException  a generic error while accessing the repository
     */
    ClassLoader getSessionClassLoader() throws RepositoryException;

    /**
     * If this is a JCR System Session, having all privileges everywhere.
     * <p>
     *     For a JCR System Session {@link #isUserInRole(String)} and {@link #hasPermission(String, String)} will always
     *     return true!
     * </p>
     * <p>
     *     This should <em>NOT</em> be confused with {@link User#isSystemUser()} which indicates a required and non-human
     *     <em>type</em> of user necessary for the <em>running system</em> itself.
     * </p>
     * <p>
     *     A JCR System Session also does <em>NOT</em> have a {@link User} representation and calling {@link #getUser()}
     *     for a JCR Session Session will result in a {@link ItemNotFoundException}!
     * </p>
     * @return true if this a JCR System Session, having all privileges everywhere.
     */
    boolean isSystemSession();

    /**
     * Get the {@link User} object identified by this session's user id.
     * @return  the {@link User} object identified by this session's user id.
     * @throws ItemNotFoundException when {{@link #isSystemSession()}} returns true
     */
    SessionUser getUser() throws ItemNotFoundException;

    /**
     * Check if a user has the specified userRole assigned, or is implied by one of the userRoles assigned.
     * <p>
     *  For a {@link #isSystemSession()} this always returns true.
     * </p>
     * @param userRoleName the user role name to check
     * @return true if the user has the specified directly or indirectly assigned
     */
    boolean isUserInRole(final String userRoleName);

    /**
     * Create a new Session that contains the union of access control rules
     * of this Session and the provided session, with the optional addition
     * of custom domain rules.  Those rules will be added to existing domain
     * rules, imposing additional restrictions on the session.
     */
    Session createSecurityDelegate(Session session, DomainRuleExtension... domainExtensions) throws RepositoryException;

    /**
     * This method discards all pending changes currently recorded in this <code>Session</code>,
     * including the built-up virtual node states. The difference with {@link Session#refresh(boolean false)} is
     * that after this method returns, the state of the items is not guaranteed to reflect the current
     * persistent storage because in a clustered environment, the cluster node is not synced as a result of this call.
     */
    void localRefresh();

    /**
     * Disable virtual layers.
     */
    void disableVirtualLayers();

    /**
     * Convenient return type override for getting the HippoWorkspace without needing to typecast
     * @return HippoWorkspace
     */
    HippoWorkspace getWorkspace();
}
