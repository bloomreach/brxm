/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.standardworkflow.CopyWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslatedNode;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationWorkflowImpl implements TranslationWorkflow, InternalWorkflow {

    private static final Logger log = LoggerFactory.getLogger(TranslationWorkflowImpl.class);
    private final Session rootSession;
    private final WorkflowContext workflowContext;
    private final Node rootSubject;
    private final Node userSubject;

    public TranslationWorkflowImpl(final WorkflowContext context, final Session userSession, final Session rootSession,
                                   final Node subject) throws RepositoryException {
        this.workflowContext = context;
        this.rootSession = rootSession;
        this.userSubject = userSession.getNodeByIdentifier(subject.getIdentifier());
        this.rootSubject = rootSession.getNodeByIdentifier(subject.getIdentifier());

        if (!userSubject.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            throw new RepositoryException("Node is not of type " + HippoTranslationNodeType.NT_TRANSLATED);
        }
    }

    public Document addTranslation(final String language, final String newDocumentName) throws WorkflowException,
            RepositoryException, RemoteException {

        final HippoTranslatedNode originNode = new HippoTranslatedNode(rootSubject);
        final Node originFolder = originNode.getContainingFolder();
        if (originFolder == null || !originFolder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            throw new WorkflowException("No translated ancestor folder found");
        }

        final HippoTranslatedNode originTranslatedFolderNode = new HippoTranslatedNode(originFolder);
        final Node targetFolderNode = originTranslatedFolderNode.getTranslation(language);

        Node copiedNode;
        if (userSubject.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            copiedNode = addTranslatedDocument(language, newDocumentName, targetFolderNode);
            toEditmode(copiedNode);
        } else {
            copiedNode = addTranslatedFolder(language, newDocumentName, targetFolderNode);
        }

        rootSession.save();
        rootSession.refresh(false);
        return new Document(copiedNode);
    }

    private Node addTranslatedDocument(final String language, final String newDocumentName, final Node targetFolderNode)
            throws WorkflowException, RepositoryException, RemoteException {

        getOriginsCopyWorkflow().copy(new Document(targetFolderNode), newDocumentName);

        Node newDocumentHandle = null;

        final NodeIterator siblings = targetFolderNode.getNodes(newDocumentName);
        while (siblings.hasNext()) {
            final Node sibling = siblings.nextNode();
            if (sibling.isNodeType(HippoNodeType.NT_HANDLE)) {
                newDocumentHandle = sibling;
            }
        }
        if (newDocumentHandle == null) {
            throw new WorkflowException("Could not locate handle for document after copying");
        }

        final NodeIterator copiedVariants = newDocumentHandle.getNodes(newDocumentHandle.getName());
        while (copiedVariants.hasNext()) {
            final Node copiedVariant = copiedVariants.nextNode();
            JcrUtils.ensureIsCheckedOut(copiedVariant);
            copiedVariant.setProperty(HippoTranslationNodeType.LOCALE, language);
        }

        return newDocumentHandle;
    }

    private CopyWorkflow getOriginsCopyWorkflow() throws RepositoryException, WorkflowException {
        // first check if a copy workflow is configured on the handle itself
        Workflow workflow = workflowContext.getWorkflow("translation-copy", new Document(rootSubject.getParent()));

        if (workflow == null) {
            // No? Fallback to a copy workflow on the subject itself
            workflow = workflowContext.getWorkflow("translation-copy", new Document(rootSubject));
        }

        if (workflow instanceof CopyWorkflow) {
            return (CopyWorkflow) workflow;
        } else {
            throw new WorkflowException("No copy workflow defined; cannot copy document");
        }
    }

    private Node addTranslatedFolder(final String language, final String newDocumentName, final Node targetFolderNode)
            throws WorkflowException, RemoteException, RepositoryException {

        final FolderWorkflow workflowOfTargetFolder = getFolderWorkflow(targetFolderNode);
        final Map<String, Set<String>> prototypes = getPrototypes(workflowOfTargetFolder);

        // find best matching category and type from prototypes
        final String primaryType = userSubject.getPrimaryNodeType().getName();
        String category = null;
        String type = null;

        for (Map.Entry<String, Set<String>> candidate : prototypes.entrySet()) {
            final String categoryName = candidate.getKey();
            final Set<String> types = candidate.getValue();
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

        if (category == null) {
            throw new WorkflowException("No category found to use for adding translation to target folder");
        }
        if (type == null) {
            throw new WorkflowException("No type found to use for adding translation to target folder");
        }

        final String newFolderPath = workflowOfTargetFolder.add(category, type, newDocumentName);
        Node copiedNode = rootSession.getNode(newFolderPath);

        JcrUtils.ensureIsCheckedOut(copiedNode);
        if (!copiedNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            copiedNode.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        }
        copiedNode.setProperty(HippoTranslationNodeType.ID,
                userSubject.getProperty(HippoTranslationNodeType.ID).getString());
        copiedNode.setProperty(HippoTranslationNodeType.LOCALE, language);
        rootSession.save();
        
        copyFolderTypes(copiedNode, prototypes);

        return copiedNode;
    }

    private Map<String, Set<String>> getPrototypes(final FolderWorkflow folderWorkflow) throws WorkflowException,
            RemoteException, RepositoryException {

        final Map<String, Set<String>> prototypes = (Map<String, Set<String>>) folderWorkflow.hints().get("prototypes");
        if (prototypes == null) {
            throw new WorkflowException("No prototype hints available in workflow of target folder.");
        }
        return prototypes;
    }

    private FolderWorkflow getFolderWorkflow(final Node targetFolderNode) throws WorkflowException,
            RepositoryException {

        Workflow workflow = workflowContext.getWorkflow("internal", new Document(targetFolderNode));
        if (!(workflow instanceof FolderWorkflow)) {
            throw new WorkflowException("Target folder does not have a folder workflow in the category 'internal'.");
        }
        return (FolderWorkflow) workflow;
    }

    public void addTranslation(final String language, final Document document) throws WorkflowException,
            RepositoryException {
        HippoTranslatedNode translatedNode = new HippoTranslatedNode(rootSubject);
        if (translatedNode.hasTranslation(language)) {
            throw new WorkflowException("Language already exists");
        }

        Node copiedDocNode = document.getNode(rootSession);
        JcrUtils.ensureIsCheckedOut(copiedDocNode);
        if (!copiedDocNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            copiedDocNode.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
        }
        copiedDocNode.setProperty(HippoTranslationNodeType.LOCALE, language);
        copiedDocNode.setProperty(HippoTranslationNodeType.ID, userSubject.getProperty(HippoTranslationNodeType.ID)
                .getString());

        rootSession.save();
        rootSession.refresh(false);
    }

    public Map<String, Serializable> hints() throws WorkflowException, RepositoryException {
        Map<String, Serializable> hints = new TreeMap<>();

        if (userSubject.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
            String state = userSubject.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
            if (HippoStdNodeType.DRAFT.equals(state)) {
                hints.put("addTranslation", Boolean.FALSE);
            } else {
                NodeIterator siblings = userSubject.getParent().getNodes(userSubject.getName());
                Node unpublished = null;
                Node published = null;
                while (siblings.hasNext()) {
                    Node sibling = siblings.nextNode();
                    if (sibling.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
                        String siblingState = sibling.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                        if (HippoStdNodeType.UNPUBLISHED.equals(siblingState)) {
                            unpublished = sibling;
                        } else if (HippoStdNodeType.PUBLISHED.equals(siblingState)) {
                            published = sibling;
                        }
                    }
                }
                if (unpublished != null && published != null) {
                    if (HippoStdNodeType.PUBLISHED.equals(state)) {
                        hints.put("addTranslation", Boolean.FALSE);
                    }
                }
            }
        }

        final HippoTranslatedNode translatedNode = new HippoTranslatedNode(userSubject);
        Set<String> translations;
        try {
            translations = translatedNode.getTranslations();
        } catch (RepositoryException ex) {
            throw new WorkflowException("Exception during searching for available translations", ex);
        }

        hints.put("locales", (Serializable) translations);

        Set<String> available = new TreeSet<>();
        // for all the available translations we pick the highest ancestor of userSubject of type 
        // HippoTranslationNodeType.NT_TRANSLATED, and take all the translations for that node
        Node highestTranslatedNode = translatedNode.getFarthestTranslatedAncestor();
        if (highestTranslatedNode != null) {
            try {
                available = new HippoTranslatedNode(highestTranslatedNode).getTranslations();
            } catch (RepositoryException ex) {
                throw new WorkflowException("Exception during searching for available translations", ex);
            }
        }

        hints.put("available", (Serializable) available);
        hints.put("locale", translatedNode.getLocale());
        return hints;
    }

    /**
     * Bring the new document to edit mode for the current user.
     */
    private void toEditmode(final Node newNode) throws WorkflowException, RepositoryException, RemoteException {
        final Workflow editing = workflowContext.getWorkflow("editing", new Document(newNode));
        if (editing instanceof EditableWorkflow) {
            EditableWorkflow editableWorkflow = (EditableWorkflow) editing;
            editableWorkflow.obtainEditableInstance();
        }
    }

    private void copyFolderTypes(final Node copiedNode, final Map<String, Set<String>> prototypes)
            throws RepositoryException {

        try {
            // check if we have all subject folder types
            final FolderWorkflow folderWorkflow = getFolderWorkflow(rootSubject);
            final Map<String, Set<String>> copyPrototypes = getPrototypes(folderWorkflow);

            if (copyPrototypes != null && copyPrototypes.size() > 0) {
                // got some stuff...check if equal:
                final Set<String> protoKeys = prototypes.keySet();
                final Set<String> copyKeys = copyPrototypes.keySet();
                // check if we have a difference and overwrite
                if (copyKeys.size() != protoKeys.size() || !copyKeys.containsAll(protoKeys)) {
                    final String[] newValues = copyKeys.toArray(new String[0]);
                    copiedNode.setProperty(HippoStdNodeType.HIPPOSTD_FOLDERTYPE, newValues);
                }
            }
        } catch (WorkflowException e) {
            log.warn(e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (RemoteException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }
}
