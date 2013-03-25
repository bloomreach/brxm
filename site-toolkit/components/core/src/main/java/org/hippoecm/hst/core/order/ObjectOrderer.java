/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to order objects into an "execution" order. Each object must have a name. It may specify a
 * list of pre-requisites and a list of post-requisites.
 */
public class ObjectOrderer<T> {

    public static final String REQUISITES_SEPARATOR_CHARS = " ,\t\r\n";

    private static Logger log = LoggerFactory.getLogger(ObjectOrderer.class);

    private final String objectOrderingName;

    private List<OrderableObjectHolder<T>> orderableObjectHolderList = null;

    private Map<String, OrderableObjectHolder<T>> orderableObjectHolderMap = null;

    private Map<OrderableObjectHolder<T>, Node<T>> nodeMap = null;

    private Node<T> leaderNode;

    private Node<T> trailerNode;

    /**
     * Flag to simply suppresses all exceptions (but it will still log them).
     * It set to true, it does not re-throw the exception.
     */
    private boolean ignoreExceptions;

    public ObjectOrderer(String objectOrderingName) {
        this.objectOrderingName = objectOrderingName;
    }

    public boolean isIgnoreExceptions() {
        return ignoreExceptions;
    }

    public void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    /**
     * Adds a new object. All invocations of {@link #add(Object, String, String, String)} should
     * occur before invoking {@link #getOrderedObjects()}.
     * 
     * @param object
     *            an object to be sorted into order based on prerequisites and postrequisites
     * @param objectName
     *            a unique name for the object
     * @param prerequisites
     *            the names of objects that should precede this object in
     *            the list (or null), separated by ' ', ',', '\t', '\r' or '\n'.
     * @param postrequisites
     *            the names of objects that should follow this object in
     *            the list (or null), separated by ' ', ',', '\t', '\r' or '\n'.
     */
    public void add(T object, String objectName, String prerequisites, String postrequisites) {
        if (orderableObjectHolderMap == null) {
            orderableObjectHolderMap = new HashMap<String, OrderableObjectHolder<T>>();
            orderableObjectHolderList = new ArrayList<OrderableObjectHolder<T>>();
        }

        OrderableObjectHolder<T> o = getOrderableObjectHolder(objectName);

        if (o != null) {
            log.error("Duplicate object by name, '{}'. Original object: {}", objectName, o.getObject());
            return;
        }

        o = new OrderableObjectHolder<T>(object, objectName, prerequisites, postrequisites);

        orderableObjectHolderMap.put(objectName, o);
        orderableObjectHolderList.add(o);
    }

    /**
     * Uses the information provided by {@link #add(Object, String, String, String)} to order the
     * objects into an appropriate order based on the pre- and post-reqts provided. Errors such as
     * cyclic dependencies or unrecognized names are logged and ignored.
     */
    public List<T> getOrderedObjects() {
        if (orderableObjectHolderMap == null) {
            return Collections.emptyList();
        }

        try {
            nodeMap = new HashMap<OrderableObjectHolder<T>, Node<T>>();

            initializeGraph();

            return trailerNode.getOrder();
        } finally {
            nodeMap = null;
            leaderNode = null;
            trailerNode = null;
        }
    }

    private OrderableObjectHolder<T> getOrderableObjectHolder(String objectName) {
        return orderableObjectHolderMap.get(objectName);
    }

    private void initializeGraph() {
        addNodes();

        if (leaderNode == null) {
            leaderNode = new Node<T>(null, "*-leader-*");
        }

        if (trailerNode == null) {
            trailerNode = new Node<T>(null, "*-trailer-*");
        }

        addDependencies();
    }

    private Node<T> getNode(String name) {
        return nodeMap.get(getOrderableObjectHolder(name));
    }

    private void addNodes() {
        Iterator<OrderableObjectHolder<T>> i = orderableObjectHolderList.iterator();

        while (i.hasNext()) {
            OrderableObjectHolder<T> o = i.next();

            Node<T> node = new Node<T>(o.getObject(), o.getName());

            nodeMap.put(o, node);

            if ("*".equals(o.getPostrequisites())) {
                if (leaderNode == null) {
                    leaderNode = node;
                } else {
                    log.error("Duplicate leader object by name, '{}'. Original object: {}", leaderNode.getName(), o.getObject());
                }
            }

            if ("*".equals(o.getPrerequisites())) {
                if (trailerNode == null) {
                    trailerNode = node;
                } else {
                    log.error("Duplicate trailer object by name, '{}'. Original object: {}", trailerNode.getName(), o.getObject());
                }
            }

        }
    }

    private void addDependencies() {
        Iterator<OrderableObjectHolder<T>> i = orderableObjectHolderList.iterator();

        while (i.hasNext()) {
            OrderableObjectHolder<T> o = i.next();
            addDependencies(o, getNode(o.getName()));
        }
    }

    private void addDependencies(OrderableObjectHolder<T> orderable, Node<T> node) {
        addPrerequisites(orderable, node);
        addPostrequisites(orderable, node);

        try {
            if (node != leaderNode) {
                node.addDependency(leaderNode);
            }

            if (node != trailerNode) {
                trailerNode.addDependency(node);
            }
        } catch (ObjectOrdererRuntimeException ex) {
            // This code is unreachable ... but nonetheless.

            String name = node.getName();
            OrderableObjectHolder<T> trigger = getOrderableObjectHolder(name);

            if (log.isDebugEnabled()) {
                log.error("Error in dependency cycle. _objectType: '" + objectOrderingName + "', object: " + trigger.getObject(), ex);
            } else {
                log.error("Error in dependency cycle. _objectType: '" + objectOrderingName + "', object: {}. {}", trigger.getObject(), ex.toString());
            }

            if (!ignoreExceptions) {
                throw ex;
            }
        }
    }

    private void addPrerequisites(OrderableObjectHolder<T> ordering, Node<T> node) {
        String prereqs = ordering.getPrerequisites();

        if ("*".equals(prereqs)) {
            return;
        }

        String [] names = StringUtils.split(prereqs, REQUISITES_SEPARATOR_CHARS);

        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                String prename = names[i];

                Node<T> prenode = getNode(prename);

                if (prenode == null) {
                    log.error("Bad dependency for '{}'. pre node not found by name, '{}'.", objectOrderingName, prename);
                    continue;
                }

                try {
                    node.addDependency(prenode);
                } catch (ObjectOrdererRuntimeException ex) {
                    if (log.isDebugEnabled()) {
                        log.error("Error in dependency cycle. _objectType: '" + objectOrderingName + "', object: " + ordering.getObject(), ex);
                    } else {
                        log.error("Error in dependency cycle. _objectType: '" + objectOrderingName + "', object: {}. {}", ordering.getObject(), ex.toString());
                    }

                    if (!ignoreExceptions) {
                        throw ex;
                    }
                }
            }
        }
    }

    private void addPostrequisites(OrderableObjectHolder<T> ordering, Node<T> node) {
        String postreqs = ordering.getPostrequisites();

        if ("*".equals(postreqs))
            return;

        String [] names = StringUtils.split(postreqs, REQUISITES_SEPARATOR_CHARS);

        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                String postname = names[i];

                Node<T> postnode = getNode(postname);

                if (postnode == null) {
                    log.error("Bad dependency for '{}'. post node not found by name, '{}'.", objectOrderingName, postname);
                } else {
                    try {
                        postnode.addDependency(node);
                    } catch (ObjectOrdererRuntimeException ex) {
                        if (log.isDebugEnabled()) {
                            log.error("Error in dependency cycle. _objectType: '" + objectOrderingName + "', object: " + ordering.getObject(), ex);
                        } else {
                            log.error("Error in dependency cycle. _objectType: '" + objectOrderingName + "', object: {}. {}", ordering.getObject(), ex.toString());
                        }

                        if (!ignoreExceptions) {
                            throw ex;
                        }
                    }
                }
            }
        }
    }

    private static class Node<T> {

        private T object;

        private String name;

        private List<Node<T>> dependencies;

        public Node(T o, String name) {
            object = o;
            this.name = name;
            dependencies = new ArrayList<Node<T>>();
        }

        public String getName() {
            return name;
        }

        public void addDependency(Node<T> n) {
            if (n.isReachable(this))
                throw new ObjectOrdererRuntimeException("A cycle has been detected from the initial object [" + name
                        + "] " + object);

            dependencies.add(n);
        }

        private boolean isReachable(Node<T> n) {
            boolean reachable = (n == this);
            Iterator<Node<T>> i = dependencies.iterator();

            while (i.hasNext() && !reachable) {
                Node<T> dep = i.next();
                reachable = (dep == n) ? true : dep.isReachable(n);
            }

            return reachable;
        }

        public List<T> getOrder() {
            List<T> result = new ArrayList<T>();
            fillOrder(result);

            return result;
        }

        private void fillOrder(List<T> result) {
            if (result.contains(object)) {
                return;
            }

            Iterator<Node<T>> i = dependencies.iterator();

            while (i.hasNext()) {
                Node<T> dep = i.next();
                dep.fillOrder(result);
            }

            if (object != null) {
                result.add(object);
            }
        }
    }

}