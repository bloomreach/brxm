/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;

public class DeprecatedVersionWorkflowImpl extends Document implements VersionWorkflow, InternalWorkflow {


    private static Node findSubject(Node version) throws RepositoryException {
        Session session = version.getSession();
        // use hippo:paths to find handle; then use the matching variant 
        if (version.hasProperty(HippoNodeType.HIPPO_PATHS)) {
            Value[] paths = version.getProperty(HippoNodeType.HIPPO_PATHS).getValues();
            if (paths.length > 1) {
                String handleUuid = paths[1].getString();
                Node handle = session.getNodeByUUID(handleUuid);

                Map<String, String> criteria = getCriteria(version, handle);
                for (NodeIterator variants = handle.getNodes(handle.getName()); variants.hasNext();) {
                    Node variant = variants.nextNode();
                    if (matches(variant, criteria)) {
                        return variant;
                    }
                }
            }
        }

        String uuid = version.getProperty("jcr:frozenUuid").getString();
        return session.getNodeByUUID(uuid);
    }

    private static Map<String, String> getCriteria(Node subject, Node handle) throws RepositoryException {
        Map<String, String> criteria = new TreeMap<String, String>();
        try {
            if (handle != null && handle.isNodeType(HippoNodeType.NT_HANDLE)
                    && handle.hasProperty(HippoNodeType.HIPPO_DISCRIMINATOR)) {
                Value[] discriminators = handle.getProperty(HippoNodeType.HIPPO_DISCRIMINATOR).getValues();
                for (Value discriminator : discriminators) {
                    String key = discriminator.getString();
                    if (subject.hasProperty(key)) {
                        criteria.put(key, subject.getProperty(key).getString());
                    }
                }
            } else if (subject.isNodeType("hippostd:publishable")) {
                String key = "hippostd:state";
                if (subject.hasProperty(key)) {
                    criteria.put(key, subject.getProperty(key).getString());
                }
            }
        } catch (ItemNotFoundException ex) {
            // ignore, handle does not exist; empty map is fine
        }
        return criteria;
    }

    private static boolean matches(Node node, Map<String, String> criteria) throws ValueFormatException,
            PathNotFoundException, RepositoryException {
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            if (!node.hasProperty(entry.getKey())
                    || !node.getProperty(entry.getKey()).getString().equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static void restore(Node target, Node source) throws RepositoryException {
        if (!target.getPrimaryNodeType().getName().equals(source.getProperty("jcr:frozenPrimaryType").getString())) {
            throw new RepositoryException("Cannot restore across different node types");
        }

        if (source.hasProperty("jcr:frozenMixinTypes")) {
            Value[] mixins = source.getProperty("jcr:frozenMixinTypes").getValues();
            for (Value mixin : mixins) {
                target.addMixin(mixin.getString());
            }
        }

        NodeType[] mixinNodeTypes = target.getMixinNodeTypes();
        NodeType[] nodeTypes = new NodeType[mixinNodeTypes.length + 1];
        nodeTypes[0] = target.getPrimaryNodeType();
        if (mixinNodeTypes.length > 0) {
            System.arraycopy(mixinNodeTypes, 0, nodeTypes, 1, mixinNodeTypes.length);
        }

        for (NodeIterator iter = source.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            // ignore virtual nodes
            if (child instanceof HippoNode) {
                Node canonical = ((HippoNode) child).getCanonicalNode();
                if (canonical == null || !canonical.isSame(child)) {
                    continue;
                }
            }

            String childType = child.getProperty("jcr:frozenPrimaryType").getString();
            // virtual nodes are checked with rep:root type
            if ("rep:root".equals(childType)) {
                continue;
            }
            if (target.hasNode(child.getName() + "[" + child.getIndex() + "]")) {
                Node childTarget = target.getNode(child.getName() + "[" + child.getIndex() + "]");
                restore(childTarget, child);
            } else {
                for (NodeType nt : nodeTypes) {
                    if (nt.canAddChildNode(child.getName(), childType)) {
                        Node childTarget = target.addNode(child.getName(), childType);
                        restore(childTarget, child);
                        break;
                    }
                }
            }
        }

        for (PropertyIterator iter = source.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if (prop.getName().startsWith("jcr:frozen") || prop.getName().startsWith("jcr:uuid") ||
                    prop.getName().equals(HippoNodeType.HIPPO_RELATED) ||
                    prop.getName().equals(HippoNodeType.HIPPO_COMPUTE) ||
                    prop.getName().equals(HippoNodeType.HIPPO_PATHS)) {
                continue;
            }
            if (target.hasProperty(prop.getName())) {
                continue;
            }

            if (prop.getDefinition().isMultiple()) {
                boolean isProtected = true;
                for (NodeType nodeType : nodeTypes) {
                    if (nodeType.canSetProperty(prop.getName(), prop.getValues())) {
                        isProtected = false;
                        break;
                    }
                }
                for (NodeType nodeType : nodeTypes) {
                    PropertyDefinition[] propDefs = nodeType.getPropertyDefinitions();
                    for (PropertyDefinition propDef : propDefs) {
                        if (propDef.getName().equals(prop.getName()) && propDef.isProtected())
                            isProtected = true;
                    }
                }
                if (!isProtected) {
                    target.setProperty(prop.getName(), prop.getValues(), prop.getType());
                }
            } else {
                boolean isProtected = true;
                for (NodeType nodeType : nodeTypes) {
                    if (nodeType.canSetProperty(prop.getName(), prop.getValue())) {
                        isProtected = false;
                        break;
                    }
                }
                for (NodeType nodeType : nodeTypes) {
                    PropertyDefinition[] propDefs = nodeType.getPropertyDefinitions();
                    for (PropertyDefinition propDef : propDefs) {
                        if (propDef.getName().equals(prop.getName()) && propDef.isProtected())
                            isProtected = true;
                    }
                }
                if (!isProtected) {
                    target.setProperty(prop.getName(), prop.getValue());
                }
            }
        }
    }

    private static Node getHandle(Node subject) throws RepositoryException {
        try {
            Node handle = subject.getParent();
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                handle = null;
            }
            return handle;
        } catch (ItemNotFoundException ex) {
            // subject is root, deliberately ignore this exception
        }
        return null;
    }

    private static void clear(Node node) throws RepositoryException {
        Set<String> immune = new TreeSet<String>();

        Node handle = getHandle(node);
        if (handle != null && handle.isNodeType(HippoNodeType.NT_HANDLE)
                && handle.hasProperty(HippoNodeType.HIPPO_DISCRIMINATOR)) {
            Value[] discriminators = handle.getProperty(HippoNodeType.HIPPO_DISCRIMINATOR).getValues();
            for (Value discriminator : discriminators) {
                String key = discriminator.getString();
                if (node.hasProperty(key)) {
                    immune.add(key);
                }
            }
        } else if (node.hasProperty("hippostd:state")) {
            // backwards compatibility
            immune.add("hippostd:state");
        }
        immune.add(HippoNodeType.HIPPO_RELATED);
        immune.add(HippoNodeType.HIPPO_COMPUTE);
        immune.add(HippoNodeType.HIPPO_PATHS);
        for (NodeIterator childIter = node.getNodes(); childIter.hasNext();) {
            Node child = childIter.nextNode();
            if (child != null) {
                if (child.getDefinition().isAutoCreated()) {
                    clear(child);
                } else {
                    child.remove();
                }
            }
        }

        for (PropertyIterator propIter = node.getProperties(); propIter.hasNext();) {
            Property property = propIter.nextProperty();
            if (property.getName().startsWith("jcr:") || property.getDefinition().isProtected()
                    || immune.contains(property.getName())) {
                continue;
            }
            property.remove();
        }
    }

    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private Node subject;
    private Version version;

    public DeprecatedVersionWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException,
            RepositoryException {
        if (subject.isNodeType("nt:frozenNode")) {
            this.subject = findSubject(rootSession.getRootNode().getNode(subject.getPath().substring(1)));
            this.version = (Version) rootSession.getRootNode().getNode(subject.getParent().getPath().substring(1));
        } else {
            this.subject = rootSession.getRootNode().getNode(subject.getPath().substring(1));
        }
    }

    public Map<String, Serializable> hints() {
        return new TreeMap<String, Serializable>();
    }

    private Version lookup(Calendar historic, boolean returnHandle) throws WorkflowException, RepositoryException {
        Node handle = getHandle(subject);
        if (handle == null) {
            VersionHistory versionHistory = subject.getVersionHistory();
            Map<String, String> criteria = getCriteria(subject, null);
            for (VersionIterator iter = versionHistory.getAllVersions(); iter.hasNext();) {
                Version version = iter.nextVersion();
                if (version.getCreated().equals(historic) && matches(version.getNode("jcr:frozenNode"), criteria)) {
                    return version;
                }
            }
        } else {
            Map<String, String> criteria = getCriteria(subject, handle);
            return lookup(historic, returnHandle, criteria);
        }
        return null;
    }

    private Version lookup(Calendar historic, boolean returnHandle, Map<String, String> criteria)
            throws WorkflowException, RepositoryException {
        Node handle = getHandle(subject);
        VersionHistory handleHistory = handle.getVersionHistory();
        for (VersionIterator iter = handleHistory.getAllVersions(); iter.hasNext();) {
            Version handleVersion = iter.nextVersion();
            if (!handleVersion.getName().equals("jcr:rootVersion")) {
                for (NodeIterator children = handleVersion.getNode("jcr:frozenNode").getNodes(); children.hasNext();) {
                    Node child = children.nextNode();
                    if (child.isNodeType("nt:versionedChild")) {
                        String ref = child.getProperty("jcr:childVersionHistory").getString();
                        VersionHistory variantHistory = (VersionHistory) child.getSession().getNodeByUUID(ref);
                        for (VersionIterator childIter = variantHistory.getAllVersions(); childIter.hasNext();) {
                            Version version = childIter.nextVersion();
                            if (!version.getName().equals("jcr:rootVersion")) {
                                if (version.getCreated().equals(historic)) {
                                    if (matches(version.getNode("jcr:frozenNode"), criteria)) {
                                        if (returnHandle)
                                            return handleVersion;
                                        else
                                            return version;
                                    } else {
                                        return null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public Document version() throws WorkflowException, RepositoryException {
        Document result;

        Node handle = getHandle(subject);
        result = new Document(subject.checkin());
        if (handle != null) {
            // FIXME ought to be no check on handle being versionable
            if (handle.isNodeType("mix:versionable")) {
                handle.checkin();
            }
        } else {
            result = new Document(subject.checkin());
        }
        return result;
    }

    public Document revert(Calendar historic) throws WorkflowException, RepositoryException {
        Version version = lookup(historic, false);
        if (version == null)
            throw new WorkflowException("No such historic version");
        try {
            subject.restore(version, true);
            return new Document(subject);
        } catch (VersionException ex) {
            Node handle = subject.getParent();
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                throw new WorkflowException("version never existed");
            }

            Map<String, Value> requiredProperties = new TreeMap<String, Value>();
            if (handle.hasProperty(HippoNodeType.HIPPO_DISCRIMINATOR)) {
                Value[] discriminators = handle.getProperty(HippoNodeType.HIPPO_DISCRIMINATOR).getValues();
                for (Value discriminator : discriminators) {
                    String key = discriminator.getString();
                    if (subject.hasProperty(key)) {
                        requiredProperties.put(key, subject.getProperty(key).getValue());
                    }
                }
            }

            version = lookup(historic, true);
            handle.restore(version, false);

            for (NodeIterator iter = handle.getNodes(); iter.hasNext();) {
                Node result = iter.nextNode();
                if (result.getName().equals(handle.getName())) {
                    try {
                        for (Map.Entry<String, Value> required : requiredProperties.entrySet()) {
                            if (!result.getProperty(required.getKey()).getString().equals(
                                    required.getValue().getString())) {
                                result = null;
                                break;
                            }
                        }
                        if (result != null) {
                            return new Document(result);
                        }
                    } catch (RepositoryException e) {
                    }
                }
            }

            return null;
        }
    }

    public Document restoreTo(Document targetDocument) throws WorkflowException, RepositoryException {
        if (version == null) {
            throw new WorkflowException("No version available to restore");
        }

        Node target = subject.getSession().getNodeByUUID(targetDocument.getIdentity());
        Node handle = getHandle(target);
        if (handle != null) {
            handle.checkin();
        }
        clear(target);
        restore(target, version.getNode("jcr:frozenNode"));
        target.save();
        return targetDocument;
    }

    public Document restore(Calendar historic) throws WorkflowException, RepositoryException {
        return restore(historic, null);
    }

    public Document restore(Calendar historic, Map<String, String[]> providedReplacements) throws WorkflowException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public SortedMap<Calendar, Set<String>> list() throws WorkflowException, RepositoryException {
        Node handle = getHandle(subject);
        if (handle == null) {
            SortedMap<Calendar, Set<String>> listing = new TreeMap<Calendar, Set<String>>();
            VersionHistory versionHistory = subject.getVersionHistory();
            for (VersionIterator iter = versionHistory.getAllVersions(); iter.hasNext();) {
                Version version = iter.nextVersion();
                Set<String> labelsSet = new TreeSet<String>();
                String[] labels = versionHistory.getVersionLabels();
                for (String label : labels) {
                    labelsSet.add(label);
                }
                labelsSet.add(version.getName());
                listing.put(version.getCreated(), labelsSet);
            }
            return listing;
        } else {
            boolean placeholder = true;
            Calendar previous = null;
            SortedMap<Calendar, Set<String>> listing = new TreeMap<Calendar, Set<String>>();
            Map<String, String> criteria = getCriteria(subject, handle);
            VersionHistory handleHistory = handle.getVersionHistory();
            for (VersionIterator iter = handleHistory.getAllVersions(); iter.hasNext();) {
                Version handleVersion = iter.nextVersion();
                if (!handleVersion.getName().equals("jcr:rootVersion")) {
                    for (NodeIterator children = handleVersion.getNode("jcr:frozenNode").getNodes(); children.hasNext();) {
                        Node child = children.nextNode();
                        if (child.isNodeType("nt:versionedChild")) {
                            String ref = child.getProperty("jcr:childVersionHistory").getString();
                            VersionHistory variantHistory = (VersionHistory) child.getSession().getNodeByUUID(ref);
                            Set<String> labelsSet = new TreeSet<String>();
                            String[] labels = variantHistory.getVersionLabels();
                            for (String label : labels) {
                                labelsSet.add(label);
                            }
                            for (VersionIterator childIter = variantHistory.getAllVersions(); childIter.hasNext();) {
                                Version version = childIter.nextVersion();
                                if (!version.getName().equals("jcr:rootVersion")) {
                                    if (matches(version.getNode("jcr:frozenNode"), criteria)) {
                                        if (previous == null || !previous.equals(version.getCreated())) {
                                            listing.put(version.getCreated(), labelsSet);
                                            placeholder = false;
                                            previous = version.getCreated();
                                        }
                                    } else if (!placeholder) {
                                        placeholder = true;
                                        listing.put(version.getCreated(), labelsSet);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return listing;
        }
    }

    public Document retrieve(Calendar historic) throws WorkflowException, RepositoryException {
        Version version = lookup(historic, false);
        return (version == null ? null : new Document(version.getNode("jcr:frozenNode")));
    }
}
