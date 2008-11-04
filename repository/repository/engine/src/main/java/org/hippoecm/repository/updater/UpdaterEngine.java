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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterModule;

public class UpdaterEngine {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    Session session;
    UpdaterSession updaterSession;
    List<ModuleRegistration> modules;

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
            return ((UpdaterSession) session).getNewType(type);
        }
        public void setName(Item item, String name) throws RepositoryException {
            ((UpdaterItem) item).setName(name);
        }
        public void setPrimaryNodeType(Node node, String name) throws RepositoryException {
            ((UpdaterNode) node).setPrimaryNodeType(name);
        }
        public NodeType[] getNodeTypes(Node node) throws RepositoryException {
            return ((UpdaterNode) node).getNodeTypes();
        }
        public boolean isMultiple(Property property) {
            return ((UpdaterProperty) property).isMultiple();
        }
    }

    public UpdaterEngine(Session session) throws RepositoryException {
        this(session, Modules.getModules());
    }
    
    public UpdaterEngine(Session session, Modules allModules) throws RepositoryException {
        this.session = session;
        this.modules = new LinkedList<ModuleRegistration>();
        for(UpdaterModule module : new Modules<UpdaterModule>(allModules, UpdaterModule.class)) {
            module.register(new ModuleRegistration(module));
        }
        updaterSession = new UpdaterSession(session);
    }

    public void update() throws RepositoryException {
        for(ModuleRegistration module : modules) {
            for(ItemVisitor visitor : module.visitors) {
                updaterSession.getRootNode().accept(visitor);
            }
        }
    }

    public void update(ItemVisitor visitor) throws RepositoryException {
        updaterSession.getRootNode().accept(visitor);
    }

    public void commit() throws RepositoryException {
        updaterSession.getRootNode().accept(new Cleaner(new ModuleRegistration(null)));
        updaterSession.commit();
    }
       
    private class Cleaner extends UpdaterItemVisitor.Converted {
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
                UpdaterProperty property = (UpdaterProperty) iter.nextProperty();
                if (property.origin == null || !((Property) property.origin).getDefinition().isProtected()) {
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

/*
class NamespaceUpdaterModule implements UpdaterModule {
    public void register(UpdaterContext context) {
        String moduleName = NamespaceUpdaterModule.class.getName();
        context.registerName("hippo:namespace");
        context.registerBefore("hippo:initialize");
        context.registerAfter("hippo:finalize");
        context.registerStartTag("milestone-7");
        context.registerEndTag("milestone-8-"+moduleName);
    }
}

class Milestone {
    class Eight {
        class Begin {
            context.register
        }
    }
}
*/
