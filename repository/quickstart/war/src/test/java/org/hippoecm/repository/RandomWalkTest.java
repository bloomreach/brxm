/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.util.TraversingItemVisitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomWalkTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(RandomWalkTest.class);

    private String base;
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(true);
        Node baseFolder = session.getRootNode().getNode("content/documents");
        while(baseFolder.hasNode("test")) {
            baseFolder.getNode("test").remove();
            baseFolder.save();
        }
        WorkflowManager workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        Workflow wf = workflowManager.getWorkflow("threepane", baseFolder);
        assertTrue(wf instanceof FolderWorkflow);
        base = ((FolderWorkflow)wf).add("new-folder", "hippostd:folder", "test");
    }

    @After
    @Override
    public void tearDown() throws Exception {
        session.refresh(false);
        while(session.getRootNode().hasNode(base.substring(1))) {
            session.getRootNode().getNode(base.substring(1)).remove();
            session.save();
        }
        super.tearDown();
    }

    @Test
    public void walk() throws Exception {
        singlethreaded(150);
        sanity(session.getRootNode().getNode(base.substring(1)));
    }

    @Ignore
    public void stampede() throws Exception {
        multithreaded(3500, 24);
        sanity(session.getRootNode().getNode(base.substring(1)));
    }

    private void sanity(Node node) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoWorkspace)node.getSession().getWorkspace()).getWorkflowManager();
        node.accept(new TraversingItemVisitor.Default(true) {
            @Override
            protected void entering(Property property, int level) throws RepositoryException {
                if(property.getDefinition().isMultiple()) {
                    for(Value value : property.getValues()) {
                        value.getString();
                    }
                } else {
                    property.getString();
                }
            }
            @Override
            public void entering(Node node, int level) throws RepositoryException {
                node.getPath();
                node.getDepth();
                node.getName();
            }
            @Override
            public void visit(Node node) throws RepositoryException {
                try {
                    if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) {
                        super.visit(node);
                    } else if (node.isNodeType("hippo:document")) {
                        EditableWorkflow editWorkflow = (EditableWorkflow)workflowManager.getWorkflow("default", node);
                        Document document = editWorkflow.obtainEditableInstance();
                        node.getSession().refresh(false);
                        Node draft = node.getSession().getNodeByUUID(document.getIdentity());
                        draft.getSession().save();
                        node.getSession().refresh(false);
                        editWorkflow = (EditableWorkflow)workflowManager.getWorkflow("edit", draft);
                        editWorkflow.commitEditableInstance();
                    } else if (node.isNodeType("hippo:handle") && !node.hasNode(node.getName())) {
                        super.visit(node);
                    } else if (node.isNodeType("hippo:handle")) {
                        Node variant = null;
                        for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();) {
                            Node child = iter.nextNode();
                            if (child.getProperty("hippostd:state").getString().equals("draft"))
                                variant = child;
                            else if (child.getProperty("hippostd:state").getString().equals("published") && variant == null)
                                variant = child;
                            else if (child.getProperty("hippostd:state").getString().equals("unpublished") &&
                                    (variant == null || !variant.getProperty("hippostd:state").getString().equals("draft")))
                                variant = child;
                        }
                        if(variant == null) {
                            throw new RepositoryException("unsupported document type for this test");
                        }
                        if(!variant.isNodeType("hippostd:publishable")) {
                            throw new RepositoryException("unsupported document type for this test "+node.getPrimaryNodeType().getName());
                        }
                        EditableWorkflow editWorkflow = (EditableWorkflow)workflowManager.getWorkflow("default", variant);
                        Document document = editWorkflow.obtainEditableInstance();
                        node.getSession().refresh(false);
                        Node draft = node.getSession().getNodeByUUID(document.getIdentity());
                        draft.getSession().save();
                        editWorkflow = (EditableWorkflow)workflowManager.getWorkflow("default", draft);
                        editWorkflow.commitEditableInstance();
                        for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();) {
                            Node child = iter.nextNode();
                            if (child.getProperty("hippostd:state").getString().equals("unpublished"))
                                variant = child;
                        }
                        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow)workflowManager.getWorkflow("default", variant);
                        workflow.publish();
                        node.getSession().refresh(false);
                        for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();) {
                            Node child = iter.nextNode();
                            if (child.getProperty("hippostd:state").getString().equals("published"))
                                variant = child;
                        }
                        workflow = (FullReviewedActionsWorkflow)workflowManager.getWorkflow("default", variant);
                        workflow.depublish();
                        node.getSession().refresh(false);
                    }
                } catch(WorkflowException ex) {
                    new RepositoryException("failed workflow", ex);
                } catch(MappingException ex) {
                    new RepositoryException("failed workflow", ex);
                } catch(RemoteException ex) {
                    new RepositoryException("failed workflow", ex);
                }
            }
        });     
        Query query = session.getWorkspace().getQueryManager().createQuery(base+"//*", Query.XPATH);
        QueryResult result = query.execute();
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (child != null) {
                child.getPath();
            }
        }
    }

    private void singlethreaded(final int nsteps) throws Exception {
        RandomWalkRunner runner = new RandomWalkRunner("", session.getRootNode().getNode(base.substring(1)), nsteps,
                                                       new Random(2718));
        long t1 = System.currentTimeMillis();
        runner.run();
        long t2 = System.currentTimeMillis();
        if(RandomWalkTest.log.isDebugEnabled()) {
            log.debug("SINGLE THREADED TIMING "+(t2-t1)/1000.0);
        }
    }

    private void multithreaded(final int nsteps, final int nthreads) throws Exception {
        Exception exception = null;
        Thread[] threads = new Thread[nthreads];
        RandomWalkRunner[] runners = new RandomWalkRunner[nthreads];
        for(int i=0; i<nthreads; i++) {
            Session runnerSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            runners[i] = new RandomWalkRunner("thr"+i+" ", runnerSession.getRootNode().getNode(base.substring(1)), nsteps,
                                              new Random(314+i));
            threads[i] = new Thread(runners[i]);
        }
        long t1 = System.currentTimeMillis();
        for(int i=0; i<nthreads; i++) {
            threads[i].start();
        }
        for(int i=0; i<nthreads; i++) {
            threads[i].join();
        }
        long t2 = System.currentTimeMillis();
        for(int i=0; i<nthreads; i++) {
            runners[i].session.logout();
        }
        int niters = 0;
        int nfail = 0;
        int nmiss = 0;
        int nsuccess = 0;
        for(int i=0; i<nthreads; i++) {
            if(runners[i].exception != null) {
                if(exception == null) {
                    exception = runners[i].exception;
                }
                log.error("thread "+i+" first exception was: "+
                          runners[i].exception.getClass().getName()+": "+runners[i].exception.getMessage());
            }
            niters += runners[i].nsteps;
            nfail += runners[i].nfail;
            nsuccess += runners[i].nsuccess;
            nmiss += runners[i].nmiss;
            if(runners[i].nsteps != nsteps) {
                log.error("IRRECOVERABLE ERROR from thread "+i+" nsteps="+runners[i].nsteps);
            }
            if(runners[i].nfail > 0) {
                log.error("FAILURE from thread "+i+"nfail="+runners[i].nfail);
            }
            if(runners[i].nfail+runners[i].nsuccess+runners[i].nmiss != runners[i].nsteps) {
                fail("thread "+i+" delivered inconsistent counts: " + runners[i].nsuccess + "/" + runners[i].nmiss +
                     "/" + runners[i].nfail + "/" + runners[i].nsteps);
            }
        }
        if(RandomWalkTest.log.isDebugEnabled()) {
            log.debug("MULTI THREADED TIMING "+(t2-t1)/1000.0);
            log.debug("MULTI THREADED SUCCESSRATE "+((nsuccess+nmiss)/(double)niters) +
                      " ("+nsuccess+"/"+nmiss+"/"+nfail+"/"+niters+")");
        }
        if(exception != null) {
            fail(exception.getClass().getName()+": "+exception.getMessage()); // throw exception;
        }
    }
}

class RandomWalkRunner implements Runnable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected Session session;

    protected WorkflowManager workflowManager;

    protected String base;

    protected Random random = new Random();

    protected Set<String> ignoreMethods = new HashSet<String>();

    Exception exception = null;
    
    transient int nsteps, nfail, nsuccess, nmiss;

    private String id;

    RandomWalkRunner(String id, Node baseFolder, int nsteps, Random randomGenerator) throws RepositoryException {
        this.id = id;
        this.session = baseFolder.getSession();
        this.workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        this.base = baseFolder.getPath();
        this.random = randomGenerator;
        this.nsteps = nsteps;
        for(String methodName : new String[] { "reorder", "rename", "delete", "archive", "copy", "move", "duplicate",
                                               "restore", "requestDeletion", "requestDepublication", "requestPublication",
                                               "copyFrom", "copyTo", "copyOver", "moveFrom", "moveTo", "moveOver", "hints" }) {
            ignoreMethods.add(methodName);
        }
    }

    class Action<T extends Workflow> {
        String name;
        double weight = 1.0;
        WorkflowDescriptor workflowDescriptor;
        Exception exception;

        Action(String name) {
            this.name = name;
            workflowDescriptor = null;
        }

        Action(String name, WorkflowDescriptor workflowDescriptor) {
            this.name = name;
            this.workflowDescriptor = workflowDescriptor;
        }

        Node run(Node node) throws RepositoryException, WorkflowException, MappingException, RemoteException {
            if (workflowDescriptor != null) {
                if(RandomWalkTest.log.isDebugEnabled()) {
                    System.err.println(id+name+" "+node.getPath()); // System.err on purpose
                }
                T workflow = (T)workflowManager.getWorkflow(workflowDescriptor);
                session.save();
                Node next = execute(workflow);
                node.getSession().refresh(false);
                return (next != null ? next : node.getParent().isNodeType("hippo:handle") ? node.getParent().getParent() : node.getParent());
            } else {
                if(RandomWalkTest.log.isDebugEnabled()) {
                    System.err.println(id+name+" "+node.getPath()); // System.err on purpose
                }
                return execute(node);
            }
        }

        protected Node execute(Node node) throws RepositoryException, WorkflowException, MappingException, RemoteException {
            return node;
        }

        protected Node execute(T workflow) throws RepositoryException, WorkflowException, MappingException, RemoteException {
            return null;
        }
    }

    final Action PARENT = new Action("parent") {
        @Override
        protected Node execute(Node node) throws RepositoryException, WorkflowException, MappingException, RemoteException {
            node = node.getParent();
            node.getSession().refresh(false); // this mimicks somewhat cms updating behaviour
            return node;
        }
    };
    final Action BASE = new Action("base") {
        @Override
        protected Node execute(Node node) throws RepositoryException, WorkflowException, MappingException, RemoteException {
            node = node.getSession().getRootNode().getNode(base.substring(1));
            node.getSession().refresh(false); // this mimicks somewhat cms updating behaviour}
            return node;
        }
    };
    Action SELECT = new Action("select") {
        @Override
        protected Node execute(Node node) throws RepositoryException, WorkflowException, MappingException, RemoteException {
            NodeIterator iter = node.getNodes();
            Node child = null;
            try {
                int index = random.nextInt((int)iter.getSize());
                if (index > 0) {
                    iter.skip(index);
                }
                child = iter.nextNode();
            } catch(NoSuchElementException ex) {
                throw new WorkflowException("failed to select child");
            }
            if(child.isNodeType("hippo:handle")) {
                child = child.getNode(child.getName());
            }
            return child;
        }
    };

    public void run() {
        int count = 0;
        nsuccess = 0;
        nfail = 0;
        nmiss = 0;
        try {
            Node node = null;
            boolean reinitialize = true;
            while(count<nsteps) {
                if(reinitialize) {
                    node = session.getRootNode().getNode(base.substring(1));
                    node.getSession().refresh(false); // this mimicks somewhat cms updating behaviour
                    reinitialize = false;
                }
                try {
                    node = step(node);
                    ++nsuccess;
                } catch(PathNotFoundException ex) {
                    ++nmiss;
                    reinitialize = true;
                } catch(WorkflowException ex) {
                    ++nmiss;
                    reinitialize = true;
                } catch(InvalidItemStateException ex) {
                    if(ex.getMessage().endsWith("the item does not exist anymore")) {
                        ++nmiss;
                        reinitialize = true;
                    } else if(ex.getMessage().contains("Item cannot be saved")) {
                        ++nmiss;
                        reinitialize = true;
                    } else if(ex.getMessage().contains("modified externally")) {
                        ++nmiss;
                        reinitialize = true;
                    } else {
                        ++nfail;
                        RandomWalkTest.log.warn("FAILURE "+ex.getClass().getName()+": "+ex.getMessage(), ex);
                        node = node.getSession().getRootNode().getNode(base.substring(1));
                        node.getSession().refresh(false); // this mimicks somewhat cms updating behaviour
                    }
                } catch(ItemNotFoundException ex) {
                    ++nmiss;
                    reinitialize = true;
                } catch(ItemExistsException ex) {
                    ++nmiss;
                    reinitialize = true;
                } catch(NullPointerException ex) {
                    ++nfail;
                    reinitialize = true;
                    RandomWalkTest.log.warn("FAILURE "+ex.getClass().getName()+": "+ex.getMessage(), ex);
                } catch(ConcurrentModificationException ex) {
                    ++nfail;
                    reinitialize = true;
                    RandomWalkTest.log.warn("FAILURE "+ex.getClass().getName()+": "+ex.getMessage(), ex);
                } catch(Throwable ex) {
                    ++nfail;
                    reinitialize = true;
                    RandomWalkTest.log.warn("FAILURE "+ex.getClass().getName()+": "+ex.getMessage(), ex);
                }
                ++count;
                if(count%10 == 0) {
                    if(RandomWalkTest.log.isDebugEnabled()) {
                        System.err.println(id+"progress "+nsuccess+"/"+count); // System.err on purpose
                    }
                }
            }
        } catch(RepositoryException ex) {
            exception = ex;
        }
        nsteps = count;
        try {
            session.refresh(false);
        } catch(RepositoryException ex) {
            if(exception != null) {
                exception = ex;
            }
        }
    }

    private Node step(Node node) throws RepositoryException, ClassNotFoundException, WorkflowException, MappingException, RemoteException {
        Set<Action> actions = new LinkedHashSet<Action>();
        Node parent = node.getParent();
        if(parent.isNodeType("hippo:handle")) {
            parent = parent.getParent();
        }
        if(parent.getPath().length() < base.length()) {
            parent = null;
        }
        actions.add(BASE);
        if(parent != null && (parent.isNodeType("hippostd:folder") || parent.isNodeType("hippostd:directory"))) {
            actions.add(PARENT);
        } else {
            parent = null;
        }
        if((node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) && node.hasNodes()) {
            actions.add(SELECT);
        }
        List<WorkflowDescriptor> workflows = new LinkedList<WorkflowDescriptor>();
        for(String category : new String[] { "threepane", "default" }) {
            if(node.isNodeType("hippo:handle")) {
                for(NodeIterator children = node.getNodes(node.getName()); children.hasNext(); ) {
                    Node child = children.nextNode();
                    if(child != null) {
                        WorkflowDescriptor workflowDescriptor = workflowManager.getWorkflowDescriptor(category, child);
                        if(workflowDescriptor != null) {
                            workflows.add(workflowDescriptor);
                        }
                    }
                }
            } else if(node.isNodeType("hippo:document")) {
                WorkflowDescriptor workflowDescriptor = workflowManager.getWorkflowDescriptor(category, node);
                if(workflowDescriptor != null) {
                    workflows.add(workflowDescriptor);
                }
            }
        }

        for(WorkflowDescriptor workflowDescriptor : workflows) {
            Class[] interfaces = workflowDescriptor.getInterfaces();
            if(interfaces == null)
                continue;
            Map<String,Serializable> hints = workflowDescriptor.hints();
            for(Class interfaceClass : interfaces) {
                for(Method method : interfaceClass.getMethods()) {
                    String methodName = method.getName();
                    if(hints != null && hints.containsKey(methodName)) {
                        Serializable info = hints.get(methodName);
                        if(info instanceof Boolean) {
                            if(!((Boolean)info).booleanValue())
                                continue;
                        } else {
                            // warning don't understance
                        }
                    } else {
                        // warning no access arranged
                    }
                    if(ignoreMethods.contains(methodName)) {
                        // ignore
                    } else if(methodName.equals("obtainEditableInstance")) {
                        if(!node.isNodeType("hippostd:folder")) {
                            actions.add(new Action("edit", workflowDescriptor) {
                                @Override
                                protected Node execute(Node node) throws RepositoryException, WorkflowException, MappingException, RemoteException {
                                    EditableWorkflow workflow = (EditableWorkflow)workflowManager.getWorkflow(workflowDescriptor);
                                    Document document = workflow.obtainEditableInstance();
                                    Node draft = node.getSession().getNodeByUUID(document.getIdentity());
                                    draft.setProperty("defaultcontent:introduction", draft.getProperty("defaultcontent:introduction").getString() + "x");
                                    draft.getSession().save();
                                    workflow = (EditableWorkflow)workflowManager.getWorkflow("default", draft);
                                    document = workflow.commitEditableInstance();
                                    node = node.getSession().getNodeByUUID(document.getIdentity());
                                    node = node.getParent();
                                    if (node.isNodeType("hippo:handle"))
                                        node = node.getParent();
                                    return node;
                                }
                            });
                        }
                    } else if(methodName.equals("commitEditableInstance")) {
                        // skip
                    } else if(methodName.equals("disposeEditableInstance")) {
                        // skip
                    } else if(methodName.equals("publish")) {
                        actions.add(new Action<FullReviewedActionsWorkflow>("publish", workflowDescriptor) {
                            @Override
                            protected Node execute(FullReviewedActionsWorkflow workflow) throws RepositoryException, WorkflowException, MappingException, RemoteException {
                                workflow.publish();
                                return null;
                            }
                        });
                    } else if(methodName.equals("depublish")) {
                        actions.add(new Action<FullReviewedActionsWorkflow>("depublish", workflowDescriptor) {
                            @Override
                            protected Node execute(FullReviewedActionsWorkflow workflow) throws RepositoryException, WorkflowException, MappingException, RemoteException {
                                workflow.depublish();
                                return null;
                            }
                        });
                    } else if(methodName.equals("add")) {
                        actions.add(new Action<FolderWorkflow>("add-folder", workflowDescriptor) {
                            @Override
                            protected Node execute(FolderWorkflow workflow) throws RepositoryException, WorkflowException, MappingException, RemoteException {
                                String absPath = workflow.add("new-folder", "hippostd:folder", "folder"+random.nextInt(10));
                                return session.getRootNode().getNode(absPath.substring(1));
                            }
                        });
                        actions.add(new Action<FolderWorkflow>("add-document", workflowDescriptor) {
                            @Override
                            protected Node execute(Node node) throws RepositoryException, WorkflowException, MappingException, RemoteException {
                                super.execute(node);
                                return node;
                            }
                            @Override
                            protected Node execute(FolderWorkflow workflow) throws RepositoryException, WorkflowException, MappingException, RemoteException {
                                String absPath = workflow.add("new-document", "defaultcontent:news", "document"+random.nextInt(10));
                                return session.getRootNode().getNode(absPath.substring(1));
                            }
                        });
                    } else {
                        RandomWalkTest.log.error("No match "+methodName);
                    }
                }
            }
        }

        Action action = null; // will never be null after loop, added to remove compile error
        double weight = 0.0;
        for(Iterator<Action> iter = actions.iterator(); iter.hasNext(); ) {
            weight += iter.next().weight;
        }
        weight = (1.0 - random.nextDouble()) * weight;
        for(Iterator<Action> iter = actions.iterator(); iter.hasNext(); ) {
            action = iter.next();
            weight -= action.weight;
            if(weight <= 0.0) {
                break;
            }
        }

        return action.run(node);
    }
}
