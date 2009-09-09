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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;

public class VersionWorkflowImpl extends Document implements VersionWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private Session userSession;
    private Node subject;

    public VersionWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        if (subject.isNodeType("nt:frozenNode")) {
            this.subject = userSession.getNodeByUUID(subject.getProperty("jcr:frozenUuid").getString());
        } else {
            this.subject = subject;
        }
        this.userSession = userSession;
    }

    public Map<String,Serializable> hints() {
        return new TreeMap<String,Serializable>();
    }

    private boolean isSimilar(Node node) throws RepositoryException {
        if (node.hasNode("jcr:frozenNode")) {
            node = node.getNode("jcr:frozenNode");
            try {
                Node handle = subject.getParent();
                if (handle.hasProperty(HippoNodeType.HIPPO_DISCRIMINATOR)) {
                    Value[] discriminators = handle.getProperty(HippoNodeType.HIPPO_DISCRIMINATOR).getValues();
                    for (int i = 0; i < discriminators.length; i++) {
                        String key = discriminators[i].getString();
                        if (subject.hasProperty(key)) {
                            if (!node.hasProperty(key) ||
                                !node.getProperty(key).getString().equals(subject.getProperty(key).getString())) {
                                return false;
                            }
                        }
                    }
                } else if(subject.isNodeType("hippostd:publishable")) {
                    String key = "hippostd:state";
                    if (subject.hasProperty(key)) {
                        if (!node.hasProperty(key) ||
                            !node.getProperty(key).getString().equals(subject.getProperty(key).getString())) {
                            return false;
                        }
                    }
                }
            } catch (ItemNotFoundException ex) {
                return true;
            }
        } else {
            return false;
        }
        return true;
    }

    private Version lookup(Calendar historic, boolean returnHandle) throws WorkflowException, RepositoryException {
        Node handle = null;
        try {
            handle = subject.getParent();
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                handle = null;
            }
        } catch (ItemNotFoundException ex) {
            // subject is root, deliberately ignore this exception
        }
        if (handle == null) {
            VersionHistory versionHistory = subject.getVersionHistory();
            for (VersionIterator iter = versionHistory.getAllVersions(); iter.hasNext();) {
                Version version = iter.nextVersion();
                if (version.getCreated().equals(historic) && isSimilar(version)) {
                    return version;
                }
            }
        } else {
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
                                        if(isSimilar(version)) {
                                            if(returnHandle)
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
        }
        return null;
    }

    public Document version() throws WorkflowException, RepositoryException {
        Document result;

        Node handle = null;
        try {
            handle = subject.getParent();
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                handle = null;
            }
        } catch (ItemNotFoundException ex) {
            // subject is root, deliberately ignore this exception
        }
        result = new Document(subject.checkin().getUUID());
        if (handle != null) {
            // FIXME ought to be no check on handle being versionable
            if (handle.isNodeType("mix:versionable")) {
                handle.checkin();
            }
        } else {
            result = new Document(subject.checkin().getUUID());
        }
        return result;
    }

    public Document revert(Calendar historic) throws WorkflowException, RepositoryException {
        Version version = lookup(historic, false);
        if(version == null)
            throw new WorkflowException("No such historic version");
        try {
            subject.restore(version, true);
            return new Document(subject.getUUID());
        } catch(VersionException ex) {
            Node handle = subject.getParent();
            if(!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                throw new WorkflowException("version never existed");
            }

            Map<String,Value> requiredProperties = new TreeMap<String,Value>();
            if(handle.hasProperty(HippoNodeType.HIPPO_DISCRIMINATOR)) {
                Value[] discriminators = handle.getProperty(HippoNodeType.HIPPO_DISCRIMINATOR).getValues();
                for (int i = 0; i < discriminators.length; i++) {
                    String key = discriminators[i].getString();
                    if (subject.hasProperty(key)) {
                        requiredProperties.put(key, subject.getProperty(key).getValue());
                    }
                }
            }

            version = lookup(historic, true);
            handle.restore(version, false);

            for(NodeIterator iter = handle.getNodes(); iter.hasNext(); ) {
                Node result = iter.nextNode();
                if(result.getName().equals(handle.getName())) {
                    try {
                        for(Map.Entry<String,Value> required : requiredProperties.entrySet()) {
                            if(!result.getProperty(required.getKey()).getString().equals(required.getValue().getString())) {
                                result = null;
                                break;
                            }
                        }
                        if(result != null) {
                            return new Document(result.getUUID());
                        }
                    } catch(RepositoryException e) {
                    }
                }
            }

            return null;
        }
    }

    public Document restore(Calendar historic) throws WorkflowException, RepositoryException {
        return restore(historic, null);
    }

    public Document restore(Calendar historic, Map<String, String[]> providedReplacements)
            throws WorkflowException, RepositoryException {
        Map<String, String[]> replacements = new TreeMap<String, String[]>();
        if(providedReplacements != null) {
            replacements.putAll(providedReplacements);
        }
        replacements.put("./_name", new String[] { subject.getName() });
        replacements.put("./jcr:primaryType", new String[] { "${jcr:frozenPrimaryType}" });
        replacements.put("./jcr:mixinTypes", new String[] { "${jcr:frozenMixinTypes}" });

        Version version = lookup(historic, false);
        if(version == null)
            throw new WorkflowException("No such historic version");
        Node handle = subject.getParent();
        if(!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            throw new WorkflowException("version never existed");
        }
        for(NodeIterator iter = handle.getNodes(handle.getName()); iter.hasNext(); ) {
            Node child = iter.nextNode();
            if(child.getProperty("hippostd:state").getString().equals("unpublished"))
                child.remove();
        }
        handle.checkout();
        Node restoredDocument = FolderWorkflowImpl.copy(version.getNode("jcr:frozenNode"), handle, replacements, ".");
        restoredDocument.getParent().save();
        restoredDocument.checkin();
        return new Document(restoredDocument.getUUID());
    }

    public SortedMap<Calendar, Set<String>> list() throws WorkflowException, RepositoryException {
        Node handle = null;
        try {
            handle = subject.getParent();
            if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                handle = null;
            }
        } catch (ItemNotFoundException ex) {
            // subject is root, deliberately ignore this exception
        }
        if (handle == null) {
            SortedMap<Calendar, Set<String>> listing = new TreeMap<Calendar, Set<String>>();
            VersionHistory versionHistory = subject.getVersionHistory();
            for (VersionIterator iter = versionHistory.getAllVersions(); iter.hasNext();) {
                Version version = iter.nextVersion();
                Calendar timestamp = version.getCreated();
                Set<String> labelsSet = new TreeSet<String>();
                String[] labels = versionHistory.getVersionLabels();
                for (int i = 0; i < labels.length; i++) {
                    labelsSet.add(labels[i]);
                }
                labelsSet.add(version.getName());
                listing.put(version.getCreated(), labelsSet);
            }
            return listing;
        } else {
            boolean placeholder = true;
            Calendar previous = null;
            SortedMap<Calendar, Set<String>> listing = new TreeMap<Calendar, Set<String>>();
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
                            for (int i = 0; i < labels.length; i++) {
                                labelsSet.add(labels[i]);
                            }
                            for (VersionIterator childIter = variantHistory.getAllVersions(); childIter.hasNext();) {
                                Version version = childIter.nextVersion();
                                if (!version.getName().equals("jcr:rootVersion")) {
                                    if(isSimilar(version)) {
                                        if(previous == null || !previous.equals(version.getCreated())) {
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
        return (version == null ? null : new Document(version.getNode("jcr:frozenNode").getUUID()));
    }
}
