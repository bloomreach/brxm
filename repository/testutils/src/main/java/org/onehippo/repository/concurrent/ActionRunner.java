/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.spi.Event;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.filter.Filterable;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.concurrent.action.Action;
import org.onehippo.repository.concurrent.action.ActionContext;
import org.onehippo.repository.concurrent.action.ActionFailure;
import org.slf4j.Logger;

public class ActionRunner extends Thread {

    private static boolean stopRunning = false;

    protected final ActionContext context;
    protected final Session session;
    protected final Logger log;
    protected final List<Class<? extends Action>> actions;
    private final Random random = new Random();
    private final long duration;
    private final long throttle;
    private boolean suppressRecoverableExceptions;

    private long endTime = -1;
    private boolean terminate = false;
    private int suspiciousExceptionCount = 0;
    private int recoverableExceptionCount = 0;
    private int successes = 0;
    private final List<ActionFailure> failures = new ArrayList<ActionFailure>();

    public ActionRunner(ActionContext actionContext, List<Class<? extends Action>> actions, long duration, long throttle) {
        this(actionContext, actions, duration, throttle, false);
    }

    public ActionRunner(ActionContext actionContext, List<Class<? extends Action>> actions, long duration, long throttle, boolean suppressRecoverableExceptions) {
        this.context = actionContext;
        this.session = actionContext.getSession();
        this.log = actionContext.getLog();
        this.actions = actions;
        this.duration = duration;
        this.throttle = throttle;
        this.suppressRecoverableExceptions = suppressRecoverableExceptions;
    }

    public void initialize() throws Exception {}

    public void terminate() throws InterruptedException, ExecutionException {
        if (isAlive()) {
            terminate = true;
            sleep(1000);
            if (isAlive()) {
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement elt : getStackTrace()) {
                    sb.append("\t").append(elt.getClassName()).append(".").append(elt.getMethodName()).append(":").append(elt.getLineNumber());
                }
                log.error("FAILURE: thread " + getName() + " seems to hang. Stacktrace:" + sb);
            }
        }
    }

    @Override
    public void run() {
        if (suppressRecoverableExceptions) {
            setupLogFilter();
        }

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
            session.getWorkspace().getObservationManager().addEventListener(eventListener, Event.ALL_TYPES, "/", true, null, null, true);
        } catch (RepositoryException ex) {
            log.error("Failed to add event listener for thread " + Thread.currentThread().getName(), ex);
        }
        while (keepRunning()) {
            try {
                step(selectNode());
            } catch (RepositoryException e) {
                log.error("Failed to select node: " + e);
                stopRunning = true;
            }
        }
        try {
            session.getWorkspace().getObservationManager().removeEventListener(eventListener);
        } catch (RepositoryException ex) {
            log.error("Failed to remove event listener for thread " + Thread.currentThread().getName());
        }
    }

    protected void setupLogFilter() {
        final Filter filter = new AbstractFilter() {
            @Override
            public Result filter(final LogEvent event) {
                if (event.getLoggerFqcn().startsWith("org.onehippo.repository.concurrent")
                    || event.getLoggerFqcn().startsWith("org.hippoecm.repository.concurrent")) {
                    return Result.NEUTRAL;
                }
                if (event.getThrown() != null) {
                    if (isRecoverable(event.getThrown())) {
                        return Result.DENY;
                    }
                }
                return Result.NEUTRAL;
            }
        };
        final Configuration config = LoggerContext.getContext(false).getConfiguration();
        for (Appender appender : config.getAppenders().values()) {
            if (appender instanceof Filterable /* should always be true, at least for standard log4j2 appenders */) {
                ((Filterable)appender).addFilter(filter);
            }
        }
    }

    protected Node selectNode() throws RepositoryException {
        Node root;
        if (random.nextGaussian() < .75) {
            root = context.getDocumentBase();
        } else {
            root = context.getAssetBase();
        }
        Node node = null;
        while (node == null) {
            try {
                node = traverse(root);
            } catch (RepositoryException e) {
                log.debug("Failed to traverse nodes: " + e);
            }
        }
        return node;
    }

    private Node traverse(final Node node) throws RepositoryException {
        if (node.hasNodes() && random.nextGaussian() < .5) {
            final NodeIterator nodes = node.getNodes();
            final int size = (int)nodes.getSize();
            if (size > 0) {
                int index = random.nextInt(size);
                if (index > 0) {
                    nodes.skip(index-1);
                }
                Node child = nodes.nextNode();
                if (child.isNodeType("hippo:handle")) {
                    child = child.getNode(child.getName());
                    return child;
                }
                return traverse(child);
            }
        }
        return node;
    }

    public int getSuspiciousExceptionCount() {
        return suspiciousExceptionCount;
    }

    public int getRecoverableExceptionCount() {
        return recoverableExceptionCount;
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

    public ActionContext getContext() {
        return context;
    }

    private Node step(Node node) {
        final Action action = getAction(node);
        if (action != null) {
            try {
                final Node result = action.execute(node);
                successes++;
                throttle();
                return result;
            } catch (Throwable t) {
                handleThrowable(t, action, node);
            }
        }
        return null;
    }

    protected Action getAction(final Node node) {
        final List<Action> actions = new ArrayList<Action>(this.actions.size());
        for (Class<? extends Action> actionClass : this.actions) {
            try {
                final Action action = context.getAction(actionClass);
                if (action.canOperateOnNode(node)) {
                    actions.add(action);
                }
            } catch (Exception ex) {
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
            return action;
        }
        return null;

    }

    private void throttle() {
        try {
            Thread.sleep(throttle);
        } catch (InterruptedException ignore) {
        }
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
        if (isSuspicious(t)) {
            log.error("Suspicious exception in action {}", action.getClass().getSimpleName(), t);
            suspiciousExceptionCount++;
            action.addSuspiciousException();
            return;
        }
        if (isRecoverable(t)) {
            log.debug("Recoverable exception in action {}", action.getClass().getSimpleName(), t);
            recoverableExceptionCount++;
            action.addRecoverableException();
            return;
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
    }

    private boolean isSuspicious(final Throwable t) {
        if (t instanceof AssertionError) {
            final StackTraceElement[] stackTrace = t.getStackTrace();
            if (stackTrace.length > 0
                    && stackTrace[0].getClassName().equals("org.apache.jackrabbit.core.ItemSaveOperation")
                    && stackTrace[0].getMethodName().equals("removeTransientItems")) {
                return true;
            }
        }
        return false;
    }

    private boolean isRecoverable(final Throwable t) {
        // calling isSuspicious because isRecoverable is also invoked from the log filter created in #setupLogFilter
        // suspicious exceptions must be logged
        if (isSuspicious(t)) {
            return false;
        }
        if (t instanceof RepositoryException) {
            return !(t.getCause() != null && !(t.getCause() instanceof RepositoryException || t.getCause().getClass().getSimpleName().endsWith("ItemStateException")));
        }
        if (t.getClass().getSimpleName().endsWith("ItemStateException")) {
            return !(t.getCause() != null && !(t.getCause() instanceof RepositoryException || t.getCause().getClass().getSimpleName().endsWith("ItemStateException")));
        }
        if (t instanceof WorkflowException) {
            return true;
        }
        return false;
    }
}
