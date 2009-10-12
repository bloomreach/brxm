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
package org.hippoecm.repository.updater;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemVisitor;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.impl.SessionDecorator;

public class UpdaterEngine {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final static Logger log = LoggerFactory.getLogger(UpdaterEngine.class);

    Session session;
    UpdaterSession updaterSession;
    private Vector<ModuleRegistration> modules;

    class ModuleRegistration implements UpdaterContext {
        String name;
        UpdaterModule module;
        Set<String> beforeModule;
        Set<String> afterModule;
        Set<String> startTag;
        String endTag;
        List<ItemVisitor> visitors;
        ContextWorkspace workspace;

        ModuleRegistration(UpdaterModule module) {
            this.module = module;
            this.beforeModule = new TreeSet<String>();
            this.afterModule = new TreeSet<String>();
            this.startTag = new TreeSet<String>();
            this.endTag = null;
            this.visitors = new LinkedList<ItemVisitor>();
        }

        public void registerName(String name) {
            this.name = name;
        }

        public void registerBefore(String name) {
            beforeModule.add(name);
        }

        public void registerAfter(String name) {
            afterModule.add(name);
        }

        public void registerStartTag(String name) {
            startTag.add(name);
        }

        public void registerEndTag(String name) {
            endTag = name;
        }

        public void registerVisitor(ItemVisitor visitor) {
            try {
                if (visitor instanceof UpdaterItemVisitor.NamespaceVisitor) {
                    visitor = new NamespaceVisitorImpl(session.getWorkspace().getNamespaceRegistry(), (UpdaterItemVisitor.NamespaceVisitor)visitor);
                }
                visitors.add(visitor);
            } catch (RepositoryException ex) {
                ex.printStackTrace(System.err);
            } catch (ParseException ex) {
                ex.printStackTrace(System.err);
            }
        }

        public NodeType getNewType(Session session, String type) throws RepositoryException {
            return ((UpdaterSession)session).getNewType(type);
        }

        public void setName(Item item, String name) throws RepositoryException {
            ((UpdaterItem)item).setName(name);
        }

        public void setPrimaryNodeType(Node node, String name) throws RepositoryException {
            ((UpdaterNode)node).setPrimaryNodeType(name);
        }

        public NodeType[] getNodeTypes(Node node) throws RepositoryException {
            return ((UpdaterNode)node).getNodeTypes();
        }

        public boolean isMultiple(Property property) {
            return ((UpdaterProperty)property).isMultiple();
        }

        public Workspace getWorkspace() throws RepositoryException {
            if(workspace == null) {
                workspace = new ContextWorkspace(this);
            }
            return workspace;
        }
    }

    class ContextWorkspace implements Workspace, NamespaceRegistry {
        Workspace upstreamWorkspace;
        NamespaceRegistry upstreamRegistry;
        ModuleRegistration module;
        Map<String, String> privateNamespaceRegister = new HashMap<String,String>();
        ContextWorkspace(ModuleRegistration module) throws RepositoryException {
            this.module = module;
            upstreamWorkspace = session.getWorkspace();
            upstreamRegistry = upstreamWorkspace.getNamespaceRegistry();
        }
        public Session getSession() {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public String getName() {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
            throw new UnsupportedOperationException("NUnsupported operation");
        }
        public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public QueryManager getQueryManager() throws RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
            return this;
        }
        public NodeTypeManager getNodeTypeManager() throws RepositoryException {
            return upstreamWorkspace.getNodeTypeManager();
        }
        public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public String[] getAccessibleWorkspaceNames() throws RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException, RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {
            throw new UnsupportedOperationException("Unsupported operation");
        }
        public void registerNamespace(String prefix, String uri) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
            privateNamespaceRegister.put(prefix, uri);
        }
        public void unregisterNamespace(String prefix) throws NamespaceException, UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException {
            throw new UnsupportedRepositoryOperationException();
        }
        public String[] getPrefixes() throws RepositoryException {
            return null;
        }
        public String[] getURIs() throws RepositoryException {
            return null;
        }
        public String getURI(String prefix) throws NamespaceException, RepositoryException {
            if (privateNamespaceRegister.containsKey(prefix)) {
                return privateNamespaceRegister.get(prefix);
            }
            for (ItemVisitor visitor : module.visitors) {
                if (visitor instanceof NamespaceVisitorImpl) {
                    NamespaceVisitorImpl namespaceVisitor = (NamespaceVisitorImpl)visitor;
                    if (namespaceVisitor.namespace.equals(prefix)) {
                        return namespaceVisitor.newURI;
                    }
                }
            }
            return upstreamRegistry.getURI(prefix);
        }
        public String getPrefix(String uri) throws NamespaceException, RepositoryException {
            return null;
        }
    }

    private Property getOrCreateVersionProperty(boolean createIfNotExists) throws RepositoryException {
        Node rootNode = session.getRootNode();
        Node configurationNode = null;
        Node initializeNode = null;
        Property versionProperty = null;
        if(!rootNode.hasNode("hippo:configuration")) {
            for(NodeIterator iter = rootNode.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();
                if(child.getName().startsWith("hippo_") && child.getName().endsWith(":configuration")) {
                    configurationNode = child;
                }
            }
        } else {
            configurationNode = rootNode.getNode("hippo:configuration");
        }
        if(configurationNode == null) {
            return null; // cannot create also
        }
        if(!configurationNode.hasNode("hippo:initialize")) {
            for(NodeIterator iter = configurationNode.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();
                if(child.getName().startsWith("hippo_") && child.getName().endsWith(":initialize")) {
                    initializeNode = child;
                }
            }
        } else {
            initializeNode = configurationNode.getNode("hippo:initialize");
        }
        if(initializeNode == null) {
            return null; // cannot create also
        }

        if(!initializeNode.hasNode("hippo:version")) {
            for(PropertyIterator iter = initializeNode.getProperties(); iter.hasNext(); ) {
                Property child = iter.nextProperty();
                if((child.getName().startsWith("hippo_") && child.getName().endsWith(":version")) ||
                   child.getName().equals("hippo:version")) {
                    versionProperty = child;
                }
            }
        } else {
            versionProperty = initializeNode.getProperty("hippo:version");
        }
        if(versionProperty == null && createIfNotExists) {
            String prefix = initializeNode.getName().substring(0, initializeNode.getName().indexOf(":")+1);
            versionProperty = initializeNode.setProperty(prefix+"version", new Value[0]);
        }
        return versionProperty;
    }

    public UpdaterEngine(Session session) throws RepositoryException {
        this(session, Modules.getModules());
    }

    public UpdaterEngine(Session session, Modules allModules) throws RepositoryException {
        this.session = session;
        this.modules = new Vector<ModuleRegistration>();
        for (UpdaterModule module : new Modules<UpdaterModule>(allModules, UpdaterModule.class)) {
            ModuleRegistration registration = new ModuleRegistration(module);
            module.register(registration);
            modules.add(registration);
        }
        updaterSession = new UpdaterSession(session);
    }

    private boolean prepare() throws RepositoryException {
        // Obtain which version we are currently at
        Set<String> currentVersions = new HashSet<String>();

        Property versionProperty = getOrCreateVersionProperty(false);
        if(versionProperty != null) {
            Value[] values = versionProperty.getValues();
            for (int i = 0; i < values.length; i++) {
                currentVersions.add(values[i].getString());
            }
        }
        StringBuffer logInfo = new StringBuffer();
        for(String tag : currentVersions) {
            logInfo.append(" ");
            logInfo.append(tag);
        }
        log.info("Migration cycle starting with version tags:"+new String(logInfo));

        // Select applicable modules for the current version.
        for (Iterator<ModuleRegistration> iter = modules.iterator(); iter.hasNext();) {
            ModuleRegistration registration = iter.next();
            boolean isValid = false;
            if(registration.startTag.size() == 0) {
                if (!"m8-bootstrap".equals(registration.name)) {
                    log.warn("module "+registration.name+" did not specify start tag");
                }
                if(currentVersions.size() == 0)
                    isValid = true;
            }
            for (String requestedVersion : registration.startTag) {
                if (currentVersions.contains(requestedVersion)) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                iter.remove();
            }
        }
        if (modules.size() == 0) {
            return false;
        }

        // Sort remaining modules
        int maxBubbles = 0;
        for (ModuleRegistration registration : modules) {
            maxBubbles += registration.beforeModule.size() + registration.afterModule.size();
        }
        for (int bubbleCount = 0; bubbleCount < maxBubbles; ++bubbleCount) {
            boolean modified = false;
            for (ListIterator<ModuleRegistration> iter = modules.listIterator(); iter.hasNext(); ) {
                ModuleRegistration module = iter.next();
                if (!modified) {
                    for (String before : module.beforeModule) {
                        int index = -1;
                        for (ListIterator<ModuleRegistration> findBefore = modules.listIterator();
                             findBefore.nextIndex() <= iter.previousIndex(); ) {
                            if (findBefore.next().name.equals(before)) {
                                index = findBefore.previousIndex();
                                break;
                            }
                        }
                        if (index != -1) {
                            modified = true;
                            iter.remove();
                            modules.insertElementAt(module, index);
                            break;
                        }
                    }
                }
                if (!modified) {
                    for (String after : module.afterModule) {
                        int index = -1;
                        for (ListIterator<ModuleRegistration> findAfter = modules.listIterator(); findAfter.hasNext(); ) {
                            if (findAfter.next().name.equals(after)) {
                                index = findAfter.nextIndex();
                                break;
                            }
                        }
                        if (index != -1) {
                            modified = true;
                            iter.remove();
                            modules.insertElementAt(module, index);
                            break;
                        }
                    }
                }
                if (modified) {
                    break;
                }
            }
            if (!modified) {
                break;
            }
        }
        return true;
    }

    private void close() throws RepositoryException {
        UpdaterException exception = null;
        NamespaceRegistryImpl nsreg = (NamespaceRegistryImpl) session.getWorkspace().getNamespaceRegistry();
        for (ModuleRegistration module : modules) {
            log.info("migration close cycle for module "+module.name);
            for (ItemVisitor visitor : module.visitors) {
                try {
                    if(visitor instanceof NamespaceVisitorImpl) {
                        NamespaceVisitorImpl remap = ((NamespaceVisitorImpl)visitor);
                        if(remap.oldPrefix != null) {
                            nsreg.externalRemap(remap.namespace, remap.oldPrefix, remap.oldURI);
                            nsreg.externalRemap(remap.newPrefix, remap.namespace, remap.newURI);
                        } else {
                            nsreg.externalRemap(remap.newPrefix, remap.namespace, remap.newURI);
                        }
                    }
                } catch(UpdaterException ex) {
                    System.err.println(ex.getClass().getName()+": "+ex.getMessage());
                    ex.printStackTrace(System.err);
                    if(exception != null) {
                        exception = ex;
                    }
                    log.error("error in migration cycle, continuing, but this might lead to subsequent errors", ex);
                }
            }
        }
        if(exception != null) {
            throw exception;
        }
    }

    private void wrapup() throws RepositoryException {
        Property versionProperty = getOrCreateVersionProperty(true);

        Set<String> currentVersions = new HashSet<String>();
        Value[] values = versionProperty.getValues();
        for (int i = 0; i < values.length; i++) {
            currentVersions.add(values[i].getString());
        }
        for (ModuleRegistration registration : modules) {
            for(String tag : registration.startTag) {
                currentVersions.remove(tag);
            }
        }
        for (ModuleRegistration registration : modules) {
            if(registration.endTag != null) {
                currentVersions.add(registration.endTag);
            }
        }
        StringBuffer logInfo = new StringBuffer();
        for(String tag : currentVersions) {
            logInfo.append(" ");
            logInfo.append(tag);
        }
        log.info("Migration cycle ending with version tags:"+new String(logInfo));
        String[] newVersions = currentVersions.toArray(new String[currentVersions.size()]);
        versionProperty.setValue(newVersions);
        versionProperty.getParent().save();
    }

    public static boolean migrate(Session session) {
        boolean needsRestart = false;
        try {
            if(!session.getRootNode().hasNode(HippoNodeType.CONFIGURATION_PATH) ||
               !session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH).hasNode(HippoNodeType.INITIALIZE_PATH)) {
                log.info("no migration cycle because clean repository startup without hippo configuration");
                return false;
            }
            boolean updates;
            do {
                Session subSession = session.impersonate(new SimpleCredentials("system", new char[] {}));
                UpdaterEngine engine = new UpdaterEngine(subSession);
                updates = engine.prepare();
                if (updates) {
                    needsRestart = true;
                    log.info("migration update cycle starting");
                    try {
                        engine.update();
                        log.info("migration cycle commit");
                        engine.commit();
                        log.info("migration cycle save");
                        subSession.save();
                        log.info("migration cycle saved");
                        engine.close();
                        subSession.save();
                        log.info("migration cycle closed");
                        engine.wrapup();
                        log.info("migration cycle wrapup");
                    } catch(UpdaterException ex) {
                        subSession.refresh(false);
                        log.error("error in migration cycle, skipping but might lead to serious errors");
                    } finally {
                        // log.info("migration cycle wrapup");
                        // engine.wrapup();
                        // subSession.logout();
                    }
                }
                subSession.logout();
            } while (updates);
            log.info("migration cycle finished successfully");
        } catch(ReferentialIntegrityException ex) {
            String uuid = ex.getMessage().substring(0, ex.getMessage().indexOf(":"));
            try {
                Node node = session.getNodeByUUID(uuid);
                log.error("error in migration cycle: node still referenced: "+uuid+" "+node.getPath());
                for(PropertyIterator iter = node.getReferences(); iter.hasNext(); ) {
                    Property reference = iter.nextProperty();
                    log.error("  referring property "+reference.getPath());
                }
            } catch(RepositoryException ignored) {
                log.error("error in migration cycle: node still referenced: "+uuid);
            }
        } catch(RepositoryException ex) {
            log.error("error in migration cycle: "+ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
        return needsRestart;
    }

    public static void migrate(Session session, Modules<UpdaterModule> modules) throws UpdaterException, RepositoryException {
        session = org.hippoecm.repository.decorating.checked.SessionDecorator.unwrap(session);
        ((SessionDecorator)session).postMountEnabled(false);
        session.refresh(false);
        try {
            UpdaterEngine engine = new UpdaterEngine(session, modules);
            engine.update();
            engine.commit();
            session.save();
            engine.close();
            session.save();
        } finally {
            ((SessionDecorator)session).postMountEnabled(true);
        }
    }
            
    private void update() throws RepositoryException {
        UpdaterException exception = null;
        for (ModuleRegistration module : modules) {
            log.info("migration update cycle for module "+module.name);
            for (ItemVisitor visitor : module.visitors) {
                if (visitor instanceof NamespaceVisitorImpl) {
                    NamespaceVisitorImpl remap = (NamespaceVisitorImpl)visitor;
                    log.info("migration handling namespace " + remap.namespace);
                    Workspace workspace = session.getWorkspace();
                    NamespaceRegistry nsreg = workspace.getNamespaceRegistry();
                    String[] prefixes = nsreg.getPrefixes();
                    boolean prefixExists = false;
                    for (String prefix : prefixes) {
                        if (prefix.equals(remap.newPrefix)) {
                            if (!nsreg.getURI(prefix).equals(remap.newURI)) {
                                throw new NamespaceException("Prefix " + remap.newPrefix + " is already mapped to " + nsreg.getURI(prefix));
                            }
                            prefixExists = true;
                        }
                    }
                    if (!prefixExists) {
                        log.info("migration registering new prefix " + remap.newPrefix + " to " + remap.newURI);
                        nsreg.registerNamespace(remap.newPrefix, remap.newURI);
                    }
                    List ntdList = remap.cndReader.getNodeTypeDefs();
                    NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl)workspace.getNodeTypeManager();
                    NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
                    for (Iterator iter = ntdList.iterator(); iter.hasNext();) {
                        try {
                            NodeTypeDef ntd = (NodeTypeDef)iter.next();
                            log.info("migration registering new nodetype " + ntd.getName());
                            /* EffectiveNodeType effnt = */ ntreg.registerNodeType(ntd);
                        } catch (InvalidNodeTypeDefException ex) {
                            // deliberate ignore
                            }
                    }
                }
            }
        }
        for (ModuleRegistration module : modules) {
            log.info("migration update cycle for module "+module.name);
            for (ItemVisitor visitor : module.visitors) {
                try {
                    if(visitor instanceof UpdaterItemVisitor.Iterated) {
                        UpdaterItemVisitor.Iterated iteratedVisitor = (UpdaterItemVisitor.Iterated) visitor;
                        for(NodeIterator iter = iteratedVisitor.iterator(updaterSession.upstream); iter.hasNext(); ) {
                            Node node = iter.nextNode();
                            String path = node.getPath();
                            node = updaterSession.getRootNode();
                            if(!path.equals("/")) {
                                node = node.getNode(path.substring(1));
                            }
                            try {
                                iteratedVisitor.visit(node);
                            } catch(InvalidItemStateException ex) {
                                // deliberate ignore
                            }
                        }
                    } else {
                        updaterSession.getRootNode().accept(visitor);
                    }
                } catch (UpdaterException ex) {
                    if (exception != null) {
                        exception = ex;
                    }
                    log.error("error in migration cycle, continuing, but this might lead to subsequent errors", ex);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private void commit() throws RepositoryException {
        updaterSession.commit();
    }

    public static class Converted extends UpdaterItemVisitor {
        public Converted() {
        }

        @Override
        public final void visit(Property property) throws RepositoryException {
            super.visit(property);
        }

        @Override
        public final void visit(Node node) throws RepositoryException {
            if (((UpdaterNode)node).hollow) {
                return;
            }
            super.visit(node);
        }

        protected void entering(Node node, int level)
                throws RepositoryException {
        }

        protected void entering(Property property, int level)
                throws RepositoryException {
        }

        protected void leaving(Node node, int level)
                throws RepositoryException {
        }

        protected void leaving(Property property, int level)
                throws RepositoryException {
        }
    }

    public static class Cleaner extends Converted {
        UpdaterContext context;

        public Cleaner(UpdaterContext context) {
            this.context = context;
        }

        void update(UpdaterSession session) throws RepositoryException {
            session.getRootNode().accept(this);
        }

        @Override
        public void entering(Node node, int level) throws RepositoryException {
            NodeType[] nodeTypes = context.getNodeTypes(node);
            for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                UpdaterProperty property = (UpdaterProperty)iter.nextProperty();
                if (property.origin == null || !((Property)property.origin).getDefinition().isProtected()) {
                    boolean isValid = false;
                    for (int i = 0; i < nodeTypes.length; i++) {
                        PropertyDefinition[] defs = nodeTypes[i].getPropertyDefinitions();
                        for (int j = 0; j < defs.length; j++) {
                            if (defs[j].getName().equals("*")) {
                                isValid = true;
                            } else if (defs[j].getName().equals(property.getName())) {
                                isValid = true;
                                break;
                            }
                        }
                    }
                    if (!isValid) {
                        property.remove();
                    }
                }
            }
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                UpdaterNode child = (UpdaterNode)iter.nextNode();
                if (child.origin == null || !((Node)child.origin).getDefinition().isProtected()) {
                    boolean isValid = false;
                    for (int i = 0; i < nodeTypes.length; i++) {
                        NodeDefinition[] defs = nodeTypes[i].getChildNodeDefinitions();
                        for (int j = 0; j < defs.length; j++) {
                            if (defs[j].getName().equals("*")) {
                                isValid = true;
                            } else if (defs[j].getName().equals(child.getName())) {
                                isValid = true;
                                break;
                            }
                        }
                    }
                    if (!isValid) {
                        child.remove();
                    }
                }
            }
        }
    }

    public final static class NamespaceVisitorImpl extends UpdaterItemVisitor {
        public String namespace;
        public String oldURI;
        public String newURI;
        public String oldPrefix;
        public String newPrefix;
        public CompactNodeTypeDefReader cndReader;
        public String cndName;
        UpdaterContext context;

        public NamespaceVisitorImpl(NamespaceRegistry nsReg, NamespaceVisitor definition) throws RepositoryException, ParseException {
            this.namespace = definition.prefix;
            this.cndName = definition.cndName;
            this.context = definition.context;
            oldURI = null;
            oldPrefix = null;
            try {
                oldURI = nsReg.getURI(namespace);
                oldPrefix = namespace + "_" + oldURI.substring(oldURI.lastIndexOf('/') + 1).replace('.', '_');
            } catch (NamespaceException ex) {
                // deliberate ignore
                }
            this.cndReader = new CompactNodeTypeDefReader(definition.cndReader, cndName);
            NamespaceMapping mapping = cndReader.getNamespaceMapping();
            newURI = mapping.getURI(namespace);
            newPrefix = namespace + "_" + newURI.substring(newURI.lastIndexOf('/') + 1).replace('.', '_');
        }

        @Override
        public final void visit(Node node) throws RepositoryException {
            if(node.getPath().equals("/jcr:system"))
                return;
            super.visit(node); // FIXME
        }

        @Override
        protected final void entering(Node node, int level) throws RepositoryException {
            if(node.hasProperty("jcr:primaryType")) {
                String primaryType = node.getProperty("jcr:primaryType").getString();
                if(primaryType.startsWith(namespace + ":")) {
                    context.setPrimaryNodeType(node, newPrefix + ":" + primaryType.substring(namespace.length() + 1));
                }
            }
            if(node.hasProperty("jcr:mixinTypes")) {
                String[] mixins = new String[node.getProperty("jcr:mixinTypes").getValues().length];
                int i=0;
                boolean mixinsChanged = false;
                for(Value value : node.getProperty("jcr:mixinTypes").getValues()) {
                    mixins[i] = value.getString();
                    if(mixins[i].startsWith(namespace + ":")) {
                        mixins[i] = newPrefix + ":" + mixins[i].substring(namespace.length() + 1);
                        mixinsChanged = true;
                   }
                    ++i;
                }
                if (mixinsChanged) {
                    node.setProperty("jcr:mixinTypes", mixins);
                }
            }
        }

        @Override
        protected final void entering(Property property, int level)
                throws RepositoryException {
        }

        @Override
        protected final void leaving(Node node, int level)
                throws RepositoryException {
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                if (child.getName().startsWith(namespace + ":")) {
                    context.setName(child, newPrefix + ":" + child.getName().substring(child.getName().indexOf(":") + 1));
                }
            }
        }

        @Override
        protected final void leaving(Property property, int level)
                throws RepositoryException {
            if (property.getName().startsWith(namespace + ":")) {
                context.setName(property, newPrefix + ":" + property.getName().substring(property.getName().indexOf(":") + 1));
            }
        }
    }
}
