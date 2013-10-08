/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.testutils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.hippoecm.repository.util.Utilities;
import org.slf4j.Logger;

import static junit.framework.Assert.fail;

public class JcrState {

    private final String path;
    private final Session session;
    private final Logger log;
    private final int hash;
    private final Map<String, Integer> hashes = new HashMap<>();
    private ByteArrayOutputStream debugStream;

    public JcrState(final String path, final Session session, final Logger log) throws RepositoryException {
        this.path = path;
        this.session = session;
        this.log = log;
        final Node node = session.getNode(path);
        hash = hashCode(node);
        for (Node child : new NodeIterable(node.getNodes())) {
            hashes.put(child.getName(), hashCode(child));
        }
        for (Property property : new PropertyIterable(node.getProperties())) {
            hashes.put("@" + property.getName(), hashCode(property));
        }
        if (log.isDebugEnabled()) {
            debugStream = new ByteArrayOutputStream();
            Utilities.dump(new PrintStream(debugStream), node);
        }
    }

    public void check() throws Exception {
        session.refresh(false);
        if (hash != hashCode(session.getNode(path))) {
            Map<String, Integer> afterHashes = new HashMap<String, Integer>();
            final Node node = session.getNode(path);
            for (Node configChild : new NodeIterable(node.getNodes())) {
                afterHashes.put(configChild.getName(), hashCode(configChild));
            }
            for (Property configProp : new PropertyIterable(node.getProperties())) {
                afterHashes.put("@" + configProp.getName(), hashCode(configProp));
            }

            Set<String> missing = new HashSet<>();
            Set<String> changed = new HashSet<>();
            Set<String> added = new HashSet<>();
            for (Map.Entry<String, Integer> oldHashEntry : hashes.entrySet()) {
                final String name = oldHashEntry.getKey();
                if (!afterHashes.containsKey(name)) {
                    missing.add(name);
                } else if (!afterHashes.get(name).equals(oldHashEntry.getValue())) {
                    changed.add(name);
                }
            }
            for (String name : afterHashes.keySet()) {
                if (!hashes.containsKey(name)) {
                    added.add(name);
                }
            }

            if (log.isDebugEnabled()) {
                System.out.println("Before:");
                System.out.write(debugStream.toByteArray());

                System.out.println("After:");
                Utilities.dump(session.getNode(path));
            }

            fail("Configuration has been changed, but not reverted; make sure changes in tearDown overrides are saved.  " +
                    "Detected changes: added = " + added + ", changed = " + changed + ", removed = " + missing + ".  " +
                    "Use RepositoryTestCase#setConfigurationChangeDebugPath to narrow down.");
        }
    }

    protected int hashCode(Node node) throws RepositoryException {
        String name = node.getName();
        String type = node.getPrimaryNodeType().getName();
        int hashCode = name.hashCode() + type.hashCode() * 31;

        int propHash = 0;
        for (Property property : new PropertyIterable(node.getProperties())) {
            propHash = propHash + hashCode(property);
        }
        hashCode = 31 * hashCode + propHash;

        boolean orderable = node.getPrimaryNodeType().hasOrderableChildNodes();
        for (NodeType mixin : node.getMixinNodeTypes()) {
            if (mixin.hasOrderableChildNodes()) {
                orderable = true;
            }
        }
        int childHash = 0;
        for (Node child : new NodeIterable(node.getNodes())) {
            if (child instanceof HippoNode) {
                if (((HippoNode) child).isVirtual()) {
                    continue;
                }
            }
            if (orderable) {
                childHash = 31 * childHash + hashCode(child);
            } else {
                childHash = childHash + hashCode(child);
            }
        }
        hashCode = 31 * hashCode + childHash;
        return hashCode;
    }

    protected int hashCode(Property property) throws RepositoryException {
        int hashCode = property.getName().hashCode();
        if (property.isMultiple()) {
            for (Value value : property.getValues()) {
                hashCode = hashCode(value) + (hashCode * 31);
            }
        } else {
            hashCode = hashCode(property.getValue()) + (hashCode * 31);
        }
        return hashCode;
    }

    protected int hashCode(Value value) throws RepositoryException {
        return value.getString().hashCode();
    }

}
