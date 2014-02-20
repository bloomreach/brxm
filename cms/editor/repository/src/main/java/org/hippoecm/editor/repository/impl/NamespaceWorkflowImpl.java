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
package org.hippoecm.editor.repository.impl;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceWorkflowImpl extends WorkflowImpl implements NamespaceWorkflow, InternalWorkflow {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NamespaceWorkflowImpl.class);
    

    private final String prefix;

    private final Node subject;
    private final Session session;

    public NamespaceWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.session = rootSession;
        this.prefix = subject.getName();
        this.subject = subject;
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> hints = new TreeMap<String, Serializable>();
        hints.put("prefix", prefix);
        LinkedList<String> documentTypes = new LinkedList<String>();
        Map<String, List<String>> descendantTypes = new TreeMap<String, List<String>>();
        try {
            final NodeIterator nodes = subject.getNodes();
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                final String relPath = HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE;
                if (child.hasNode(relPath)) {
                    Node typeNode = child.getNode(relPath);
                    if (typeNode.hasProperty("hipposysedit:supertype")) {
                        final Value[] values = typeNode.getProperty("hipposysedit:supertype").getValues();
                        for (Value value : values) {
                            String superType = value.getString();
                            if (superType.startsWith(prefix + ":")) {
                                superType = superType.substring(prefix.length() + 1);
                                if (!descendantTypes.containsKey(superType)) {
                                    descendantTypes.put(superType, new LinkedList<String>());
                                }
                                descendantTypes.get(superType).add(child.getName());
                            }
                        }
                    }
                }
            }
            if (subject.hasNode("basedocument")) {
                documentTypes.add("basedocument");
                boolean update = true;
                while (update) {
                    update = false;
                    for (String superType : new ArrayList<String>(documentTypes)) {
                        if (descendantTypes.containsKey(superType)) {
                            List<String> subTypes = descendantTypes.get(superType);
                            for (String subType : subTypes) {
                                if (!documentTypes.contains(subType)) {
                                    documentTypes.add(subType);
                                    update = true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not determine list of document types for prefix " + prefix, e);
        }
        hints.put("documentTypes", documentTypes);

        return hints;
    }

    @Override
    public void addCompoundType(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        WorkflowContext context = getWorkflowContext();

        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();

        FolderWorkflow folderWorkflow = (FolderWorkflow) context.getWorkflow("internal");
        Map<String, String> replacements = new TreeMap<String, String>();
        replacements.put("name", name);
        replacements.put("uri", nsReg.getURI(prefix));
        replacements.put("supertype", "hippo:compound");
        replacements.put("type", prefix + ":" + name);

        // ignore return type, as workflow chaining implies that the folder workflow
        // isn't executed until current method completes
        folderWorkflow.add("new-type", "compound", replacements);
    }

    @Override
    public void addDocumentType(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        addDocumentType(name, "basedocument");
    }

    @Override
    public void addDocumentType(final String name, final String superType) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        WorkflowContext context = getWorkflowContext();

        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();

        FolderWorkflow folderWorkflow = (FolderWorkflow) context.getWorkflow("internal");
        Map<String, String> replacements = new TreeMap<String, String>();
        replacements.put("name", name);
        replacements.put("uri", nsReg.getURI(prefix));
        replacements.put("supertype", prefix + ":" + superType);

        replacements.put("type", prefix + ":" + name);

        // ignore return type, as workflow chaining implies that the folder workflow
        // isn't executed until current method completes  
        folderWorkflow.add("new-type", "document", replacements);
    }
}
