/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.branch;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.statistics.Counter;
import org.hippoecm.hst.statistics.DefaultCounter;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.HASHABLE_PROPERTY_DELETED;
import static org.hippoecm.hst.configuration.HstNodeTypes.HASHABLE_PROPERTY_HASH;
import static org.hippoecm.hst.configuration.HstNodeTypes.HASHABLE_PROPERTY_UPSTREAM_HASH;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_HASHABLE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;

/**
 * <p>
 * Given a jcr node, it will compute a hash (digest) for that node, and add the mixin 'hst:hashable' and property
 * 'hst:hash'. The hash will be computed through the jcr properties (that are not skipped by excludeProperties)
 * and all the hashes of all children nodes. Note that all properties from the 'jcr:' namespace like uuid and such are
 * skipped in this NodeHasher because not relevant for this Hasher.
 * Note that the order of the children do impact the hash
 * </p>
 * <p>
 * The JCR property types that contribute are
 * <ul>
 * <li>String</li>
 * <li>Boolean</li>
 * <li>Double</li>
 * <li>Long</li>
 * <li>Decimal</li>
 * <li>Date</li>
 * </ul>
 * </p>
 */
public class WorkspaceHasher implements NodeHasher {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceHasher.class);

    private static final String JCR_PREFIX = "jcr:";

    private Set<String> excludeProperties;
    private final HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();

    public void setExcludeProperties(final Set<String> excludeProperties) {
        this.excludeProperties = excludeProperties;
    }

    /**
     * Computes and sets hashes for all nodes without persisting the changes
     *
     * @throws BranchException If {@code node} is not of type hst:workspace or a descendant of a node of type hst:workspace
     *                         or MD5 MessageDigest is not supported or some repository exception occurs
     */
    public String hash(final Node node, final boolean setHash, final boolean setUpstreamHash) throws BranchException {

        final Counter counter = new DefaultCounter();
        Task hashTask = null;
        try {
            if (HDC.isStarted()) {
                hashTask = HDC.getCurrentTask().startSubtask("HashTask Node");
                hashTask.setAttribute("Node path", node.getPath());
            }

            if (!isOrHasAncestorOfType(node, NODETYPE_HST_WORKSPACE)) {
                throw new BranchException(String.format("Cannot not hash the node '%s' because not of type '%s' or " +
                        "not a descendant of a node of type '%s'.", node.getPath(), NODETYPE_HST_WORKSPACE, NODETYPE_HST_WORKSPACE));
            }

            return startHashing(node, setHash, setUpstreamHash, counter);

        } catch (RepositoryException | NoSuchAlgorithmException e) {
            try {
                throw new BranchException(String.format("Could not hash the node '%s'", node.getPath()), e);
            } catch (RepositoryException e1) {
                throw new BranchException("Could not hash the node", e1);
            }
        } finally {
            if (hashTask != null) {
                hashTask.setAttribute("Number of hashed nodes", counter.getValue());
                hashTask.stop();
            }
        }
    }

    private boolean isOrHasAncestorOfType(final Node node, final String nodeType) throws RepositoryException {
        try {
            return node.isNodeType(nodeType) || isOrHasAncestorOfType(node.getParent(), nodeType);
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private String startHashing(final Node node, final boolean setHash,
                              final boolean setUpstreamHash, final Counter counter) throws NoSuchAlgorithmException, RepositoryException {
        long start = System.currentTimeMillis();
        byte[] bytes = doHash(node, true, setHash, setUpstreamHash, counter);
        log.info("Hashing '{}' containing '{}' nodes took '{}' ms", node.getPath(), counter.getValue(), (System.currentTimeMillis() - start));
        return hexBinaryAdapter.marshal(bytes);
    }

    private byte[] doHash(final Node node, final boolean isRoot, final boolean setHash,
                          final boolean setUpstreamHash, final Counter counter)
            throws RepositoryException, NoSuchAlgorithmException, BranchException {

        counter.increment();
        final MessageDigest md5 = MessageDigest.getInstance("MD5");
        if (!node.isNodeType(MIXINTYPE_HST_HASHABLE)) {
            node.addMixin(MIXINTYPE_HST_HASHABLE);
        }

        if (!isRoot) {
            md5.update(node.getName().getBytes());
        }

        SortedSet<String> sortedFilterPropertyNames = getSortedFilterPropertyNames(node);
        for (String propertyName : sortedFilterPropertyNames) {
            Property property = node.getProperty(propertyName);
            if (property.isMultiple()) {
                for (Value value : property.getValues()) {
                    update(md5, value);
                }
            } else {
                update(md5, property.getValue());
            }
        }

        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.hasProperty(HASHABLE_PROPERTY_DELETED) && child.getProperty(HASHABLE_PROPERTY_DELETED).getBoolean() == true) {
                log.debug("Node '{}' marked deleted hence skip for hashing and take the original hash/upstreamhash.", child.getPath());
                confirmDeletedState(child);
                continue;
            }
            byte[] hash = doHash(child, false, setHash, setUpstreamHash, counter);
            md5.update(hash);
        }
        byte[] digest = md5.digest();
        String hex = hexBinaryAdapter.marshal(digest);
        if (node.hasProperty(HASHABLE_PROPERTY_DELETED)) {
            throw new BranchException(String.format("Node '%s' is marked deleted and should never be (re)hashed", node.getPath()));
        }
        if (setHash) {
            node.setProperty(HASHABLE_PROPERTY_HASH, hex);
        }
        if (setUpstreamHash) {
            node.setProperty(HASHABLE_PROPERTY_UPSTREAM_HASH, hex);
        }
        return digest;

    }

    private void confirmDeletedState(final Node node) throws RepositoryException {
        if (!node.hasProperty(HASHABLE_PROPERTY_HASH)) {
            throw new BranchException(String.format("Node '%s' has an illegal state: If marked as deleted, property '%s' must" +
                    "be present and equal to '%s'.", node.getPath(), HASHABLE_PROPERTY_HASH, HASHABLE_PROPERTY_UPSTREAM_HASH));
        }
        if (!node.hasProperty(HASHABLE_PROPERTY_UPSTREAM_HASH)) {
            throw new BranchException(String.format("Node '%s' has an illegal state: If marked as deleted, property '%s' must" +
                    "be present and equal to '%s'.", node.getPath(), HASHABLE_PROPERTY_UPSTREAM_HASH, HASHABLE_PROPERTY_HASH));
        }
        if (!node.getProperty(HASHABLE_PROPERTY_HASH).getString().equals(node.getProperty(HASHABLE_PROPERTY_UPSTREAM_HASH).getString())) {
            throw new BranchException(String.format("Node '%s' has an illegal state: If marked as deleted, the '%s' and '%s' should " +
                    "be equal but they are not equal.", node.getPath(), HASHABLE_PROPERTY_HASH, HASHABLE_PROPERTY_UPSTREAM_HASH));
        }
    }

    // since the order in which node.getProperties returns the properties can change over time we need to return
    // a sorted (filtered) set of property names first
    private SortedSet<String> getSortedFilterPropertyNames(final Node node) throws RepositoryException {
        SortedSet<String> props = new TreeSet<>();
        for (Property property : new PropertyIterable(node.getProperties())) {
            if (excludeProperties.contains(property.getName()) || property.getName().startsWith(JCR_PREFIX)) {
                continue;
            }
            props.add(property.getName());
        }
        return props;
    }

    private void update(final MessageDigest md5, final Value value) throws RepositoryException {
        md5.update(PropertyType.nameFromValue(value.getType()).getBytes());
        if (value.getType() == PropertyType.STRING) {
            md5.update(value.getString().getBytes());
            return;
        }
        if (value.getType() == PropertyType.BOOLEAN) {
            md5.update(String.valueOf(value.getBoolean()).getBytes());
            return;
        }
        if (value.getType() == PropertyType.LONG) {
            md5.update(String.valueOf(value.getLong()).getBytes());
            return;
        }
        if (value.getType() == PropertyType.DOUBLE) {
            md5.update(String.valueOf(value.getDouble()).getBytes());
            return;
        }
        if (value.getType() == PropertyType.DECIMAL) {
            md5.update(String.valueOf(value.getDecimal()).getBytes());
            return;
        }
        if (value.getType() == PropertyType.DATE) {
            md5.update(String.valueOf(value.getDate()).getBytes());
        }
    }
}
