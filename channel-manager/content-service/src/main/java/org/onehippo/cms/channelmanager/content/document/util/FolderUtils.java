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
import java.util.Arrays;
import java.util.LinkedList;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderUtils {

    private static final String HIPPOSTD_FOLDERTYPE = "hippostd:foldertype";
    private static final Logger log = LoggerFactory.getLogger(FolderUtils.class);

    public static boolean nodeExists(final Node parentNode, final String name) throws ErrorWithPayloadException {
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        try {
            return parentNode.hasNode(name);
        } catch (RepositoryException e) {
            log.warn("Failed to check whether node '{}' exists below '{}'", name, JcrUtils.getNodePathQuietly(parentNode));
            throw new InternalServerErrorException();
        }
    }

    public static Node getOrCreateFolder(final String absPath, final Session session) throws ErrorWithPayloadException {
        try {
            if (session.nodeExists(absPath)) {
                return getFolder(absPath, session);
            } else {
                return createFolder(absPath, session);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to get or create folder '{}'", absPath, e);
            throw new InternalServerErrorException();
        }
    }

    private static Node getFolder(final String absPath, final Session session) throws RepositoryException, BadRequestException {
        final Node folderNode = session.getNode(absPath);

        if (!folderNode.isNodeType(HippoStdNodeType.NT_FOLDER)) {
            throw new BadRequestException(new ErrorInfo(ErrorInfo.Reason.NOT_A_FOLDER, "path", absPath));
        }

        return folderNode;
    }

    private static Node createFolder(final String absPath, final Session session) throws RepositoryException, InternalServerErrorException {
        final LinkedList<String> newFolderNames = new LinkedList<>();

        String checkPath = absPath;
        Node folderNode = null;

        // navigate up the tree to find the lowest already existing node in the path
        while (folderNode == null) {
            if (session.nodeExists(checkPath)) {
                folderNode = session.getNode(checkPath);
            } else {
                newFolderNames.addFirst(StringUtils.substringAfterLast(checkPath, "/"));
                checkPath = StringUtils.substringBeforeLast(checkPath, "/");
                if (StringUtils.isEmpty(checkPath)) {
                    folderNode = session.getRootNode();
                }
            }
        }

        // add all (parent) folders that do not exist yet
        final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
        final WorkflowManager workflowMgr = workspace.getWorkflowManager();
        for (String folderName : newFolderNames) {
            folderNode = createFolder(folderName, folderNode, workflowMgr);
        }

        return folderNode;
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
