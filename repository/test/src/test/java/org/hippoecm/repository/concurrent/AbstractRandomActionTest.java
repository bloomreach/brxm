/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.core.state.ItemStateException;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.concurrent.action.Action;
import org.hippoecm.repository.concurrent.action.ActionContext;
import org.hippoecm.repository.concurrent.action.ActionFailure;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for tests that want to hit the repository with random actions.
 * For adding actions look at the action package and the Action class in particular.
 * This test relies on the presence of default content in the repository.
 */
abstract class AbstractRandomActionTest {
    public static final Logger log = LoggerFactory.getLogger(AbstractRandomActionTest.class);

    private final String ASSET_BASE_PATH = "/content/assets";
    private final String DOCUMENT_BASE_PATH = "/content/documents";
    private final String IMAGE_BASE_PATH = "/content/gallery";

    protected ActionContext context;

    private static final int ALL_EVENTS = Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED
            | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;

    protected Random random = new Random();
    protected Repository repository;
    protected HippoRepository hippoRepository;
    protected int nthreads;
    private boolean stopRunning = false;
    private final long duration;

    protected AbstractRandomActionTest(long duration, TimeUnit units, int nthreads) {
        this.duration = units.toMillis(duration);
        this.nthreads = nthreads;
    }

    @Before
    public void setUp() throws Exception {
        hippoRepository = HippoRepositoryFactory.getHippoRepository();
        repository = hippoRepository.getRepository();

        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        WorkflowManager workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();

        final String clusterNodeId = getClusterNodeId(session);
        Workflow wf = workflowManager.getWorkflow("threepane", session.getNode("/content/documents"));
        ((FolderWorkflow)wf).add("new-folder", "hippostd:folder", clusterNodeId);
        wf = workflowManager.getWorkflow("threepane", session.getNode("/content/assets"));
        ((FolderWorkflow)wf).add("new-file-folder", "asset gallery", clusterNodeId);
        session.save();
        session.logout();

        context = new ActionContext(DOCUMENT_BASE_PATH + "/" + clusterNodeId, ASSET_BASE_PATH + "/" + clusterNodeId, IMAGE_BASE_PATH + "/" + clusterNodeId);
    }

    @After
    public void tearDown() throws Exception {
        if (repository != null) {
            if (Boolean.getBoolean("stampede.prompt")) {
                System.out.println("Press enter to stop repository...");
                System.console().readLine();
            }
            hippoRepository.close();
        }
    }

    protected class RandomActionRunner extends Thread {
        private final Session session;
        private final List<Action> allActions;
        private int stepsCount = 0;
        private long endTime = -1;
        //private final long timeOut;
        //private boolean stopRunning = false;
        private boolean terminate = false;
        private int misses = 0;
        private int successes = 0;
        private boolean reinitialize = false;
        private boolean completed = false;
        private final List<ActionFailure> failures = new ArrayList<ActionFailure>();

        protected RandomActionRunner(Session session, List<Action> actions) {
            this.session = session;
            this.allActions = actions;
        }

        public void terminate(ExecutorService executor) throws InterruptedException, ExecutionException {
            if (isAlive()) {
                terminate = true;
                sleep(3000); // give the thread time to finish its regular action
                if (isAlive()) {
                    interrupt();
                    sleep(3000); // give the thread time to gracefully terminate
                    if (isAlive()) {
                        System.err.println("FAILURE: thread " + getName() + " seems to hang. Stacktrace:");
                        for (StackTraceElement elt : getStackTrace()) {
                            System.err.println("\t" + elt.getClassName() + "." + elt.getMethodName() + ":" + elt.getLineNumber());
                        }
                    }
                }
            }
            try {
                // log out on a separate thread so we won't fall victim to deadlock
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        session.logout();
                    }
                }).get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("Unable to log out session on thread " + getName() + ": timed out");
            }
        }

        @Override
        public void run() {
            if (duration > 0) {
                endTime = System.currentTimeMillis() + duration;
            }
            final EventListener eventListener = new EventListener() {
                @Override
                public void onEvent(EventIterator events) {
                    // NOP
                }
            };
            try {
                // adding an event listener emulates typical CMS usage
                // for one thing, the access manager is invoked to see if 
                // the user has access to the node the event was on
                session.getWorkspace().getObservationManager().addEventListener(eventListener, ALL_EVENTS, "/", true, null, null, true);
            } catch (RepositoryException ex) {
                log.error("Failed to add event listener for thread " + Thread.currentThread().getName(), ex);
            }
            Node node = null;
            while (keepRunning()) {
                if (node == null || reinitialize) {
                    try {
                        session.refresh(false);
                        node = session.getRootNode().getNode(DOCUMENT_BASE_PATH.substring(1));
                        reinitialize = false;
                    } catch (RepositoryException ex) {
                        System.err.println(ex.getClass().getName()+": "+ex.getMessage());
//                        ex.printStackTrace(System.err);
                        // no use to keep trying
                        stopRunning = true;
                    }
                }
                node = step(node);
            }
            try {
                session.getWorkspace().getObservationManager().removeEventListener(eventListener);
            } catch (RepositoryException ex) {
                log.error("Failed to remove event listener for thread " + Thread.currentThread().getName());
            }
            completed = true;
        }

        public int getMisses() {
            return misses;
        }

        public int getSuccesses() {
            return successes;
        }

        public List<ActionFailure> getFailures() {
            return failures;
        }

        public Session getSession() {
            return session;
        }

        /*
         * Perform a random action
         */
        private Node step(Node node) {
            List<Action> actions = new ArrayList<Action>(allActions.size());
            for (Action action : allActions) {
                try {
                    if (action.canOperateOnNode(node)) {
                        actions.add(action);
                    }
                } catch (Exception ex) {
                    reinitialize = true;
                    if (log.isDebugEnabled()) {
                        log.debug("Failed to determine if action is able to operate on node", ex);
                    }
                }
            }
            if (actions.size() > 0) {
                Action action = null;
                double weight = 0.0;
                for (Action a : actions) {
                    weight += a.getWeight();
                }
                weight = (1.0 - random.nextDouble()) * weight;
                for (Action a : actions) {
                    weight -= a.getWeight();
                    if (weight <= 0.0) {
                        action = a;
                        break;
                    }
                }
                if (action != null) {
                    try {
                        Node result = action.execute(node);
                        successes++;
                        return result;
                    } catch (Throwable t) {
                        handleThrowable(t, action, node);
                    }
                }
            }
            return null;
        }

        private boolean keepRunning() {
            if (terminate) {
                return false;
            }
            if (stopRunning) {
                log.error("Stopping RandomActionRunner[" + Thread.currentThread().getName() + "] because of unrecoverable error");
                return false;
            }
            if (endTime > 0 && endTime < System.currentTimeMillis()) {
                log.info("Stopping RandomActionRunner[" + Thread.currentThread().getName() + "] because of end of test run");
                return false;
            }
            if (failures.size() > 50) {
                log.warn("Stopping RandomActionRunner[" + Thread.currentThread().getName() + "] because of too many failures");
                return false;
            }
            return true;
        }

        private void handleThrowable(Throwable t, Action action, Node node) {
            if (t instanceof Exception) {
                if (isRecoverableException((Exception)t)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Recoverable exception in action " + action.getClass().getSimpleName(), t);
                    }
                    misses++;
                    return;
                }
            }
            String path = null;
            try {
                path = node.getPath();
            } catch (Exception ignore) {
            }
            log.error("FAILURE in thread " + Thread.currentThread().getName() + " performing action "
                    + action.getClass().getSimpleName() + " on path " + path, t);
            ActionFailure failure = new ActionFailure(t, action, path);
            failures.add(failure);
            reinitialize = true;
        }

        private boolean isRecoverableException(Exception e) {
            if (e instanceof RepositoryException) {
                return !(e.getCause() != null && !(e.getCause() instanceof RepositoryException || e.getCause() instanceof ItemStateException));
            }
            if (e instanceof ItemStateException) {
                return !(e.getCause() != null && !(e.getCause() instanceof RepositoryException || e.getCause() instanceof ItemStateException));
            }
            if (e instanceof WorkflowException) {
                if (!e.getMessage().equals("Cannot rename document to same name")) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean report(RandomActionRunner[] runners, long startTime, long endTime, Collection<Action> actionSet) {
        int totalSteps = 0;
        int totalWriteSteps = 0;
        List<ActionFailure> failures = new ArrayList<ActionFailure>();
        for (Action action : actionSet) {
            totalSteps += action.getCount();
            if (action.isWriteAction()) {
                totalWriteSteps += action.getCount();
            }
            int actionFailures = 0;
            for (RandomActionRunner runner : runners) {
                for (ActionFailure failure : runner.getFailures()) {
                    failures.add(failure);
                    if (failure.getAction() == action) {
                        actionFailures++;
                    }
                }
            }
            int averageTime = (action.getCount() == 0 || action.getTimeSpent() == 0) ? 0 : (action.getTimeSpent() / action.getCount());
            System.err.println(action.getClass().getSimpleName() + " executed " + action.getCount() + " times; " +
                    "failed " + actionFailures + " times; " +
                    "total time spent " + action.getTimeSpent() + " ms; " +
                    "average time " + averageTime);
        }
        int totalFailures = 0, totalSuccesses = 0, totalMisses = 0;
        for (RandomActionRunner runner : runners) {
            totalFailures += runner.getFailures().size();
            totalSuccesses += runner.getSuccesses();
            totalMisses += runner.getMisses();
        }
        System.err.println("Total actions run: " + totalSteps);
        System.err.println("Total successful actions: " + totalSuccesses);
        System.err.println("Total write actions run: " + totalWriteSteps);
        // missed actions are those that have recoverable exceptions; added for completeness
        System.err.println("Total missed actions: " + totalMisses);
        System.err.println("Total failed actions: " + totalFailures);
        System.err.println("Successrate: " + (totalSuccesses + totalMisses) / (double)totalSteps);
        System.err.println("Total time it took: " + (endTime - startTime) / 1000.0 + " sec");
        if (totalFailures > 0) {
            System.err.println("Failures:");
            for (ActionFailure failure : failures) {
                failure.printFailure();
            }
            return false;
        }
        return true;
    }

    private String getClusterNodeId(Session session) {
        String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusteNodeId == null) {
            clusteNodeId = "default";
        }
        return clusteNodeId;
    }

}
