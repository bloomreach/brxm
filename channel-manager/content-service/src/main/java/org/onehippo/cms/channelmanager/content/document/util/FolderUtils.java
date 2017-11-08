/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.document.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.channelmanager.content.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderUtils {

    private static final String HIPPOSTD_FOLDERTYPE = "hippostd:foldertype";
    private static final Logger log = LoggerFactory.getLogger(FolderUtils.class);

    private FolderUtils() {
    }

    public static boolean nodeExists(final Node parentNode, final String name) throws InternalServerErrorException {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        try {
            return parentNode.hasNode(name);
        } catch (final RepositoryException e) {
            log.warn("Failed to check whether node '{}' exists below '{}'", name, JcrUtils.getNodePathQuietly(parentNode), e);
            throw new InternalServerErrorException();
        }
    }

    public static boolean nodeWithDisplayNameExists(final Node parentNode, final String displayName) throws InternalServerErrorException {
        try {
            final NodeIterator children = parentNode.getNodes();
            while (children.hasNext()) {
                final Node child = children.nextNode();
                if (child.isNodeType(HippoStdNodeType.NT_FOLDER) || child.isNodeType(HippoNodeType.NT_HANDLE)) {
                    final String childName = ((HippoNode) child).getDisplayName();
                    if (StringUtils.equals(childName, displayName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (final RepositoryException e) {
            log.warn("Failed to check whether a node with display name '{}' exists below '{}'",
                    displayName, JcrUtils.getNodePathQuietly(parentNode), e);
            throw new InternalServerErrorException();
        }
    }

    public static String getLocale(final Node folderNode) {
        try {
            if (folderNode.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                return folderNode.getProperty(HippoTranslationNodeType.LOCALE).getString();
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to determine locale of folder '{}', assuming no locale", JcrUtils.getNodePathQuietly(folderNode), e);
        }
        return null;
    }

    public static Node getFolder(final String absPath, final Session session) throws NotFoundException, BadRequestException, InternalServerErrorException {
        try {
            if (!session.nodeExists(absPath)) {
                throw new NotFoundException();
            }
            return getExistingFolder(absPath, session);
        } catch (final RepositoryException e) {
            log.warn("Failed to get folder '{}'", absPath, e);
            throw new InternalServerErrorException();
        }
    }

    public static Node getFolder(final Node documentHandle) throws NotFoundException, InternalServerErrorException {
        try {
            return documentHandle.getParent();
        } catch (ItemNotFoundException e) {
            log.warn("Cannot get folder of document handle '{}': it does not have a parent", JcrUtils.getNodePathQuietly(documentHandle));
            throw new NotFoundException();
        } catch (RepositoryException e) {
            log.warn("Failed to get folder of document handle '{}'", JcrUtils.getNodePathQuietly(documentHandle), e);
            throw new InternalServerErrorException();
        }
    }

    public static Node getOrCreateFolder(final Node parentFolder, final String relPath, final Session session) throws BadRequestException, InternalServerErrorException {
        try {
            if (parentFolder.hasNode(relPath)) {
                return getExistingFolder(getAbsPath(parentFolder, relPath), session);
            } else {
                return createFolder(parentFolder, relPath, session);
            }
        } catch (final RepositoryException e) {
            log.warn("Failed to get or create folder '{}' below '{}'", relPath, JcrUtils.getNodePathQuietly(parentFolder), e);
            throw new InternalServerErrorException();
        }
    }

    private static String getAbsPath(final Node parentNode, final String relPath) throws RepositoryException {
        if (parentNode.getDepth() == 0) {
            // parent is the root node
            return "/" + relPath;
        } else {
            return parentNode.getPath() + "/" + relPath;
        }
    }

    private static Node getExistingFolder(final String absPath, final Session session) throws BadRequestException, RepositoryException {
        final Node folderNode = session.getNode(absPath);

        if (!folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
            throw new BadRequestException(new ErrorInfo(Reason.NOT_A_FOLDER, "path", absPath));
        }

        return folderNode;
    }

    private static Node createFolder(final Node parentFolder, final String relPath, final Session session) throws RepositoryException, InternalServerErrorException {
        final List<String> newFolderNames = new ArrayList<>();
        Node folderNode = parentFolder;

        final StringTokenizer pathElements = new StringTokenizer(relPath, "/");

        // drill down into existing parent folders
        while (pathElements.hasMoreTokens()) {
            final String pathElement = pathElements.nextToken();
            if (newFolderNames.isEmpty() && folderNode.hasNode(pathElement)) {
                folderNode = folderNode.getNode(pathElement);
            } else {
                newFolderNames.add(pathElement);
            }
        }

        // create new (parent) folders
        if (!newFolderNames.isEmpty()) {
            final WorkflowManager workflowMgr = getWorkflowManager(session);
            for (final String newFolderName : newFolderNames) {
                folderNode = createFolder(newFolderName, folderNode, workflowMgr);
            }
        }

        return folderNode;
    }

    private static WorkflowManager getWorkflowManager(final Session session) throws RepositoryException {
        final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
        return workspace.getWorkflowManager();
    }

    private static Node createFolder(final String name, final Node parentNode, final WorkflowManager workflowMgr) throws RepositoryException, InternalServerErrorException {
        final Workflow workflow = workflowMgr.getWorkflow("internal", parentNode);

        if (workflow instanceof FolderWorkflow) {
            final Node newFolder = createFolder(name, parentNode, (FolderWorkflow) workflow);
            copyFolderTypes(parentNode, newFolder);
            return newFolder;
        } else {
            log.warn("Failed to create folder '{}': workflow 'internal' of node '{}' has type {},"
                            + " which is not an instance of FolderWorkflow",
                    name, JcrUtils.getNodePathQuietly(parentNode), workflow.getClass().getCanonicalName());
            throw new InternalServerErrorException();
        }
    }

    private static Node createFolder(final String name, final Node parentNode, final FolderWorkflow workflow) throws RepositoryException, InternalServerErrorException {
        final String category = getNewFolderWorkflowCategory(parentNode);
        try {
            workflow.add(category, HippoStdNodeType.NT_FOLDER, name);
            return parentNode.getNode(name);
        } catch (RemoteException | WorkflowException e) {
            log.warn("Failed to execute 'add' with category '{}', type '{}' and relPath '{}' in folder workflow {}",
                    category, HippoStdNodeType.NT_FOLDER, name, workflow.getClass().getCanonicalName(), e);
            throw new InternalServerErrorException();
        }
    }

    private static String getNewFolderWorkflowCategory(final Node parentNode) throws RepositoryException {
        if (Arrays.stream(parentNode.getMixinNodeTypes())
                .map(NodeTypeDefinition::getName)
                .anyMatch(name -> name.equals(HippoTranslationNodeType.NT_TRANSLATED))) {
            return "new-translated-folder";
        }
        return "new-folder";
    }

    private static void copyFolderTypes(final Node parentNode, final Node newFolder) throws RepositoryException {
        final Property parentFolderType = parentNode.getProperty(HIPPOSTD_FOLDERTYPE);
        newFolder.setProperty(HIPPOSTD_FOLDERTYPE, parentFolderType.getValues());
    }
}
