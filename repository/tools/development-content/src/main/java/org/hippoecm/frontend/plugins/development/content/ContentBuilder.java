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

package org.hippoecm.frontend.plugins.development.content;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugins.development.content.names.Names;
import org.hippoecm.frontend.plugins.development.content.names.NamesFactory;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentBuilder implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ContentBuilder.class);

    public static class NameSettings implements IClusterable {
        private static final long serialVersionUID = 1L;

        int minLength = 20;
        int maxLength = 35;

        public NameSettings() {
        }
    }

    public static class NodeTypeSettings implements IClusterable {
        private static final long serialVersionUID = 1L;

        Collection<String> types = new LinkedList<String>();
        boolean random = true;

        public void setTypes(Collection<String> selectedTypes) {
            this.types = selectedTypes;
        }

        public Collection<String> getTypes() {
            return types;
        }

        public void setRandom(boolean random) {
            this.random = random;
        }

        public boolean isRandom() {
            return random;
        }
    }

    public static class NodeSettings implements IClusterable {
        private static final long serialVersionUID = 1L;

        String folderUUID;

        NameSettings naming;
        NodeTypeSettings nodeTypes;

        public NodeSettings() {
            naming = new NameSettings();
            nodeTypes = new NodeTypeSettings();
        }

        public NodeSettings(String folderUUID) {
            this();
            this.folderUUID = folderUUID;
        }
    }

    public static class FolderSettings extends NodeSettings {
        private static final long serialVersionUID = 1L;

        int depth = 3;
        int minimumChildNodes = 2;
        int maximumChildNodes = 2;

        DocumentSettings document = new DocumentSettings();

        public FolderSettings() {
        }

        public FolderSettings(String folderUUID) {
            super(folderUUID);
        }
    }

    public static class DocumentSettings extends NodeSettings {
        private static final long serialVersionUID = 1L;

        int amount = 5;

        public DocumentSettings() {
        }

        public DocumentSettings(String folderUUID) {
            super(folderUUID);
        }
    }

    private static final String DEFAULT_WORKFLOW_CATEGORY = "threepane";
    private static final String NEW_FOLDER_CATEGORY = "new-folder";
    private static final String NEW_DOCUMENT_CATEGORY = "new-document";

    String folderPath;
    FolderWorkflow folderWorkflow;
    Names names;

    Collection<String> docTypes;
    Collection<String> folderTypes;

    private String workflowCategory;

    Random generator = new Random();

    public ContentBuilder() {
        this(DEFAULT_WORKFLOW_CATEGORY);
    }

    public ContentBuilder(String workflowCategory) {
        this.workflowCategory = workflowCategory;
    }

    public void createDocuments(DocumentSettings settings) {
        updateFolder(settings.folderUUID);

        if (settings.nodeTypes.random) {
            settings.nodeTypes.types = getDocumentTypes(settings.folderUUID);
        }

        if (folderWorkflow != null) {
            String[] typeAr = settings.nodeTypes.types.toArray(new String[settings.nodeTypes.types.size()]);

            for (int i = 0; i < settings.amount; i++) {
                createNode(NEW_DOCUMENT_CATEGORY, typeAr, settings.naming);
            }
        }
    }

    private String createNode(String category, String[] typeAr, NameSettings settings) {
        int index = generator.nextInt(typeAr.length);
        String prototype = typeAr[index];
        String targetName = generateName(settings.minLength, settings.maxLength);
        try {
            String name = NodeNameCodec.encode(targetName, true);
            folderWorkflow.add(category, prototype, name);
            return name;
        } catch (MappingException e) {
            log.error("Error creating new node of category " + category, e);
        } catch (RemoteException e) {
            log.error("Error creating new node of category " + category, e);
        } catch (WorkflowException e) {
            log.error("Error creating new node of category " + category, e);
        } catch (RepositoryException e) {
            log.error("Error creating new node of category " + category, e);
        }
        return null;
    }

    public void createFolders(FolderSettings settings, int depth) {
        updateFolder(settings.folderUUID);

        if (settings.nodeTypes.random) {
            settings.nodeTypes.types = getFolderTypes(settings.folderUUID);
        }

        if (settings.nodeTypes.types == null || settings.nodeTypes.types.size() == 0) {
            return;
        }

        if (folderWorkflow != null) {
            String[] nodeTypes = settings.nodeTypes.types.toArray(new String[settings.nodeTypes.types.size()]);

            int amount = settings.minimumChildNodes;
            int diff = settings.maximumChildNodes - settings.minimumChildNodes;
            if (diff > 0) {
                amount += generator.nextInt(diff);
            }

            List<String> newNodes = new LinkedList<String>();
            for (int i = 0; i < amount; i++) {
                newNodes.add(createNode(NEW_FOLDER_CATEGORY, nodeTypes, settings.naming));
            }
            
            String rootPath = folderPath;
            for (String newNode : newNodes) {
                settings.folderUUID = path2uuid(rootPath + "/" + newNode);

                if (settings.document.amount > 0) {
                    settings.document.folderUUID = settings.folderUUID;
                    createDocuments(settings.document);
                }

                if (depth < settings.depth) {
                    depth++;
                    createFolders(settings, depth);
                }


            }
        }
    }

    public Collection<String> getFolderTypes(String folderUUID) {
        updateFolder(folderUUID);
        return getTypes(NEW_FOLDER_CATEGORY, new LinkedList<String>());
    }

    public Collection<String> getDocumentTypes(String folderUUID) {
        updateFolder(folderUUID);
        return getTypes(NEW_DOCUMENT_CATEGORY, new LinkedList<String>());
    }

    @SuppressWarnings("unchecked")
    private Collection<String> getTypes(String category, List<String> types) {
        if (folderWorkflow != null) {
            Map<String, Serializable> hints;
            try {
                hints = folderWorkflow.hints();
                final Map<String, Set<String>> prototypes = (Map<String, Set<String>>) hints.get("prototypes");
                if (prototypes.containsKey(category)) {
                    for (String s : prototypes.get(category)) {
                        types.add(s);
                    }
                }
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (WorkflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return types;
    }

    private boolean updateFolder(String folderUUID) {
        String folder = uuid2path(folderUUID);
        if (folder != null && (folderPath == null || !folderPath.equals(folder))) {
            folderPath = folder;
            reloadWorkflow();
            return true;
        }
        return false;
    }

    private void reloadWorkflow() {

        Session jcrSession = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
        WorkflowDescriptor folderWorkflowDescriptor = null;
        Node node = null;

        try {
            node = jcrSession.getRootNode().getNode(folderPath.startsWith("/") ? folderPath.substring(1) : folderPath);

            WorkflowManager manager = ((HippoWorkspace) (jcrSession.getWorkspace())).getWorkflowManager();
            folderWorkflowDescriptor = manager.getWorkflowDescriptor(workflowCategory, node);
            Workflow workflow = (folderWorkflowDescriptor != null ? manager.getWorkflow(folderWorkflowDescriptor)
                    : null);
            if (workflow instanceof FolderWorkflow) {
                folderWorkflow = (FolderWorkflow) workflow;
            }
        } catch (MappingException ex) {
            log.warn("failure to initialize shortcut", ex);
            node = null;
        } catch (RepositoryException ex) {
            log.warn("failure to initialize shortcut", ex);
            node = null;
        }

    }

    public String generateName(int minLength, int maxLength) {
        if (names == null) {
            names = new NamesFactory().newNames();
        }
        names.setMaximumLength(maxLength);
        String newName = "";
        while (newName.length() < minLength) {
            if (!newName.equals("")) {
                newName += " ";
            }
            String gen = names.generate();
            if (newName.length() + gen.length() > maxLength) {
                names.setMaximumLength(maxLength - newName.length());
                gen = names.generate();
            }
            newName += gen;
        }
        if (newName.length() > maxLength) {
            newName = newName.substring(0, maxLength);
            if (newName.length() > maxLength) {
                throw new IllegalStateException("warning, imcapable programmer at work");
            }
        }
        return newName;
    }

    private String uuid2path(String uuid) {
        Session jcrSession = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
        try {
            Node node = jcrSession.getNodeByUUID(uuid);
            return node.getPath();
        } catch (ItemNotFoundException e) {
            log.error("Node node found for uuid " + uuid, e);
        } catch (RepositoryException e) {
            log.error("Error while retrieving node path for uuid " + uuid, e);
        }
        return null;
    }

    private String path2uuid(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        //WorkflowManager manager = ((HippoWorkspace) (jcrSession.getWorkspace())).getWorkflowManager();
        //manager.getSession().save();
        Session jcrSession = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
        try {
            if (jcrSession.getRootNode().hasNode(path)) {
                Node node = jcrSession.getRootNode().getNode(path);
                return node.getUUID();
            }
            log.error("Could not find node with path " + path);
        } catch (ItemNotFoundException e) {
            log.error("Node node found for path " + path, e);
        } catch (RepositoryException e) {
            log.error("Error while retrieving uuid for node path" + path, e);
        }
        return null;
    }

}
