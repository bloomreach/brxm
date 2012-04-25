package org.hippoecm.frontend.plugins.cms.admin.domains;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.ActivityViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 */
public class MockNode implements Node {

    @Override
    public boolean hasProperty(final String relPath) throws RepositoryException {
        return false;
    }

    @Override
    public Node addNode(final String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Node addNode(final String relPath, final String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void orderBefore(final String srcChildRelPath, final String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final Value value, final int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final Value[] values, final int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final String[] values, final int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final String value, final int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property setProperty(final String name, final Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Node getNode(final String relPath) throws PathNotFoundException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return new NodeIterator() {
            @Override
            public Node nextNode() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void skip(final long skipNum) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public long getSize() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public long getPosition() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean hasNext() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public Object next() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void remove() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    @Override
    public NodeIterator getNodes(final String namePattern) throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NodeIterator getNodes(final String[] nameGlobs) throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Property getProperty(final String relPath) throws PathNotFoundException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyIterator getProperties(final String namePattern) throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyIterator getProperties(final String[] nameGlobs) throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getIndex() throws RepositoryException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyIterator getReferences(final String name) throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PropertyIterator getWeakReferences(final String name) throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasNode(final String relPath) throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return new NodeType[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isNodeType(final String nodeTypeName) throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setPrimaryType(final String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addMixin(final String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeMixin(final String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean canAddMixin(final String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, ActivityViolationException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doneMerge(final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void cancelMerge(final Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(final String srcWorkspace) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NodeIterator merge(final String srcWorkspace, final boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getCorrespondingNodePath(final String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isCheckedOut() throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void restore(final String versionName, final boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void restore(final Version version, final boolean removeExisting) throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void restore(final Version version, final String relPath, final boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void restoreByLabel(final String versionLabel, final boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Lock lock(final boolean isDeep, final boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean holdsLock() throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isLocked() throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void followLifecycleTransition(final String transition) throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPath() throws RepositoryException {
        return "/test-path";
    }

    @Override
    public String getName() throws RepositoryException {
        return "test-name";
    }

    @Override
    public Item getAncestor(final int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getDepth() throws RepositoryException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session getSession() throws RepositoryException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isNode() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isNew() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isModified() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSame(final Item otherItem) throws RepositoryException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void accept(final ItemVisitor visitor) throws RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void refresh(final boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
