/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
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
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.util.DefaultCopyHandler;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropInfo;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.util.JcrConstants;

public class VersionWorkflowImpl extends Document implements VersionWorkflow, InternalWorkflow {

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
        JcrUtils.copyToChain(source, new DefaultCopyHandler(target) {
            @Override
            public void setProperty(final PropInfo prop) throws RepositoryException {
                final String name = prop.getName();
                if (name.startsWith("jcr:frozen") || name.startsWith("jcr:uuid") ||
                        name.equals(HippoNodeType.HIPPO_RELATED) ||
                        name.equals(HippoNodeType.HIPPO_COMPUTE) ||
                        name.equals(HippoNodeType.HIPPO_PATHS) ||
                        name.equals(HippoStdNodeType.HIPPOSTD_STATE)) {
                    return;
                }
                super.setProperty(prop);
            }
        });
    }

    private static Node getVersionableHandle(Node subject) throws RepositoryException {
        try {
            Node handle = subject.getParent();
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE) || !handle.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
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

        Node handle = getVersionableHandle(node);
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

    public VersionWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException,
            RepositoryException {
        if (subject.isNodeType("nt:frozenNode")) {
            this.subject = rootSession.getNodeByIdentifier(subject.getProperty("jcr:frozenUuid").getString());
            this.version = (Version) rootSession.getNode(subject.getParent().getPath());
        } else {
            this.subject = rootSession.getNode(subject.getPath());
        }
    }

    public Map<String, Serializable> hints() {
        return new TreeMap<>();
    }

    private Version lookup(Calendar historic, boolean returnHandle) throws WorkflowException, RepositoryException {
        Node handle = getVersionableHandle(subject);
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
        Node handle = getVersionableHandle(subject);
        VersionHistory handleHistory = handle.getVersionHistory();
        for (VersionIterator iter = handleHistory.getAllVersions(); iter.hasNext(); ) {
            Version handleVersion = iter.nextVersion();
            if (!handleVersion.getName().equals("jcr:rootVersion")) {
                for (NodeIterator children = handleVersion.getNode("jcr:frozenNode").getNodes(); children.hasNext(); ) {
                    Node child = children.nextNode();
                    if (child.isNodeType("nt:versionedChild")) {
                        String ref = child.getProperty("jcr:childVersionHistory").getString();
                        VersionHistory variantHistory = (VersionHistory) child.getSession().getNodeByUUID(ref);
                        for (VersionIterator childIter = variantHistory.getAllVersions(); childIter.hasNext(); ) {
                            Version version = childIter.nextVersion();
                            if (!version.getName().equals("jcr:rootVersion")) {
                                if (version.getCreated().equals(historic)) {
                                    if (matches(version.getNode("jcr:frozenNode"), criteria)) {
                                        if (returnHandle) {
                                            return handleVersion;
                                        } else {
                                            return version;
                                        }
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

        Node handle = getVersionableHandle(subject);
        result = new Document(subject.checkin());

        if (handle != null) {
            handle.checkin();
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

        Node target = subject.getSession().getNodeByIdentifier(targetDocument.getIdentity());
        Node handle = getVersionableHandle(target);
        if (handle != null) {
            handle.checkin();
        }
        clear(target);
        restore(target, version.getNode("jcr:frozenNode"));
        target.save();
        return targetDocument;
    }

    public Document restore(Calendar historic) throws WorkflowException, RepositoryException {
        Version version = lookup(historic, false);
        if (version == null)
            throw new WorkflowException("No such historic version");
        Node handle = subject.getParent();
        if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            throw new WorkflowException("version never existed");
        }

        JcrUtils.ensureIsCheckedOut(subject);
        for (Property property : new PropertyIterable(subject.getProperties())) {
            if (property.getDefinition().isProtected()) {
                continue;
            }
            property.remove();
        }
        for (Node node : new NodeIterable(subject.getNodes())) {
            if (node.getDefinition().isProtected()) {
                continue;
            }
            node.remove();
        }
        Node frozenNode = version.getFrozenNode();
        NodeType[] frozenMixins = JcrUtils.getMixinNodeTypes(frozenNode);
        for (NodeType mixin : subject.getMixinNodeTypes()) {
            boolean found = false;
            for (NodeType frozenMixin : frozenMixins) {
                if (frozenMixin.getName().equals(mixin.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                subject.removeMixin(mixin.getName());
            }
        }

        JcrUtils.copyTo(frozenNode, subject);

        final Session session = subject.getSession();
        session.save();
        session.getWorkspace().getVersionManager().checkpoint(subject.getPath());

        return new Document(subject);
    }

    public Document restore(Calendar historic, Map <String, String[]> providedReplacements) throws WorkflowException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public SortedMap<Calendar, Set<String>> list() throws WorkflowException, RepositoryException {
        if (subject.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            final SortedMap<Calendar, Set<String>> listing = new TreeMap<>();
            VersionHistory versionHistory = subject.getVersionHistory();

            for (VersionIterator iter = versionHistory.getAllVersions(); iter.hasNext();) {
                Version version = iter.nextVersion();
                if (version.getName().equals("jcr:rootVersion")) {
                    continue;
                }
                Set<String> labelsSet = new TreeSet<String>();
                String[] labels = versionHistory.getVersionLabels();
                Collections.addAll(labelsSet, labels);
                labelsSet.add(version.getName());
                listing.put(version.getCreated(), labelsSet);
            }
            return listing;
        }
        return new TreeMap<>();
    }

    public Document retrieve(Calendar historic) throws WorkflowException, RepositoryException {
        Version version = lookup(historic, false);
        return (version == null ? null : new Document(version.getNode("jcr:frozenNode")));
    }
}
