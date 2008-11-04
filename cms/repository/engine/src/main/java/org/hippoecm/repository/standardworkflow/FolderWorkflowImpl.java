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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;

public class FolderWorkflowImpl implements FolderWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(FolderWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    private final Session userSession;
    private final Session rootSession;
    private final Node subject;

    private final String ATTIC_PATH = "/content/attic"; // FIXME

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
            Node templates = rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates");
            Value[] foldertypeRefs = null;
            if (subject.hasProperty("hippostd:foldertype")) {
                try {
                    foldertypeRefs = subject.getProperty("hippostd:foldertype").getValues();
                    if (foldertypeRefs.length > 0) {
                        for (int i = 0; i < foldertypeRefs.length; i++) {
                            String foldertype = foldertypeRefs[i].getString();
                            if (templates.hasNode(foldertype)) {
                                foldertypes.add(templates.getNode(foldertype));
                            } else {
                                log.warn("Unknown folder type " + foldertype);
                            }
                        }
                    } else {
                        foldertypeRefs = null;
                    }
                } catch (PathNotFoundException ex) {
                    foldertypeRefs = null;
                    log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                } catch (ValueFormatException ex) {
                    foldertypeRefs = null;
                    log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                }
            }
            if (foldertypeRefs == null) {
                try {
                    for (NodeIterator iter = templates.getNodes(); iter.hasNext();) {
                        foldertypes.add(iter.nextNode());
                    }
                } catch (PathNotFoundException ex) {
                    log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                }
            }

            for (Node foldertype : foldertypes) {
                try {
                    Set<String> prototypes = new TreeSet<String>();
                    if (foldertype.isNodeType("nt:query")) {
                        Query query = qmgr.getQuery(foldertype);
                        query = qmgr.createQuery(foldertype.getProperty("jcr:statement").getString(), query.getLanguage()); // HREPTWO-1266
                        QueryResult rs = query.execute();
                        for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
                            Node typeNode = iter.nextNode();
                            if (typeNode.getName().equals("hippo:prototype")) {
                                String documentType = typeNode.getPrimaryNodeType().getName();
                                if (!documentType.startsWith("hippo:") && !documentType.startsWith("reporting:")
                                        && !documentType.equals("nt:unstructured") && !documentType.startsWith("hippogallery:")) {
                                    prototypes.add(documentType);
                                }
                            } else {
                                prototypes.add(typeNode.getName());
                            }
                        }
                    }
                    types.put(foldertype.getName(), prototypes);
                } catch (InvalidQueryException ex) {
                    log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
        return types;
    }

    public String add(String category, String template, String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Map<String,String> arguments = new TreeMap<String,String>();
        arguments.put("name", name);
        return add(category, template, arguments);
    }

    public String add(String category, String template, Map<String,String> arguments) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String name = ISO9075Helper.encodeLocalName(arguments.get("name"));
        QueryManager qmgr = rootSession.getWorkspace().getQueryManager();
        Node foldertype = rootSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNode(category);
        Query query = qmgr.getQuery(foldertype);
        query = qmgr.createQuery(foldertype.getProperty("jcr:statement").getString(), query.getLanguage());
        QueryResult rs = query.execute();
        Node result = null;
        Node target = userSession.getRootNode();
        if(!subject.getPath().substring(1).equals(""))
            target = target.getNode(subject.getPath().substring(1));
        Map<String, String[]> renames = new TreeMap<String, String[]>();
        for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
            Node prototypeNode = iter.nextNode();
            if (prototypeNode.getName().equals("hippo:prototype")) {
                String documentType = prototypeNode.getPrimaryNodeType().getName();
                if (documentType.equals(template)) {
                    // create handle ourselves
                    result = target.addNode(name, "hippo:handle");
                    result.addMixin("hippo:hardhandle");
                    renames.put("./_name", new String[] { name });
                    result = copy(prototypeNode, result, renames, ".");
                    break;
                }
            } else if (prototypeNode.getName().equals(template)) {
                if(foldertype.hasProperty("hippostd:modify")) {
                    Value[] values = foldertype.getProperty("hippostd:modify").getValues();
                    String currentTime = null;
                    for(int i=0; i+1<values.length; i+=2) {
                        String newValue = values[i+1].getString();
                        if(newValue.equals("$name")) {
                            newValue = name;
                        } else if(newValue.equals("$now")) {
                            if(currentTime == null) {
                                currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new java.util.Date());
                            }
                            newValue = currentTime;
                        } else if(newValue.startsWith("$")) {
                            newValue = arguments.get(newValue.substring(1));
                        }
                        if(renames.containsKey(values[i].getString())) {
                            String[] oldValues = renames.get(values[i].getString());
                            String[] newValues = new String[oldValues.length + 1];
                            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                            newValues[oldValues.length] = newValue;
                            renames.put(values[i].getString(), newValues);
                        } else {
                            renames.put(values[i].getString(), new String[] { newValue });
                        }
                    }
                }
                result = copy(prototypeNode, target, renames, ".");
                break;
            }
        }
        if(result != null) {
            userSession.save();
            rootSession.save();
            return result.getPath();
        } else {
            return null;
        }
    }

    public void archive(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if(name.startsWith("/"))
            name  = name.substring(1);
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            if (subject.getPath().equals(ATTIC_PATH)) {
                offspring.remove();
                folder.save();
            } else {
                userSession.getWorkspace().move(folder.getPath() + "/" + offspring.getName(),
                                                ATTIC_PATH + "/" + offspring.getName());
            }
        }
    }

    public void delete(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if(name.startsWith("/"))
            name  = name.substring(1);
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            offspring.remove();
            folder.save();
        }
    }
    
    public void reorder(List<String> newOrder) throws WorkflowException, MappingException, RepositoryException, RemoteException {
       List<String> list = new ArrayList<String>(newOrder);
       Collections.reverse(list);
       Node folder = userSession.getNodeByUUID(subject.getUUID());
       for (String item : list) {
           Node head = folder.getNodes().nextNode();
           if (!head.isSame(folder.getNode(item))) {
               folder.orderBefore(item, head.getName());
           }
       }
       folder.save();
    }

    public void delete(Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folderNode = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        Node documentNode = userSession.getNodeByUUID(document.getIdentity());
        if (documentNode.getPath().startsWith(folderNode.getPath()+"/")) {
            documentNode.remove();
            folderNode.save();
        }
    }

    static Node copy(Node source, Node target, Map<String, String[]> renames, String path) throws RepositoryException {
        String[] renamed;
        Value[] values;
        String name = source.getName();
        if (renames.containsKey(path+"/_name")) {
            renamed = renames.get(path+"/_name");
            if (renamed.length > 0) {
                name = renamed[0];
            }
        }

        String primaryType = source.getPrimaryNodeType().getName();
        if (renames.containsKey(path+"/jcr:primaryType")) {
            renamed = renames.get(path+"/jcr:primaryType");
            if (renamed.length > 0) {
                primaryType = expand(renamed, source)[0].getString();
                if (primaryType.equals("")) {
                    primaryType = null;
                }
            } else {
                primaryType = null;
            }
        }

        if (primaryType != null) {
            target = target.addNode(name, primaryType);
        } else {
            target = target.addNode(name);
        }
        
        if(source.hasProperty("jcr:mixinTypes")) {
            Value[] mixins = source.getProperty("jcr:mixinTypes").getValues();
            for(int i=0; i<mixins.length; i++) {
                target.addMixin(mixins[i].getString());
            }
        }

        if (renames.containsKey(path+"/jcr:mixinTypes")) {
            values = expand(renames.get(path+"/jcr:mixinTypes"), source);
            for (int i = 0; i < values.length; i++) {
                if (!target.isNodeType(values[i].getString())) {
                    target.addMixin(values[i].getString());
                }
            }
        }

        NodeType[] mixinNodeTypes = target.getMixinNodeTypes();
        NodeType[] nodeTypes = new NodeType[mixinNodeTypes.length + 1];
        nodeTypes[0] = target.getPrimaryNodeType();
        if(mixinNodeTypes.length > 0) {
            System.arraycopy(mixinNodeTypes, 0, nodeTypes, 1, mixinNodeTypes.length);
        }

        for (NodeIterator iter = source.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            String childPath;
            if(renames.containsKey(path + "/_node/_name") && !renames.containsKey(path + "/"+ child.getName()))
                childPath = path + "/_node";
            else
                childPath = path + "/" + child.getName();

            copy(child, target, renames, childPath);
        }

        for (PropertyIterator iter = source.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            // FIXME: workaround for HREPTWO-1585
            if(prop.getName().equals("hippo:paths"))
                continue;
            if (prop.getDefinition().isMultiple()) {
                boolean isProtected = true;
                for(int i=0; i<nodeTypes.length; i++) {
                    if(nodeTypes[i].canSetProperty(prop.getName(), prop.getValues())) {
                        isProtected = false;
                        break; 
                    }
                }
                for(int i=0; i<nodeTypes.length; i++) {
                    PropertyDefinition[] propDefs = nodeTypes[i].getPropertyDefinitions();
                    for(int j=0; j<propDefs.length; j++) {
                        if(propDefs[j].getName().equals(prop.getName()) && propDefs[j].isProtected())
                            isProtected = true;
                    }
                }
                if (!isProtected) {
                    if(renames.containsKey(path+"/"+prop.getName())) {
                        target.setProperty(prop.getName(), expand(renames.get(path+"/"+prop.getName()), source));
                    } else {
                        target.setProperty(prop.getName(), prop.getValues());
                    }
                }
            } else {
                boolean isProtected = true;
                for(int i=0; i<nodeTypes.length; i++) {
                    if(nodeTypes[i].canSetProperty(prop.getName(), prop.getValue())) {
                        isProtected = false;
                        break; 
                    }
                }
                for(int i=0; i<nodeTypes.length; i++) {
                    PropertyDefinition[] propDefs = nodeTypes[i].getPropertyDefinitions();
                    for(int j=0; j<propDefs.length; j++) {
                        if(propDefs[j].getName().equals(prop.getName()) && propDefs[j].isProtected())
                            isProtected = true;
                    }
                }
                if (!isProtected) {
                    if(renames.containsKey(path+"/"+prop.getName())) {
                        target.setProperty(prop.getName(), expand(renames.get(path+"/"+prop.getName()), source)[0]);
                    } else {
                        target.setProperty(prop.getName(), prop.getValue());
                    }
                }
            }
        }

        return target;
    }

    static private Value[] expand(String[] values, Node source) throws RepositoryException {
        Vector<Value> newValues = new Vector<Value>();
        for(int i=0; i<values.length; i++) {
            String value = values[i];
            if(value.startsWith("${") && value.endsWith("}")) {
                value = value.substring(2,value.length()-1);
                Property p = source.getProperty(value);
                if(p.getDefinition().isMultiple()) {
                    Value[] referencedValues = p.getValues();
                    for(int j=0; j<referencedValues.length; j++)
                        newValues.add(referencedValues[j]);
                } else {
                    newValues.add(p.getValue());
                }
            } else {
                newValues.add(source.getSession().getValueFactory().createValue(value));
            }
        }
        return newValues.toArray(new Value[newValues.size()]);
    }
}
