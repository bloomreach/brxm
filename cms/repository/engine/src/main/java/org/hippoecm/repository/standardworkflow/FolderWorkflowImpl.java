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

import java.rmi.RemoteException;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;

public class FolderWorkflowImpl implements FolderWorkflow {

    private static final long serialVersionUID = 1L;
    private Session userSession;
    private Session rootSession;
    private Node subject;

    public FolderWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.subject = subject;
        this.userSession = userSession;
        this.rootSession = rootSession;
    }

    public Map<String, Set<String>> list() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Map<String, Set<String>> types = new TreeMap<String, Set<String>>();
        try {
            QueryManager qmgr = rootSession.getWorkspace().getQueryManager();
            Vector<Node> foldertypes = new Vector<Node>();
            Value[] foldertypeRefs = null;
            if (subject.hasProperty("hippostd:foldertype")) {
                try {
                    foldertypeRefs = subject.getProperty("hippostd:foldertype").getValues();
                    if (foldertypeRefs.length > 0) {
                        for (int i = 0; i < foldertypeRefs.length; i++) {
                            foldertypes.add(rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNode(foldertypeRefs[i].getString()));
                        }
                    } else {
                        foldertypeRefs = null;
                    }
                } catch (PathNotFoundException ex) {
                    foldertypeRefs = null;
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                } catch (ValueFormatException ex) {
                    foldertypeRefs = null;
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
            if (foldertypeRefs == null) {
                try {
                    for (NodeIterator iter = rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNodes(); iter.hasNext();) {
                        foldertypes.add(iter.nextNode());
                    }
                } catch (PathNotFoundException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }

            for (Node foldertype : foldertypes) {
                try {
                    Set<String> prototypes = new TreeSet<String>();
                    if (foldertype.isNodeType("nt:query")) {
                        Query query = qmgr.getQuery(foldertype);
                        QueryResult rs = query.execute();
                        for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
                            Node prototypeNode = iter.nextNode();
                            if (prototypeNode.isNodeType("hippo:templatetype")) {
                                if (prototypeNode.hasNode("hippo:prototype")) {
                                    String documentType = prototypeNode.getName();
                                    if (!documentType.startsWith("hippo:") && !documentType.startsWith("reporting:")) {
                                        if (documentType.contains(":")) {
                                            documentType = documentType.substring(documentType.indexOf(":") + 1);
                                        }
                                        prototypes.add(documentType);
                                    }
                                }
                            } else {
                                prototypes.add(prototypeNode.getName());
                            }
                        }
                    }
                    types.put(foldertype.getName(), prototypes);
                } catch (InvalidQueryException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                }
            }
        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }

        return types;
    }

    public String add(String name, String category, String template) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        name = ISO9075Helper.encodeLocalName(name);
        QueryManager qmgr = rootSession.getWorkspace().getQueryManager();
        Node foldertype = rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNode(category);
        Query query = qmgr.getQuery(foldertype);
        QueryResult rs = query.execute();
        Node result = null;
        Node target = userSession.getRootNode();
        if(!subject.getPath().substring(1).equals(""))
            target = target.getNode(subject.getPath().substring(1));
        Map<String, String[]> renames = new TreeMap<String, String[]>();
        for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
            Node prototypeNode = iter.nextNode();
            if (prototypeNode.isNodeType("hippo:templatetype")) {
                if (prototypeNode.hasNode("hippo:prototype")) {
                    String documentType = prototypeNode.getName();
                    Node prototype = prototypeNode.getNode("hippo:prototype");
                    if (!documentType.startsWith("hippo:") && !documentType.startsWith("reporting:")) {
                        if (documentType.contains(":")) {
                            documentType = documentType.substring(documentType.indexOf(":") + 1);
                        }
                        if (documentType.equals(template)) {
                            renames.put("_name", new String[] { name });
                            result = copy(prototype, target, renames, "");
                        }
                        
                    }
                }
	    } else {
                renames.put("_name", new String[] { name });
                result = copy(prototypeNode, target, renames, "");
	    }
        }
        if(result != null) {
            target.save();
            return result.getPath();
        } else {
            return null;
        }
    }

    public void archive(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        name = ISO9075Helper.encodeLocalName(name);
        Node root = userSession.getRootNode();
        String path = subject.getPath() + "/" + name;
        if (root.hasNode(path)) {
            Node offspring = root.getNode(path);
            if (subject.getPath().equals("/attic")) {
                offspring.remove();
            } else {
                userSession.getWorkspace().move(path, "/attic" + offspring.getName());
            }

        }
    }

    public // FIXME
    Node copy(Node source, Node target, Map<String, String[]> renames, String path) throws RepositoryException {
        String[] values;
        String name = source.getName();
        if (renames.containsKey("_name")) {
            values = renames.get("_name");
            if (values.length > 0) {
                name = values[0];
            }
        }

        String type = source.getPrimaryNodeType().getName();
        if (renames.containsKey("jcr:primaryType")) {
            values = renames.get("jcr:primaryType");
            if (values.length > 0) {
                type = values[0];
                if (type.equals("")) {
                    type = null;
                }
            } else {
                type = null;
            }
        }

        if (type != null) {
            target = target.addNode(name);
        } else {
            target = target.addNode(name, type);
        }
        if(source.hasProperty("jcr:mixinTypes")) {
            Value[] mixins = source.getProperty("jcr:mixinTypes").getValues();
            for(int i=0; i<mixins.length; i++) {
                target.addMixin(mixins[i].getString());
            }
        }
        if (renames.containsKey("jcr:mixinTypes")) {
            values = renames.get("jcr:mixinTypes");
            for (int i = 0; i <
                    values.length; i++) {
                if (!target.isNodeType(values[i])) {
                    target.addMixin(values[i]);
                }
            }
        }

        for (NodeIterator iter = source.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            copy(child, target, renames, path + child.getName());
        }

        for (PropertyIterator iter = source.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if(!prop.getDefinition().isProtected()) {
                if(prop.getDefinition().isMultiple()) {
                    target.setProperty(prop.getName(), prop.getValues());
                } else {
                    target.setProperty(prop.getName(), prop.getValue());
                }
            }
        }

        return target;
    }
}
