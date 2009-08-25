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
    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(ContentBuilder.class);
    
    public static class NameSettings implements IClusterable {
        private static final long serialVersionUID = 1L;
        
        int minLength = 20;
        int maxLength = 35;
        int amount = 5;
        
        public NameSettings() {
        }
        
        public NameSettings(int min, int max, int amount) {
            this.minLength = min;
            this.maxLength = max;
            this.amount = amount;
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
    
    public ContentBuilder() {
        this(DEFAULT_WORKFLOW_CATEGORY);
    }

    public ContentBuilder(String workflowCategory) {
        this.workflowCategory = workflowCategory;
    }

    public void createRandomDocuments(String folder, NameSettings nameSettings) {
        Collection<String> types = getDocumentTypes(folder);
        createDocuments(folder, types, nameSettings, true);
    }

    public void createDocuments(String folder, Collection<String> selectedTypes, NameSettings nameSettings) {
        updateFolder(folder);
        createDocuments(folder, selectedTypes, nameSettings, false);
    }

    private void createDocuments(String folder, Collection<String> types, NameSettings nameSettings, boolean random) {

        if (folderWorkflow != null) {
            String[] typeAr = types.toArray(new String[types.size()]);
            for(int i=0; i<nameSettings.amount; i++) {
                Random generator = new Random();
                int index = generator.nextInt(types.size());   
                String prototype = typeAr[index];
                String targetName = generateName(nameSettings.minLength, nameSettings.maxLength);
                try {
                    folderWorkflow.add(NEW_DOCUMENT_CATEGORY, prototype, NodeNameCodec.encode(targetName, true));
                } catch (MappingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
        }
    }

    public Collection<String> getFolderTypes(String folder) {
        if(updateFolder(folder)) {
            folderTypes = getTypes(NEW_FOLDER_CATEGORY, new LinkedList<String>()); 
        }
        return folderTypes;
    }

    
    public Collection<String> getDocumentTypes(String folder) {
        if(updateFolder(folder)) {
            docTypes = getTypes(NEW_DOCUMENT_CATEGORY, new LinkedList<String>()); 
        }
        return docTypes;
    }
    
    private Collection<String> getTypes(String category, List<String> types) {
        if(folderWorkflow != null) {
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
    
    private boolean updateFolder(String folder) {
        if(folderPath == null || !folderPath.equals(folder)) {
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
        if(names == null) {
            names = new NamesFactory().newNames();
        }
        names.setMaximumLength(maxLength);
        String newName = "";
        while(newName.length() < minLength) {
            if(!newName.equals("")) {
                newName += " ";
            }
            String gen = names.generate();
            if(newName.length() + gen.length() > maxLength) {
                names.setMaximumLength(maxLength - newName.length());
                gen = names.generate();
            }
            newName += gen;
        }
        if(newName.length() > maxLength) {
            newName = newName.substring(0, maxLength);
            if(newName.length() > maxLength) {
                throw new IllegalStateException("warning, imcapable programmer at work");
            }
        }
        return newName;
    }

    public String uuid2path(String uuid) {
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

    
}
