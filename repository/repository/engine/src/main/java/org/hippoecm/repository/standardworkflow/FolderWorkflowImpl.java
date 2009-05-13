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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;

public class FolderWorkflowImpl implements FolderWorkflow, EmbedWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(FolderWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    private final Session userSession;
    private final Session rootSession;
    private final WorkflowContext workflowContext;
    private final Node subject;

    public FolderWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
      throws RemoteException {
        this.workflowContext = context;
        this.subject = subject;
        this.userSession = userSession;
        this.rootSession = rootSession;
    }

    public Map<String,Serializable> hints() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Map<String,Serializable> info = new TreeMap<String,Serializable>();
        info.put("add", new Boolean(true));
        info.put("list", new Boolean(false));
        info.put("archive", new Boolean(true));
        info.put("delete", new Boolean(true));
        info.put("rename", new Boolean(true));
        info.put("copy", new Boolean(true));
        info.put("duplicate", new Boolean(true));
        info.put("move", new Boolean(true));
        info.put("reorder", new Boolean(subject.getPrimaryNodeType().hasOrderableChildNodes()));
        info.put("prototypes", (Serializable) prototypes());
        return info;
    }

    /**
     * @deprecated
     */
    public Map<String, Set<String>> list() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return prototypes();
    }

    protected Map<String, Set<String>> prototypes() throws RepositoryException {
        Map<String, Set<String>> types = new TreeMap<String, Set<String>>();
        try {
            QueryManager qmgr = userSession.getWorkspace().getQueryManager();
            Vector<Node> foldertypes = new Vector<Node>();
            Node templates = userSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates");
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
        String name = arguments.get("name");
        QueryManager qmgr = userSession.getWorkspace().getQueryManager();
        Node foldertype = userSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates").getNode(category);
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
                    // create handle ourselves, if not already exists
                    if(!target.hasNode(name) || !target.getNode(name).isNodeType(HippoNodeType.NT_HANDLE) ||
                                                 target.getNode(name).hasNode(name)) {
                        result = target.addNode(name, "hippo:handle");
                        result.addMixin("hippo:hardhandle");
                    } else {
                        result = target.getNode(name);
                    }
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
            return result.getPath();
        } else {
            return null;
        }
    }

    private void doArchive(String source, String destination) throws ConstraintViolationException, PathNotFoundException, ItemNotFoundException, VersionException, VersionException, AccessDeniedException, RepositoryException {
        rootSession.getWorkspace().move(source, destination);
        Node target = rootSession.getRootNode().getNode(destination.substring(1));
        if(target.isNodeType(HippoNodeType.NT_HANDLE)) {
            target.checkout();
            for(NodeIterator iter = target.getNodes(); iter.hasNext(); ) {
                iter.nextNode().checkin();
            }
            for(NodeIterator iter = target.getNodes(); iter.hasNext(); ) {
                iter.nextNode().remove();
            }
            target.save();
            target.checkin();
        }
    }

    public void archive(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String atticPath = null;
        RepositoryMap config = workflowContext.getWorkflowConfiguration();
        if(config.exists() && config.containsKey("attic") && config.get("attic") instanceof String) {
            atticPath = (String) config.get("attic");
        }
        if(name.startsWith("/"))
            name  = name.substring(1);
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            if (atticPath != null) {
                if (offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    offspring = offspring.getParent();
                }
                if (subject.getPath().equals(atticPath)) {
                    offspring.remove();
                    folder.save();
                } else {
                    if (userSession.getRootNode().hasNode(atticPath.substring(1))) {
                        doArchive(folder.getPath() + "/" + offspring.getName(),
                                  atticPath + "/" + atticName(atticPath, offspring.getName(), true));
                    } else {
                        throw new WorkflowException("Attic " + atticPath + " for archivation does not exist");
                    }
                }
            } else {
                throw new WorkflowException("No attic for archivation defined");
            }
        }
    }

    public void archive(Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String atticPath = null;
        RepositoryMap config = workflowContext.getWorkflowConfiguration();
        if(config.exists() && config.get("attic") instanceof String) {
            atticPath = (String) config.get("attic");
        }
        String path = subject.getPath().substring(1);
        Node folderNode = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        Node documentNode = rootSession.getNodeByUUID(document.getIdentity());
        if (documentNode.getPath().startsWith(folderNode.getPath()+"/")) {
            if (atticPath != null) {
            if (documentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && documentNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                documentNode = documentNode.getParent();
            }
            if (subject.getPath().equals(atticPath)) {
                documentNode.remove();
                folderNode.save();
            } else {
                doArchive(documentNode.getPath(), atticPath + "/" + atticName(atticPath, documentNode.getName(), true));
            }
            } else {
                throw new WorkflowException("No attic for archivation defined");
            }
        }
    }

    private String atticName(String atticPath, String documentName, boolean createPath) throws RepositoryException {
        Calendar now = Calendar.getInstance();
        String year = Integer.toString(now.get(Calendar.YEAR));
        String month = Integer.toString(now.get(Calendar.MONTH) + 1);
        String day = Integer.toString(now.get(Calendar.DAY_OF_MONTH));
        if(createPath) {
            Node destination, attic = rootSession.getRootNode().getNode(atticPath.substring(1));
            if(!attic.hasNode(year)) {
                destination = attic.addNode(year, "nt:unstructured");
            } else {
                destination = attic.getNode(year);
            }
            if(!destination.hasNode(month)) {
                destination = destination.addNode(month, "nt:unstructured");
            } else {
                destination = destination.getNode(month);
            }
            if(!destination.hasNode(day)) {
                destination = destination.addNode(day, "nt:unstructured");
            } else {
                destination = destination.getNode(day);
            }
            attic.save();
        }
        return year + "/" + month + "/" + day + "/" + documentName;
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

    public void delete(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if(name.startsWith("/"))
            name  = name.substring(1);
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            if (offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                offspring = offspring.getParent();
            }
            offspring.remove();
            folder.save();
        }
    }

    public void delete(Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folderNode = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        Node documentNode = userSession.getNodeByUUID(document.getIdentity());
        if (documentNode.getPath().startsWith(folderNode.getPath()+"/")) {
            if (documentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && documentNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                documentNode = documentNode.getParent();
            }
            documentNode.remove();
            folderNode.save();
        }
    }

    private void renameChildDocument(Node folderNode, String newName) throws RepositoryException {
        Node documentNode = folderNode.getSession().getRootNode().getNode(folderNode.getPath().substring(1)+"/"+newName);
        if (documentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            for (NodeIterator children = documentNode.getNodes(); children.hasNext(); ) {
                Node child = children.nextNode();
                if (child != null) {
                    if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        child.checkout();
                        folderNode.getSession().move(child.getPath(), documentNode.getPath()+"/"+documentNode.getName());
                    }
                }
            }
        }
    }

    public void rename(String name, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if(name.startsWith("/"))
            name  = name.substring(1);
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            if (folder.hasNode(newName)) {
                throw new WorkflowException("Cannot move document to same name");
            }
            Node offspring = folder.getNode(name);
            if(offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(HippoNodeType.NT_HANDLE))  {
                offspring = offspring.getParent();
            }
            offspring.checkout();
            folder.getSession().move(offspring.getPath(), folder.getPath()+"/"+newName);
            renameChildDocument(folder, newName);
            folder.save();
        }
    }

    public void rename(Document document, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folderNode = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        Node documentNode = userSession.getNodeByUUID(document.getIdentity());
        if(documentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && documentNode.getParent().isNodeType(HippoNodeType.NT_HANDLE))  {
            documentNode = documentNode.getParent();
        }
        if (documentNode.getPath().startsWith(folderNode.getPath()+"/")) {
            if (folderNode.hasNode(newName)) {
                throw new WorkflowException("Cannot move document to same name");
            }
            documentNode.checkout();
            folderNode.getSession().move(documentNode.getPath(), folderNode.getPath()+"/"+newName);
            renameChildDocument(folderNode, newName);
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
            if(child instanceof HippoNode) {
                Node canonical = ((HippoNode)child).getCanonicalNode();
                if(canonical == null || !canonical.isSame(child)) {
                    continue;
                }
            }
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

    public Document duplicate(String relPath)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = subject.getNode(relPath);
        return duplicate(source, "Copy of "+source.getName());
    }

    public Document duplicate(Document offspring)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = userSession.getNodeByUUID(offspring.getIdentity());
        return duplicate(source, "Copy of "+source.getName());
    }

    public Document duplicate(String relPath, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = subject.getNode(relPath);
        return duplicate(source, "Copy of "+arguments.get("name"));
    }

    public Document duplicate(Document offspring, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = userSession.getNodeByUUID(offspring.getIdentity());
        String targetName = arguments.get("name");
        return duplicate(source, targetName);
    }

    private Document duplicate(Node source, String targetName) throws WorkflowException, RepositoryException {
        if (subject.hasNode(targetName)) {
            throw new WorkflowException("Cannot duplicate document when duplicate already exists");
        }
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            source = source.getParent();
        }
        subject.getSession().getWorkspace().copy(source.getPath(), subject.getPath() + "/" + targetName);
        renameChildDocument(subject, targetName);
        subject.save();
        return new Document(subject.getNode(targetName).getUUID());
    }

    public Document copy(String relPath, String absPath)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return copy(relPath, absPath, null);
    }
    public Document copy(Document offspring, Document targetFolder, String targetName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return copy(offspring, targetFolder, targetName, null);
    }
    public Document copy(String relPath, String absPath, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = subject.getNode(relPath);
        if(!source.isNodeType(HippoNodeType.NT_DOCUMENT) && !source.isNodeType(HippoNodeType.NT_HANDLE)) {
            throw new MappingException("copied item is not a document");
        }
        Node target = subject.getSession().getRootNode().getNode(absPath.substring(1, absPath.lastIndexOf("/")));
        if(!target.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            throw new MappingException("copied destination is not a document");
        }
        return copyFrom(new Document(source.getUUID()), new Document(target.getUUID()), absPath.substring(absPath.lastIndexOf("/"+1)), arguments);
    }
    public Document copy(Document offspring, Document targetFolder, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return copyFrom(offspring, targetFolder, targetName, arguments);
    }

    public Document move(String relPath, String absPath)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return move(relPath, absPath, null);
    }
    public Document move(Document offspring, Document targetFolder, String targetName)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return move(offspring, targetFolder, targetName, null);
    }
    public Document move(String relPath, String absPath, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = subject.getNode(relPath);
        if(!source.isNodeType(HippoNodeType.NT_DOCUMENT) && !source.isNodeType(HippoNodeType.NT_HANDLE)) {
            throw new MappingException("copied item is not a document");
        }
        Node target = subject.getSession().getRootNode().getNode(absPath.substring(1, absPath.lastIndexOf("/")));
        if(!target.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            throw new MappingException("copied destination is not a document");
        }
        return moveFrom(new Document(source.getUUID()), new Document(target.getUUID()), absPath.substring(absPath.lastIndexOf("/"+1)), arguments);
    }
    public Document move(Document offspring, Document targetFolder, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return moveFrom(offspring, targetFolder, targetName, arguments);
    }

    public Document copyFrom(Document offspring, Document targetFolder, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        Node destination = userSession.getNodeByUUID(targetFolder.getIdentity());
        Node source = userSession.getNodeByUUID(offspring.getIdentity());
        if (folder.isSame(destination)) {
            //throw new WorkflowException("Cannot copy document to same folder, use duplicate instead");
            return duplicate(source, targetName);
        }
        if (source.getAncestor(folder.getDepth()).isSame(folder)) {
            ((EmbedWorkflow)workflowContext.getWorkflow("embedded", new Document(destination.getUUID()))).copyTo(new Document(subject.getUUID()), offspring, targetName, arguments);
        }
        return null;
    }

    public Document copyTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(targetName)) {
            throw new WorkflowException("Cannot copy document when document with same name exists");
        }
        Node source = userSession.getNodeByUUID(offspring.getIdentity());
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            source = source.getParent();
        }
        folder.getSession().getWorkspace().copy(source.getPath(), folder.getPath() + "/" + targetName);
        renameChildDocument(folder, targetName);
        folder.save();
        ((EmbedWorkflow)workflowContext.getWorkflow("embedded", sourceFolder)).copyOver(folder, offspring, new Document(folder.getNode(targetName).getUUID()), arguments);
        return new Document(folder.getNode(targetName).getUUID());
    }

    public Document copyOver(Node destination, Document offspring, Document result, Map<String,String> arguments) {
        return result;
    }

    public Document moveFrom(Document offspring, Document targetFolder, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        Node destination = userSession.getNodeByUUID(targetFolder.getIdentity());
        if (folder.isSame(destination)) {
            throw new WorkflowException("Cannot move document to same folder");
        }
        Node source = userSession.getNodeByUUID(offspring.getIdentity());
        if (source.getAncestor(folder.getDepth()).isSame(folder)) {
            ((EmbedWorkflow)workflowContext.getWorkflow("embedded", new Document(destination.getUUID()))).moveTo(new Document(subject.getUUID()), offspring, targetName, arguments);
        }
        return null;
    }

    public Document moveTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? userSession.getRootNode() : userSession.getRootNode().getNode(path));
        if (folder.hasNode(targetName)) {
            throw new WorkflowException("Cannot move document when document with same name exists");
        }
        Node source = userSession.getNodeByUUID(offspring.getIdentity());
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            source = source.getParent();
        }
        folder.getSession().getWorkspace().move(source.getPath(), folder.getPath() + "/" + targetName);
        renameChildDocument(folder, targetName);
        folder.save();
        ((EmbedWorkflow)workflowContext.getWorkflow("embedded", sourceFolder)).moveOver(folder, offspring, new Document(folder.getNode(targetName).getUUID()), arguments);
        return new Document(folder.getNode(targetName).getUUID());
    }

    public Document moveOver(Node destination, Document offspring, Document result, Map<String,String> arguments) {
        return result;
    }
}
