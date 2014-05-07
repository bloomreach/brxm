/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.standardworkflow.CopyWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslatedNode;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationWorkflowImpl implements TranslationWorkflow, InternalWorkflow {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(TranslationWorkflowImpl.class);
    private final Session rootSession;
    private final WorkflowContext workflowContext;
    private final Node rootSubject;
    private final Node userSubject;

    public TranslationWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
            throws RemoteException, RepositoryException {
        this.workflowContext = context;
        this.rootSession = rootSession;
        this.userSubject = userSession.getNodeByIdentifier(subject.getIdentifier());
        this.rootSubject = rootSession.getNodeByIdentifier(subject.getIdentifier());

        if (!userSubject.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            throw new RepositoryException("Node is not of type " + HippoTranslationNodeType.NT_TRANSLATED);
        }
    }

    public Document addTranslation(String language, String name) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {
        HippoTranslatedNode translatedNode = new HippoTranslatedNode(rootSubject);
        Node lclContainingFolder = translatedNode.getContainingFolder();
        if (!lclContainingFolder.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            throw new WorkflowException("No translated ancestor found");
        }

        HippoTranslatedNode translatedFolder = new HippoTranslatedNode(lclContainingFolder);
        Node folderTranslation = translatedFolder.getTranslation(language);
        Document targetFolder = new Document(folderTranslation);
        Node copiedDoc = null;
        if (userSubject.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            // first check if a copy workflow is configured on the handle itself
            Workflow copyWorkflow = workflowContext.getWorkflow("translation-copy", new Document(rootSubject.getParent()));
            if (copyWorkflow == null) {
                // No? Fallback to a copy workflow on the subject itself
                copyWorkflow = workflowContext.getWorkflow("translation-copy", new Document(rootSubject));
            }
            if (copyWorkflow instanceof CopyWorkflow) {
                ((CopyWorkflow) copyWorkflow).copy(targetFolder, name);
            } else {
                throw new WorkflowException("No copy workflow defined; cannot copy document");
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
            NodeIterator copiedVariants = copiedDoc.getNodes(copiedDoc.getName());
            while (copiedVariants.hasNext()) {
                Node copiedVariant = copiedVariants.nextNode();
                JcrUtils.ensureIsCheckedOut(copiedVariant);
                copiedVariant.setProperty(HippoTranslationNodeType.LOCALE, language);
            }
        } else {
            Workflow internalWorkflow = workflowContext.getWorkflow("internal", targetFolder);
            if (!(internalWorkflow instanceof FolderWorkflow)) {
                throw new WorkflowException(
                        "Target folder does not have a folder workflow in the category 'internal'.");
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

            if (category == null) {
                throw new WorkflowException("No category found to use for adding translation to target folder");
            }
            if (type == null) {
                throw new WorkflowException("No type found to use for adding translation to target folder");
            }

            String path = ((FolderWorkflow) internalWorkflow).add(category, type, name);
            copiedDoc = rootSession.getNode(path);

            JcrUtils.ensureIsCheckedOut(copiedDoc);
            if (!copiedDoc.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                copiedDoc.addMixin(HippoTranslationNodeType.NT_TRANSLATED);
            }
            copiedDoc.setProperty(HippoTranslationNodeType.ID,
                    userSubject.getProperty(HippoTranslationNodeType.ID).getString());
            copiedDoc.setProperty(HippoTranslationNodeType.LOCALE, language);
            rootSession.save();
            copyFolderTypes(copiedDoc, prototypes);
        }

        rootSession.save();
        rootSession.refresh(false);
        return new Document(copiedDoc);
    }

    public void addTranslation(String language, Document document) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {
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

        HippoTranslatedNode translatedNode = new HippoTranslatedNode(rootSubject);
        Set<String> translations;
        try {
            translations = translatedNode.getTranslations();
        } catch (RepositoryException ex) {
            throw new WorkflowException("Exception during searching for available translations", ex);
        }

        hints.put("locales", (Serializable) translations);

        Set<String> available = new TreeSet<String>();
        // for all the available translations we pick the highest ancestor of rootSubject of type HippoTranslationNodeType.NT_TRANSLATED,
        // and take all the translations for that node
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

    private void copyFolderTypes(final Node copiedDoc, final Map<String, Set<String>> prototypes) throws RepositoryException {
        // check if we have all subject folder types;
        Document rootDocument = new Document(rootSubject);
        Workflow internalWorkflow;
        try {
            internalWorkflow = workflowContext.getWorkflow("internal", rootDocument);
            if (!(internalWorkflow instanceof FolderWorkflow)) {
                throw new WorkflowException(
                        "Target folder does not have a folder workflow in the category 'internal'.");
            }
            final Map<String, Set<String>> copyPrototypes = (Map<String, Set<String>>) internalWorkflow.hints().get("prototypes");
            if (copyPrototypes != null && copyPrototypes.size() > 0) {
                // got some stuff...check if equal:
                final Set<String> protoKeys = prototypes.keySet();
                final Set<String> copyKeys = copyPrototypes.keySet();
                // check if we have a difference and overwrite
                if (copyKeys.size() != protoKeys.size() || !copyKeys.containsAll(protoKeys)) {
                    final String[] newValues = copyKeys.toArray(new String[copyKeys.size()]);
                    copiedDoc.setProperty("hippostd:foldertype", newValues);
                }
            }
        } catch (WorkflowException e) {
            log.warn(e.getClass().getName() + ": " + e.getMessage(), e);
        } catch (RemoteException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage(), e);
        }
    }
}
