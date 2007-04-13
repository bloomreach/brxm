import javax.jcr.*;
import javax.jcr.lock.*;
import javax.jcr.version.*;
import javax.jcr.nodetype.*;
import java.io.InputStream;
import java.util.Calendar;

class VirtualNodeImpl
{
  protected boolean isVirtual;
  protected Node actual;
  protected String virtualPath;

  public VirtualNodeImpl(Node actual) {
    isVirtual = false;
    this.actual = actual;
  }
  public VirtualNodeImpl(Node actual, String path) {
    isVirtual = true;
    this.actual = actual;
    virtualPath = path;
  }

  Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    return actual.getAncestor(depth);
  }
  int getDepth() throws ItemNotFoundException, RepositoryException {
    return actual.getDepth();
  }
  String getName() throws RepositoryException {
    return actual.getName();
  }
  Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
    if(isVirtual)
      return actual.getSession().getRootNode().getNode(virtualPath);
    else
      return actual.getParent();
  }
  String getPath() throws RepositoryException {
    if(isVirtual)
      return virtualPath + "/" + getName();
    else
      return actual.getPath();
  }
  Session getSession() throws RepositoryException {
    return actual.getSession();
  }
  boolean isModified() {
    return actual.isModified();
  }
  boolean isNew() {
    return actual.isNew();
  }
  boolean isNode() {
    return actual.isNode();
  }
  boolean isSame(Item otherItem) throws RepositoryException {
    return actual.isSame(otherItem);
  }
  void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
    actual.refresh(keepChanges);
  }
  void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
    actual.remove();
  }
  void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    actual.save();
  }


  void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, ReferentialIntegrityException, RepositoryException {
    actual.addMixin(mixinName);
  }
  Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    return actual.addNode(relPath);
  }
  Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, RepositoryException {
    return actual.addNode(relPath);
  }
  boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
    return actual.canAddMixin(mixinName);
  }
  void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
    actual.cancelMerge(version);
  }
  Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
    return actual.checkin();
  }
  void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
    actual.checkout();
  }
  void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
    actual.doneMerge(version);
  }
  Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
    return actual.getBaseVersion();
  }
  String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
    return actual.getCorrespondingNodePath(workspaceName);
  }
  NodeDefinition getDefinition() throws RepositoryException {
    return actual.getDefinition();
  }
  int getIndex() throws RepositoryException {
    return actual.getIndex();
  }
  Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
    return actual.getLock();
  }
  NodeType[] getMixinNodeTypes() throws RepositoryException {
    return actual.getMixinNodeTypes();
  }
  Node getNode(String relPath) throws PathNotFoundException, RepositoryException  {
    if(isVirtual) {
      return actual.getSession().getRootNode().getNode("documents").getNode("all").getNode(relPath);
    } else
      return actual.getNode(relPath);
  }
  NodeIterator getNodes() throws RepositoryException {
    return actual.getNodes();
  }
  NodeIterator getNodes(String namePattern) throws RepositoryException {
    return actual.getNodes(namePattern);
  }
  Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
    return actual.getPrimaryItem();
  }
  NodeType getPrimaryNodeType() throws RepositoryException {
    return actual.getPrimaryNodeType();
  }
  PropertyIterator getProperties() throws RepositoryException {
    return actual.getProperties();
  }
  PropertyIterator getProperties(String namePattern) throws RepositoryException {
    return actual.getProperties(namePattern);
  }
  Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
    return actual.getProperty(relPath);
  }
  PropertyIterator getReferences() throws RepositoryException {
    return actual.getReferences();
  }
  String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
    return actual.getUUID();
  }
  VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
    return actual.getVersionHistory();
  }
  boolean hasNode(String relPath) throws RepositoryException {
    return actual.hasNode(relPath);
  }
  boolean hasNodes() throws RepositoryException {
    return actual.hasNodes();
  }
  boolean hasProperties() throws RepositoryException {
    return actual.hasProperties();
  }
  boolean hasProperty(String relPath) throws RepositoryException {
    return actual.hasProperty(relPath);
  }
  boolean holdsLock() throws RepositoryException {
    return actual.holdsLock();
  }
  boolean isCheckedOut() throws RepositoryException {
    return actual.isCheckedOut();
  }
  boolean isLocked() throws RepositoryException {
    return actual.isLocked();
  }
  boolean isNodeType(String nodeTypeName) throws RepositoryException {
    return actual.isNodeType(nodeTypeName);
  }
  Lock lock(boolean isDeep, boolean isSessionScoped) throws RepositoryException {
    return actual.lock(isDeep, isSessionScoped);
  }
  NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
    return actual.merge(srcWorkspace,bestEffort);
  }
  void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedOperationException, UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
    actual.orderBefore(srcChildRelPath, destChildRelPath);
  }
  void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    actual.removeMixin(mixinName);
  }
  void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, LockException, LockException, UnsupportedRepositoryOperationException, InvalidItemStateException, RepositoryException {
    actual.restore(versionName, removeExisting);
  }
  void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
    actual.restore(version, removeExisting);
  }
  void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
    actual.restore(version, relPath, removeExisting);
  }
  void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
    actual.restoreByLabel(versionLabel, removeExisting);
  }
  Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, values);
  }
  Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, values, type);
  }
  Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value, type);
  }
  Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value);
  }
  Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, values);
  }
  Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, values, type);
  }
  Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
    return actual.setProperty(name, value, type);
  }
  void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
    actual.unlock();
  }
  void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
    actual.update(srcWorkspaceName);
  }
}
