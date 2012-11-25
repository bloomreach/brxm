/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.translation.impl;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.standardworkflow.CopyWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.translation.TranslationWorkflow;

public class TranslationWorkflowImpl implements TranslationWorkflow, InternalWorkflow {

    private static final long serialVersionUID = 1L;

    private final Session userSession;
    private final Session rootSession;
    private final WorkflowContext workflowContext;
    private final Node rootSubject;
    private final Node userSubject;

    public TranslationWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
            throws RemoteException, RepositoryException {
        this.workflowContext = context;
        this.userSession = userSession;
        this.rootSession = rootSession;
        this.userSubject = this.userSession.getNodeByUUID(subject.getUUID());
        this.rootSubject = rootSession.getNodeByUUID(subject.getUUID());

        if (!userSubject.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            throw new RepositoryException("Node is not of type " + HippoTranslationNodeType.NT_TRANSLATED);
        }
    }

    public Document addTranslation(String language, String name) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {
        Node translations = rootSubject.getNode(HippoTranslationNodeType.TRANSLATIONS);
        if (translations.hasNode(language)) {
            throw new WorkflowException("Translation already exists");
        }

        Node lclContainingFolder = getContainingFolder();
        if (!lclContainingFolder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            throw new WorkflowException("No translated ancestor found");
        }

        // find target
        Node folderTranslation;
        // TODO replace below with search
        Node folderTranslations = lclContainingFolder.getNode(HippoTranslationNodeType.TRANSLATIONS);
        if (!folderTranslations.hasNode(language)) {
            throw new WorkflowException("Folder was not translated to " + language);
        }
        folderTranslation = ((HippoNode) folderTranslations.getNode(language)).getCanonicalNode();
        if (folderTranslation == null) {
            throw new WorkflowException("Could not find canonical equivalent of translated folder");
        }
        Document targetFolder = new Document(folderTranslation.getUUID());
        Node copiedDoc = null;
        if (userSubject.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            Workflow defaultWorkflow = workflowContext.getWorkflowContext(null).getWorkflow("translation-copy",
                    new Document(rootSubject.getUUID()));
            if (defaultWorkflow instanceof CopyWorkflow) {
                ((CopyWorkflow) defaultWorkflow).copy(targetFolder, name);
            } else {
                throw new WorkflowException("Unknown default workflow; cannot copy document");
            }
            NodeIterator siblings = folderTranslation.getNodes(name);
            while (siblings.hasNext()) {
                Node sibling = siblings.nextNode();
                if (sibling.isNodeType(HippoNodeType.NT_HANDLE)) {
                    copiedDoc = sibling;
                }
            }
            if (copiedDoc == null) {
                throw new WorkflowException("Could not locate handle for document after copying");
            }
            copiedDoc = copiedDoc.getNode(copiedDoc.getName());
            if (!copiedDoc.isCheckedOut()) {
                copiedDoc.checkout();
            }
        } else {
            Workflow internalWorkflow = workflowContext.getWorkflowContext(null).getWorkflow("internal", targetFolder);
            if (!(internalWorkflow instanceof FolderWorkflow)) {
                throw new WorkflowException("Target folder does not have a folder workflow in the category 'internal'.");
            }
            Map<String, Set<String>> prototypes = (Map<String, Set<String>>) internalWorkflow.hints().get("prototypes");
            if (prototypes == null) {
                throw new WorkflowException("No prototype hints available in workflow of target folder.");
            }

            // find best matching category and type from prototypes
            String primaryType = userSubject.getPrimaryNodeType().getName();
            String category = null;
            String type = null;
            for (Map.Entry<String, Set<String>> candidate : prototypes.entrySet()) {
                String categoryName = candidate.getKey();
                Set<String> types = candidate.getValue();
                if (types.contains(primaryType)) {
                    category = categoryName;
                    type = primaryType;
                    break;
                }
                if (category == null) {
                    category = categoryName;
                }
                if (type == null && types.size() > 0) {
                    type = types.iterator().next();
                }
            }

            if (category != null && type != null) {
                String path = ((FolderWorkflow) internalWorkflow).add(category, type, name);
                copiedDoc = rootSession.getNode(path);
            } else {
                throw new WorkflowException("No category found to use for adding translation to target folder");
            }
            if (!copiedDoc.isCheckedOut()) {
                copiedDoc.checkout();
            }
            if (!copiedDoc.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                copiedDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
            }
            copiedDoc.setProperty(HippoTranslationNodeType.ID, userSubject.getProperty(HippoTranslationNodeType.ID)
                    .getString());
        }

        copiedDoc.setProperty(HippoTranslationNodeType.LOCALE, language);
        Document copy = new Document(copiedDoc.getUUID());

        rootSession.save();
        rootSession.refresh(false);
        return copy;
    }

    public void addTranslation(String language, Document document) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {
        if (userSubject.getNode(HippoTranslationNodeType.TRANSLATIONS).hasNode(language)) {
            throw new WorkflowException("Language already exists");
        }

        Node copiedDocNode = rootSession.getNodeByUUID(document.getIdentity());
        if (!copiedDocNode.isCheckedOut()) {
            copiedDocNode.checkout();
        }
        if (!copiedDocNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            copiedDocNode.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        }
        copiedDocNode.setProperty(HippoTranslationNodeType.LOCALE, language);
        copiedDocNode.setProperty(HippoTranslationNodeType.ID, userSubject.getProperty(HippoTranslationNodeType.ID)
                .getString());

        rootSession.save();
        rootSession.refresh(false);
    }

    public Map<String, Serializable> hints() throws WorkflowException, RemoteException, RepositoryException {
        Map<String, Serializable> hints = new TreeMap<String, Serializable>();

        if (rootSubject.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
            String state = rootSubject.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
            if ("draft".equals(state)) {
                hints.put("addTranslation", Boolean.FALSE);
            } else {
                NodeIterator siblings = rootSubject.getParent().getNodes(rootSubject.getName());
                Node unpublished = null;
                Node published = null;
                while (siblings.hasNext()) {
                    Node sibling = siblings.nextNode();
                    if (sibling.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
                        String siblingState = sibling.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                        if ("unpublished".equals(siblingState)) {
                            unpublished = sibling;
                        } else if ("published".equals(siblingState)) {
                            published = sibling;
                        }
                    }
                }
                if (unpublished != null && published != null) {
                    if ("published".equals(state)) {
                        hints.put("addTranslation", Boolean.FALSE);
                    }
                }
            }
        }

        Set<String> translations = new TreeSet<String>();
        try {
            if (rootSubject.hasProperty(HippoTranslationNodeType.ID)) {
                translations = getTranslations(rootSubject);
            }
        } catch (RepositoryException ex) {
            throw new WorkflowException("Exception during searching for available translations", ex);
        }
        
        hints.put("locales", (Serializable) translations);

        Set<String> available = new TreeSet<String>();
        // for all the available translations we pick the highest ancestor of rootSubject of type HippoTranslationNodeType.NT_TRANSLATED,
        // and take all the translations for that node
        Node highestTranslatedNode = getFarthestTranslatedAncestor(rootSubject);
        if (highestTranslatedNode != null) {
            try {
                if (highestTranslatedNode.hasProperty(HippoTranslationNodeType.ID)) {
                    available = getTranslations(highestTranslatedNode);
                }
            } catch (RepositoryException ex) {
                throw new WorkflowException("Exception during searching for available translations", ex);
            }
        }

        hints.put("available", (Serializable) available);
        hints.put("locale", getLocale(userSubject));
        return hints;
    }


    private Set<String> getTranslations(final Node translatedNode) throws RepositoryException {
        final Set<String> available = new TreeSet<String>();
        String id = translatedNode.getProperty(HippoTranslationNodeType.ID).getString();
        Query query = translatedNode.getSession().getWorkspace().getQueryManager().createQuery(
                "SELECT " + HippoTranslationNodeType.LOCALE
                        + " FROM " + HippoTranslationNodeType.NT_TRANSLATED
                        + " WHERE " + HippoTranslationNodeType.ID + "='" + id + "'",
                Query.SQL);
        final QueryResult result = query.execute();
        final RowIterator rowIterator = result.getRows();
        while (rowIterator.hasNext()) {
            final Row row = rowIterator.nextRow();
            final Value value = row.getValue(HippoTranslationNodeType.LOCALE);
            available.add(value.getString());
        }
        return available;
    }

    /**
     * returns farthest ancestor from rootSubject of type HippoTranslationNodeType.NT_TRANSLATED and returns null if all ancestors and rootSubject
     * are not of type HippoTranslationNodeType.NT_TRANSLATED
     *
     */
    private Node getFarthestTranslatedAncestor(final Node rootSubject) throws RepositoryException {
        Node jcrRoot = rootSubject.getSession().getRootNode();
        Node current = rootSubject;
        Node highestAncestorOfTypeTranslated = null;
        while (!current.isSame(jcrRoot)) {
            if (current.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                highestAncestorOfTypeTranslated = current;
            }
            current = current.getParent();
        }
        return highestAncestorOfTypeTranslated;
    }


    private String getLocale(Node node) throws RepositoryException {
        while (node.getDepth() != 0) {
            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                if (node.hasProperty(HippoTranslationNodeType.LOCALE)) {
                    return node.getProperty(HippoTranslationNodeType.LOCALE).getString();
                }
            }
            node = node.getParent();
        }
        return null;
    }

    private Node getContainingFolder() throws ItemNotFoundException, AccessDeniedException, RepositoryException,
            WorkflowException {
        Node parent = rootSubject.getParent();
        if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
            parent = parent.getParent();
        }
        while (!parent.isSame(rootSession.getRootNode())) {
            if (parent.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

}
