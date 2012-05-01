/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.hippoecm.repository.updater.UpdaterEngine;
import org.hippoecm.repository.updater.UpdaterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryWorkflowImpl implements RepositoryWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RepositoryWorkflowImpl.class);

    private Session session;
    private Node subject;

    public RepositoryWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.session = rootSession;
        if(subject.getDepth() == 0)
            this.subject = rootSession.getRootNode();
        else
            this.subject = rootSession.getRootNode().getNode(subject.getPath().substring(1));
    }

    public Map<String,Serializable> hints() {
        return null;
    }

    public void createNamespace(String prefix, String uri) throws WorkflowException, MappingException,
            RepositoryException {
        try {
            NamespaceRegistry nsreg = session.getWorkspace().getNamespaceRegistry();
            nsreg.registerNamespace(prefix, uri);
        } catch (NamespaceException ex) {
            log.error(ex.getMessage() + " For: " + prefix + ":" + uri);
            throw new WorkflowException("Cannot create new namespace", ex);
        }
    }
    
    private void updateModel(final String prefix, final String cnd, final UpdaterModule module) throws WorkflowException, MappingException,
                                                                                                       RepositoryException, RemoteException {
        UpdaterModule updateModelUpdaterModule = new UpdaterModule() {
            public void register(final UpdaterContext context) {
                context.registerName(module != null ? module.getClass().getName() : getClass().getName());
                context.registerStartTag(null);
                context.registerEndTag(null);
                if (module != null) {
                    module.register(new UpdaterContext() {
                        public void registerName(String name) {
                        }

                        public void registerBefore(String name) {
                        }

                        public void registerAfter(String name) {
                        }

                        public void registerStartTag(String name) {
                        }

                        public void registerExpectTag(String name) {
                        }

                        public void registerEndTag(String name) {
                        }

                        public void registerVisitor(ItemVisitor visitor) {
                            context.registerVisitor(visitor);
                        }

                        public NodeType getNewType(Session session, String type) throws RepositoryException {
                            return context.getNewType(session, type);
                        }

                        public void setName(Item item, String name) throws RepositoryException {
                            context.setName(item, name);
                        }

                        public void setPrimaryNodeType(Node node, String name) throws RepositoryException {
                            context.setPrimaryNodeType(node, name);
                        }

                        public NodeType[] getNodeTypes(Node node) throws RepositoryException {
                            return context.getNodeTypes(node);
                        }

                        public boolean isMultiple(Property property) throws RepositoryException {
                            return context.isMultiple(property);
                        }

                        public Workspace getWorkspace() throws RepositoryException {
                            return context.getWorkspace();
                        }
                    });
                }
                context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, prefix, "-", new StringReader(cnd)));
                //context.registerVisitor(new UpdaterEngine.Cleaner(context));
            }
        };
        Modules<UpdaterModule> modules = new Modules(Collections.singletonList(updateModelUpdaterModule));
        UpdaterEngine.migrate(session, modules);
    }

    private void updateModel(final String prefix, final String cnd, final UpdaterModule module, Map<String, List<Change>> changes) throws RepositoryException, WorkflowException, RemoteException {
        final Map<String, List<ChangeImpl>> changesImpl;
        if (changes != null) {
            changesImpl = ChangeImpl.convert(changes, session);
        } else {
            changesImpl = null;
        }
        updateModel(prefix, cnd, new UpdaterModule() {
            public void register(final UpdaterContext context) {
                for (String nodeType : changesImpl.keySet()) {
                    context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor(nodeType) {
                        @Override
                        protected void leaving(Node n, int level) throws RepositoryException {
                            UpdaterNode node = (UpdaterNode)n;
                            ChangeImpl.change(node, changesImpl);
                        }
                    });
                }
                if (module != null) {
                    module.register(context);
                }
            }
        });
    }

    public void updateModel(String prefix, String cnd) throws WorkflowException, MappingException,
            RepositoryException, RemoteException
    {
        updateModel(prefix, cnd, (UpdaterModule)null);
    }

    public void updateModel(final String prefix, final String cnd, final String contentUpdater, Object contentUpdaterCargo) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        try {
            UpdaterModule module =  getUpdaterModule(contentUpdater, contentUpdaterCargo);
            updateModel(prefix, cnd, module);
        } catch (ClassNotFoundException ex) {
            throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch (NoSuchMethodException ex) {
            throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch (InstantiationException ex) {
            throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch (IllegalAccessException ex) {
            throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    public void updateModel(final String prefix, final String cnd, final String contentUpdater, Map<String, List<Change>> changes) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if (contentUpdater != null) {
            try {
                UpdaterModule module =  getUpdaterModule(contentUpdater, changes);
                updateModel(prefix, cnd, module, changes);
            } catch (ClassNotFoundException ex) {
                throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
            } catch (NoSuchMethodException ex) {
                throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
            } catch (InstantiationException ex) {
                throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
            } catch (IllegalAccessException ex) {
                throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
            } catch (InvocationTargetException ex) {
                throw new WorkflowException(ex.getClass().getName()+": "+ex.getMessage(), ex);
            }
        } else {
            updateModel(prefix, cnd, (UpdaterModule)null, changes);
        }
    }

    private UpdaterModule getUpdaterModule(String className, Object cargo) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class cls = Class.forName(className);
        if (UpdaterModule.class.isAssignableFrom(cls)) {
            Constructor<UpdaterModule> constructor = null;
            for(Constructor<UpdaterModule> candidate : cls.getConstructors()) {
                Class[] formalParams = candidate.getParameterTypes();
                if(formalParams.length == 1 && formalParams[0].isInstance(cargo)) {
                    if(constructor == null || !constructor.getParameterTypes()[0].isAssignableFrom(formalParams[0])) {
                        constructor = candidate;
                    }
                }
            }
            if(constructor == null) {
                throw new NoSuchMethodException("no suitable constructor found");
            }
            return constructor.newInstance(new Object[] {cargo});
        } else {
            throw new ClassNotFoundException("class "+className+" is not an "+UpdaterModule.class.getName());
        }
    }

    public void consistency(String argument) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if (argument.equals("versionHistoryReport") || argument.equals("versionHistoryCleanup")) {
            VersionHistoryCleanup cleanup = new VersionHistoryCleanup();
            cleanup.traverse(session);
            cleanup.process();
            cleanup.report(session);
            if (argument.equals("versionHistoryCleanup")) {
                cleanup.repair(session);
            }
        }
    }
}
