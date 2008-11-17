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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class UpdaterEngine {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final static Logger log = LoggerFactory.getLogger(UpdaterEngine.class);    

    private Session session;
    private UpdaterSession updaterSession;
    private Vector<ModuleRegistration> modules;

    private static class Converted extends UpdaterItemVisitor.Default {
        @Override
        public void visit(Node node) throws RepositoryException {
            if (((UpdaterNode)node).hollow) {
                return;
            }
            super.visit(node);
        }
    }

    class ModuleRegistration implements UpdaterContext {
        String name;
        UpdaterModule module;
        Set<String> beforeModule;
        Set<String> afterModule;
        Set<String> startTag;
        String endTag;
        List<ItemVisitor> visitors;

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
            visitors.add(visitor);
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
    }

    public UpdaterEngine(Session session) throws RepositoryException {
        this(session, Modules.getModules());
    }

    public UpdaterEngine(Session session, Modules allModules) throws RepositoryException {
        this.session = session;
        this.modules = new Vector<ModuleRegistration>();
        for (UpdaterModule module : new Modules<UpdaterModule>(allModules, UpdaterModule.class)) {
            module.register(new ModuleRegistration(module));
        }
        updaterSession = new UpdaterSession(session);
    }

    public boolean prepare() throws RepositoryException {
        // Obtain which version we are currently at
        Node initializeNode = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH);
        Set<String> currentVersions = new HashSet<String>();
        if(initializeNode.hasProperty(HippoNodeType.HIPPO_VERSION)) {
            Value[] values = initializeNode.getProperty(HippoNodeType.HIPPO_VERSION).getValues();
            for (int i = 0; i < values.length; i++) {
                currentVersions.add(values[i].getString());
            }
        }

        // Select applicable modules for the current version.
        for (Iterator<ModuleRegistration> iter = modules.iterator(); iter.hasNext();) {
            ModuleRegistration registration = iter.next();
            boolean isValid = false;
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
                        for (ListIterator<ModuleRegistration> findBefore = modules.listIterator(); findBefore.nextIndex() <= iter.previousIndex();) {
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

    public void wrapup() throws RepositoryException {
        Node initializeNode = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH);
        Set<String> currentVersions = new HashSet<String>();
        Value[] values = initializeNode.getProperty(HippoNodeType.HIPPO_VERSION).getValues();
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
        String[] newVersions = currentVersions.toArray(new String[currentVersions.size()]);
        initializeNode.setProperty(HippoNodeType.HIPPO_ACTIVE, newVersions);
    }

    public static void migrate(Session session) {
        try {
            if(!session.getRootNode().isNodeType("mix:referenceable")) {
                session.getRootNode().addMixin("mix:referenceable");
                session.save();
            }
            if(!session.getRootNode().hasNode(HippoNodeType.CONFIGURATION_PATH) || !session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH).hasNode(HippoNodeType.INITIALIZE_PATH)) {
                log.info("no migration cycle because clean repository startup");
                return;
            }
            boolean updates;
            do {
                UpdaterEngine engine = new UpdaterEngine(session);
                updates = engine.prepare();
                if (updates) {
                    log.info("migration update cycle starting");
                    try {
                        engine.update();
                        engine.commit();
                    } catch(UpdaterException ex) {
                        log.error("error in migration cycle, skipping but might lead to serious errors");
                    } finally {
                        engine.wrapup();
                    }
                }
            } while (updates);
            log.info("migration cycle finished successfully");
        } catch(RepositoryException ex) {
            log.error("error in migration cycle: "+ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    public void update() throws RepositoryException {
        UpdaterException exception = null;
        for (ModuleRegistration module : modules) {
            for (ItemVisitor visitor : module.visitors) {
                try {
                    updaterSession.getRootNode().accept(visitor);
                } catch(UpdaterException ex) {
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

    public void update(ItemVisitor visitor) throws RepositoryException {
        updaterSession.getRootNode().accept(visitor);
    }

    public void commit() throws RepositoryException {
        updaterSession.getRootNode().accept(new Cleaner(new ModuleRegistration(null)));
        updaterSession.commit();
    }

    private class Cleaner extends Converted {
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
        }
    }
}
