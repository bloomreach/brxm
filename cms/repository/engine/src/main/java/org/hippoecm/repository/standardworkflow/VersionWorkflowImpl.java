/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.standardworkflow;

import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;

import java.rmi.RemoteException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;

public class VersionWorkflowImpl extends Document implements VersionWorkflow {

    private static final long serialVersionUID = 1L;
    private Session userSession;
    private Node subject;

    public VersionWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.subject = subject;
        this.userSession = userSession;
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
                }
            } catch (ItemNotFoundException ex) {
                return true;
            }
            if (node.hasProperty("hippostd:state")) {
                if (!node.getProperty("hippostd:state").getString().equals("published")) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
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

    public Document revert() throws WorkflowException, RepositoryException {
        Node primary = subject.getSession().getNodeByUUID(((Version) subject).getContainingHistory().getVersionableUUID());
        // FIXME primary may have been deleted, should go through handle
        primary.restore((Version) subject, true);
        return new Document(primary.getUUID());
    }

    public Map<Calendar, Set<String>> list() throws WorkflowException, RepositoryException {
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
            Map<Calendar, Set<String>> listing = new TreeMap<Calendar, Set<String>>();
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
            Map<Calendar, Set<String>> listing = new TreeMap<Calendar, Set<String>>();
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
                                    if(isSimilar(version) && (previous == null || !previous.equals(version.getCreated()))) {
                                        listing.put(version.getCreated(), labelsSet);
                                        placeholder = false;
                                        previous = version.getCreated();
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
                    return new Document(version.getNode("jcr:frozenNode").getUUID());
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
                                        Node variant = version.getNode("jcr:frozenNode");
                                        if (variant.getProperty("hippostd:state").getString().equals("published")) {
                                            return new Document(variant.getUUID());
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
}
