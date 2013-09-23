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
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizable implementation of the FolderWorkflow.  Uses foldertypes from the
 * /hippo:configuration/hippo:queries/hippo:templates folder to populate properties
 * in prototypes.  Prototypes are found with configurable queries.  For documents, these
 * usually reside under the hippo:namespaces folder.
 * <p>
 * Folder types contain a set of 'renames', in the form of the multi-valued property
 * hippostd:modify.  This is a set of (path, value) values, with the value being subject to
 * variable expansion.  The expansion takes the following form:
 * <ul>
 *   <li>$now: the current time
 *   <li>$holder: the user of the session that invoked the workflow
 *   <li>$inherit: the value under the same path in the parent document.
 *     Can be overridden by providing the property name as an argument. 
 *   <li>$&lt;other&gt;: value specified as an argument
 * </ul>
 */
public class FolderWorkflowImpl implements FolderWorkflow, EmbedWorkflow, InternalWorkflow {

    static final Logger log = LoggerFactory.getLogger(FolderWorkflowImpl.class);

    private static final long serialVersionUID = 1L;

    private final Session userSession;
    private final Session rootSession;
    private final WorkflowContext workflowContext;
    private final Node subject;

    public FolderWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
      throws RemoteException, RepositoryException {
        this.workflowContext = context;
        this.userSession = userSession;
        this.rootSession = rootSession;
        this.subject = rootSession.getNodeByIdentifier(subject.getIdentifier());
    }

    public Map<String,Serializable> hints() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Map<String,Serializable> info = new TreeMap<String,Serializable>();
        info.put("add", true);
        info.put("list", false);
        info.put("archive", true);
        info.put("delete", true);
        info.put("rename", true);
        info.put("copy", true);
        info.put("duplicate", true);
        info.put("move", true);
        info.put("prototypes", (Serializable) prototypes());

        if (subject.getPrimaryNodeType().hasOrderableChildNodes()) {

            NodeIterator nodes = subject.getNodes();
            boolean isEnabled = false;
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node.isNodeType(HippoNodeType.NT_HANDLE) || node.isNodeType(HippoStdNodeType.NT_FOLDER)) {
                    isEnabled = true;
                    break;
                }
            }
            info.put("reorder", isEnabled);
        }
        return info;
    }

    /**
     * @deprecated
     */
    public Map<String, Set<String>> list() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return prototypes();
    }

    protected Map<String, Set<String>> prototypes() throws RepositoryException {
        Map<String, Set<String>> types = new LinkedHashMap<String, Set<String>>();
        try {
            QueryManager qmgr = userSession.getWorkspace().getQueryManager();
            Vector<Node> foldertypes = new Vector<Node>();
            Node templates = userSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates");
            Value[] foldertypeRefs = null;
            if (subject.hasProperty("hippostd:foldertype")) {
                try {
                    foldertypeRefs = subject.getProperty("hippostd:foldertype").getValues();
                    for (int i = 0; i < foldertypeRefs.length; i++) {
                        String foldertype = foldertypeRefs[i].getString();
                        if (templates.hasNode(foldertype)) {
                            foldertypes.add(templates.getNode(foldertype));
                        } else {
                            log.warn("Unknown folder type " + foldertype);
                        }
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
                            if (typeNode.getName().equals("hipposysedit:prototype")) {
                                String documentType = typeNode.getPrimaryNodeType().getName();
                                if (!documentType.startsWith("hippo:") && !documentType.startsWith("hipposys:") && !documentType.startsWith("hipposysedit:") && !documentType.startsWith("reporting:")
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

    private void populateRenames(Map<String, String[]> renames, String[] values, Node target, 
            Map<String, String> arguments) throws ValueFormatException, IllegalStateException, RepositoryException,
            WorkflowException {
        String name = arguments.get("name");
        String currentTime = null;
        for (int i = 0; i + 1 < values.length; i += 2) {
            String newValue = values[i + 1];
            if (newValue.equals("$name")) {
                newValue = name;
            } else if (newValue.equals("$holder")) {
                newValue = workflowContext.getUserIdentity();
            } else if (newValue.equals("$now")) {
                if (currentTime == null) {
                    currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                            .format(new java.util.Date());
                    currentTime = currentTime.substring(0, currentTime.length() - 2) + ":"
                            + currentTime.substring(currentTime.length() - 2);
                }
                newValue = currentTime;
            } else if (newValue.startsWith("$inherit")) {
                String relpath = values[i];
                String propPath = relpath.substring(relpath.lastIndexOf('/') + 1);
                if (arguments.containsKey(propPath)) {
                    newValue = arguments.get(propPath);
                } else {
                    final HierarchyResolver hr = ((HippoWorkspace) rootSession.getWorkspace()).getHierarchyResolver();
                    Property parentProperty = hr.getProperty(target, propPath);
                    if (parentProperty == null) {
                        continue;
                    } else {
                        newValue = parentProperty.getValue().getString();
                    }
                }
            } else if (newValue.startsWith("$uuid")) {
                newValue = UUID.randomUUID().toString();
            } else if (newValue.startsWith("$")) {
                String key = newValue.substring(1);
                if (arguments.containsKey(key)) {
                    newValue = arguments.get(key);
                } else {
                    newValue = null;
                }
            }
            if (renames.containsKey(values[i])) {
                String[] oldValues = renames.get(values[i]);
                String[] newValues = new String[oldValues.length + 1];
                System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                newValues[oldValues.length] = newValue;
                renames.put(values[i], newValues);
            } else {
                if (newValue == null) {
                    renames.put(values[i], null);
                } else {
                    renames.put(values[i], new String[] { newValue });
                }
            }
        }
    }

    public String add(String category, String template, String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Map<String,String> arguments = new TreeMap<String,String>();
        arguments.put("name", name);
        return add(category, template, arguments);
    }

    public String add(String category, String template, Map<String,String> arguments) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String name = arguments.get("name");
        rootSession.save();
        QueryManager qmgr = userSession.getWorkspace().getQueryManager();
        Node foldertype = userSession.getRootNode().getNode("hippo:configuration/hippo:queries/hippo:templates");
        if (!foldertype.hasNode(category)) {
            throw new WorkflowException("No category defined for add to folder");
        }
        foldertype = foldertype.getNode(category);
        Query query = qmgr.getQuery(foldertype);
        query = qmgr.createQuery(foldertype.getProperty("jcr:statement").getString(), query.getLanguage());
        QueryResult rs = query.execute();
        Node result = null;
        Node target = rootSession.getRootNode();
        if (!subject.getPath().substring(1).equals("")) {
            target = target.getNode(subject.getPath().substring(1));
        }
        Map<String, String[]> renames = new TreeMap<String, String[]>();
        if (foldertype.hasProperty("hippostd:modify")) {
            Value[] values = foldertype.getProperty("hippostd:modify").getValues();
            String[] params = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                params[i] = values[i].getString();
            }
            populateRenames(renames, params, target, arguments);
        }
        try {
            Node handleNode = null;
            for (NodeIterator iter = rs.getNodes(); iter.hasNext();) {
                Node prototypeNode = iter.nextNode();
                prototypeNode = rootSession.getNode(prototypeNode.getPath());
                if (prototypeNode.getName().equals("hipposysedit:prototype")) {
                    String documentType = prototypeNode.getPrimaryNodeType().getName();
                    if (documentType.equals(template)) {
                        // create handle ourselves, if not already exists
                        if (!target.hasNode(name) || !target.getNode(name).isNodeType(HippoNodeType.NT_HANDLE)
                                || target.getNode(name).hasNode(name)) {
                            result = target.addNode(name, "hippo:handle");
                            result.addMixin(HippoNodeType.NT_HARDHANDLE);
                        } else {
                            result = target.getNode(name);
                        }
                        handleNode = result;
                        renames.put("./_name", new String[] {name});
                        result = copy(prototypeNode, result, renames, ".");
                        break;
                    }
                } else if (prototypeNode.getName().equals(template)) {
                    result = copy(prototypeNode, target, renames, ".");
                    if (result.isNodeType(HippoNodeType.NT_HANDLE)) {
                        handleNode = result;
                        if (result.hasNode(result.getName())) {
                            result = result.getNode(result.getName());
                        } else {
                            result = null;
                        }
                    }
                    break;
                }
            }
            if (result != null) {
                if (handleNode != null && result.isNodeType(HippoNodeType.NT_DOCUMENT)
                        && !result.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)) {
                    result.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[0]);
                }
                rootSession.save();
                return result.getPath();
            } else if (handleNode != null) {
                rootSession.save();
                return handleNode.getPath();
            } else {
                throw new WorkflowException("No template defined for add to folder");
            }
        } finally {
            rootSession.refresh(false);
        }
    }

    private void doArchive(String source, String destination) throws RepositoryException {
        rootSession.move(source, destination);
        rootSession.save();
        String targetParentPath = destination.substring(0, destination.lastIndexOf("/"));
        String targetName = destination.substring(destination.lastIndexOf("/")+1);
        for (final Node target : new NodeIterable(rootSession.getNode(targetParentPath).getNodes(targetName))) {
            try {
                if (target.isNodeType(HippoNodeType.NT_HANDLE) && target.hasNodes()) {
                    JcrUtils.ensureIsCheckedOut(target, false);
                    Node unpublished = null;
                    for (final Node child : new NodeIterable(target.getNodes(target.getName()))) {
                        final String state = JcrUtils.getStringProperty(child, HippoStdNodeType.HIPPOSTD_STATE, null);
                        if (HippoStdNodeType.UNPUBLISHED.equals(state)) {
                            unpublished = child;
                        } else {
                            child.remove();
                        }
                    }
                    rootSession.save();
                    if (unpublished != null) {
                        final VersionManager versionManager = rootSession.getWorkspace().getVersionManager();
                        versionManager.checkpoint(unpublished.getPath());
                        clear(unpublished);
                        unpublished.setPrimaryType(HippoNodeType.NT_DELETED);
                        rootSession.save();
                    }
                }
            } catch(RepositoryException ex) {
                log.error("error while deleting document variants from attic", ex);
            }
        }
    }

    private void clear(final Node node) throws RepositoryException {
        for (Property property : new PropertyIterable(node.getProperties())) {
            if (!property.getDefinition().isProtected()) {
                property.remove();
            }
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            if (!child.getDefinition().isProtected()) {
                child.remove();
            }
        }
        for (NodeType nodeType : node.getMixinNodeTypes()) {
            if (!nodeType.getName().equals(HippoNodeType.NT_HARDDOCUMENT)) {
                node.removeMixin(nodeType.getName());
            }
        }
    }

    public void archive(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String atticPath = null;
        RepositoryMap config = workflowContext.getWorkflowConfiguration();
        if(config.exists() && config.containsKey("attic") && config.get("attic") instanceof String) {
            atticPath = (String) config.get("attic");
        }
        if (atticPath == null) {
            throw new WorkflowException("No attic for archivation defined");
        }
        if (!rootSession.nodeExists(atticPath)) {
            throw new WorkflowException("Attic " + atticPath + " for archivation does not exist");
        }
        if(name.startsWith("/")) {
            name  = name.substring(1);
        }
        String path = subject.getPath();
        Node folder = rootSession.getNode(path);
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            if (offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                offspring = offspring.getParent();
            }
            if (subject.getPath().equals(atticPath)) {
                offspring.remove();
                folder.save();
            } else {
                doArchive(folder.getPath() + "/" + offspring.getName(),
                        atticPath + "/" + atticName(atticPath, offspring.getName(), true));            }
        }
    }

    public void archive(Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String atticPath = null;
        RepositoryMap config = workflowContext.getWorkflowConfiguration();
        if(config.exists() && config.get("attic") instanceof String) {
            atticPath = (String) config.get("attic");
        }
        if (atticPath == null) {
            throw new WorkflowException("No attic for archivation defined");
        }
        String path = subject.getPath();
        Node folderNode = rootSession.getNode(path);
        Node documentNode = rootSession.getNodeByIdentifier(document.getIdentity());
        if (documentNode.getPath().startsWith(path+"/")) {
            if (documentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && documentNode.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                documentNode = documentNode.getParent();
            }
            if (subject.getPath().equals(atticPath)) {
                documentNode.remove();
                folderNode.save();
            } else {
                doArchive(documentNode.getPath(), atticPath + "/" + atticName(atticPath, documentNode.getName(), true));
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
       Node folder = rootSession.getNodeByUUID(subject.getUUID());
       for (String item : list) {
           Node head = folder.getNodes().nextNode();
           if (!head.isSame(folder.getNode(item))) {
               folder.orderBefore(item, head.getName());
           }
       }
       folder.save();
    }

    public void delete(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            delete(folder, offspring);
        }
    }

    private void delete(Node folder, Node offspring) throws RepositoryException, WorkflowException {
        if (offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            offspring = offspring.getParent();
        }
        if (!offspring.isNodeType(HippoNodeType.NT_HANDLE)) {
            for (NodeIterator iter = offspring.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();
                NodeDefinition nd = child.getDefinition();
                if (!nd.getDeclaringNodeType().isMixin()) {
                    throw new WorkflowException("Folder is not empty; cannot be deleted");
                }
            }
        }
        offspring.remove();
        folder.getSession().save();
    }

    public void delete(Document document) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folderNode = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        Node documentNode = rootSession.getNodeByIdentifier(document.getIdentity());
        if (documentNode.getPath().startsWith(folderNode.getPath()+"/")) {
            delete(folderNode, documentNode);
        }
    }

    private void renameChildDocument(Node folderNode, String newName) throws RepositoryException {
        Node documentNode = folderNode.getSession().getRootNode().getNode(folderNode.getPath().substring(1)+"/"+newName);
        renameChildDocument(documentNode);
    }

    private void renameChildDocument(Node documentNode) throws RepositoryException {
        if (documentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            JcrUtils.ensureIsCheckedOut(documentNode, false);
            for (NodeIterator children = documentNode.getNodes(); children.hasNext(); ) {
                Node child = children.nextNode();
                if (child != null) {
                    if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        JcrUtils.ensureIsCheckedOut(child, false);
                        documentNode.getSession().move(child.getPath(), documentNode.getPath()+"/"+documentNode.getName());
                    }
                }
            }
        }
    }

    public void rename(String name, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        Node folder = rootSession.getNode(subject.getPath());
        if (folder.hasNode(name)) {
            if (folder.hasNode(newName)) {
                throw new WorkflowException("Cannot rename document to same name");
            }
            Node offspring = folder.getNode(name);
            if(offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(HippoNodeType.NT_HANDLE))  {
                offspring = offspring.getParent();
            }
            offspring.checkout();
            String nextSiblingRelPath = folder.getPrimaryNodeType().hasOrderableChildNodes() ? findNextSiblingRelPath(offspring) : null;
            rootSession.move(offspring.getPath(), folder.getPath()+"/"+newName);
            renameChildDocument(folder, newName);
            if (nextSiblingRelPath != null) {
                String offspringRelPath = offspring.getName() + "[" + offspring.getIndex() + "]";
                folder.orderBefore(offspringRelPath, nextSiblingRelPath);
            }
            folder.save();
        }
    }

    public void rename(Document document, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node folderNode = rootSession.getNode(subject.getPath());
        Node documentNode = rootSession.getNodeByIdentifier(document.getIdentity());
        if(documentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && documentNode.getParent().isNodeType(HippoNodeType.NT_HANDLE))  {
            documentNode = documentNode.getParent();
        }
        if (documentNode.getPath().startsWith(folderNode.getPath()+"/")) {
            if (folderNode.hasNode(newName)) {
                throw new WorkflowException("Cannot rename document to same name");
            }
            documentNode.checkout();
            String nextSiblingRelPath = folderNode.getPrimaryNodeType().hasOrderableChildNodes() ? findNextSiblingRelPath(documentNode) : null;
            rootSession.move(documentNode.getPath(), folderNode.getPath()+"/"+newName);
            renameChildDocument(folderNode, newName);
            if (nextSiblingRelPath != null) {
                String documentNodeRelPath = documentNode.getName() + "[" + documentNode.getIndex() + "]";
                folderNode.orderBefore(documentNodeRelPath, nextSiblingRelPath);
            }
            folderNode.save();
        }
    }

    private static String findNextSiblingRelPath(Node node) {
        try {
            Node parentNode = node.getParent();
            for (NodeIterator siblings = parentNode.getNodes(); siblings.hasNext(); ) {
                if (siblings.nextNode().isSame(node)) {
                    if (siblings.hasNext()) {
                        final Node nextSibling = siblings.nextNode();
                        return nextSibling.getName() + "[" + nextSibling.getIndex() + "]";
                    }
                    return null;
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    static Node copy(Node source, Node target, Map<String, String[]> renames, String path) throws RepositoryException {
        String[] renamed;
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
                primaryType = expand(renamed, source, PropertyType.NAME)[0].getString();
                if (primaryType.equals("")) {
                    primaryType = null;
                }
            } else {
                primaryType = null;
            }
        }

        boolean found = false;
        if (target.hasNode(name)) {
            Node candidate = target.getNode(name);
            if (candidate.getDefinition().isAutoCreated()) {
                target = candidate;
                found = true;
            }
        }
        if (!found) {
            if (primaryType != null) {
                target = target.addNode(name, primaryType);
            } else {
                target = target.addNode(name);
            }
        }

        if(source.hasProperty("jcr:mixinTypes")) {
            Value[] mixins = source.getProperty("jcr:mixinTypes").getValues();
            for(int i=0; i<mixins.length; i++) {
                target.addMixin(mixins[i].getString());
            }
        }

        if (renames.containsKey(path+"/jcr:mixinTypes")) {
            Value[] values = expand(renames.get(path+"/jcr:mixinTypes"), source, PropertyType.NAME);
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
            if(child instanceof HippoNode && !((HippoNode)child).isSame(((HippoNode)child).getCanonicalNode())) {
                continue;
            }
            String childPath;
            if (renames.containsKey(path + "/_node/_name") && !renames.containsKey(path + "/" + child.getName())) {
                childPath = path + "/_node";
            } else {
                childPath = path + "/" + child.getName();
            }
            copy(child, target, renames, childPath);
        }

        for (PropertyIterator iter = source.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if (prop.getDefinition().isMultiple()) {
                boolean isProtected = true;
                for (int i = 0; i < nodeTypes.length; i++) {
                    PropertyDefinition matchingDefinition = null;
                    for (PropertyDefinition def : nodeTypes[i].getPropertyDefinitions()) {
                        if (def.getRequiredType() == PropertyType.UNDEFINED || def.getRequiredType() == prop.getType()) {
                            if (def.getName().equals("*")) {
                                if (!def.isProtected()) {
                                    matchingDefinition = def;
                                }
                                // now continue because there may be a more limiting definition
                            } else if (def.getName().equals(prop.getName())) {
                                matchingDefinition = def;
                                break;
                            }
                        }
                    }
                    if (matchingDefinition != null && !matchingDefinition.isProtected()) {
                        isProtected = false;
                    }
                }
                for(int i=0; i<nodeTypes.length; i++) {
                    PropertyDefinition[] propDefs = nodeTypes[i].getPropertyDefinitions();
                    for(int j=0; j<propDefs.length; j++) {
                        if (propDefs[j].getName().equals(prop.getName()) && propDefs[j].isProtected()) {
                            isProtected = true;
                        }
                    }
                }
                if (!isProtected) {
                    if(renames.containsKey(path+"/"+prop.getName()) && renames.get(path+"/"+prop.getName()) != null) {
                        target.setProperty(prop.getName(), expand(renames.get(path+"/"+prop.getName()), source, prop.getDefinition().getRequiredType()), prop.getType());
                    } else {
                        Value[] values = prop.getValues();
                        List<Value> newValues = new LinkedList<Value>();
                        for (int i = 0; i < values.length; i++) {
                            String key = path+"/"+prop.getName()+"["+i+"]";
                            if (renames.containsKey(key)) {
                                for (Value substitute : expand(renames.get(key), source, prop.getDefinition().getRequiredType())) {
                                    newValues.add(substitute);
                                }
                            } else {
                                newValues.add(values[i]);
                            }
                        }
                        target.setProperty(prop.getName(), newValues.toArray(new Value[newValues.size()]), prop.getType());
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
                        if (propDefs[j].getName().equals(prop.getName()) && propDefs[j].isProtected()) {
                            isProtected = true;
                        }
                    }
                }
                if (!isProtected) {
                    if(renames.containsKey(path+"/"+prop.getName()) && renames.get(path+"/"+prop.getName()) != null) {
                        Value[] newValues = expand(renames.get(path+"/"+prop.getName()), source, prop.getDefinition().getRequiredType());
                        if(newValues.length >= 1) {
                            target.setProperty(prop.getName(), newValues[0]);
                        } else {
                            target.setProperty(prop.getName(), (String)null);
                        }
                    } else {
                        target.setProperty(prop.getName(), prop.getValue());
                    }
                }
            }
        }

        return target;
    }

    static private Value[] expand(String[] values, Node source, int propertyType) throws RepositoryException {
        Vector<Value> newValues = new Vector<Value>();
        for(int i=0; i<values.length; i++) {
            String value = values[i];
            if(value == null) {
                continue;
            }
            if(value.startsWith("${") && value.endsWith("}")) {
                value = value.substring(2,value.length()-1);
                Property p = source.getProperty(value);
                if(p.getDefinition().isMultiple()) {
                    Value[] referencedValues = p.getValues();
                    for (int j = 0; j < referencedValues.length; j++) {
                        newValues.add(referencedValues[j]);
                    }
                } else {
                    newValues.add(p.getValue());
                }
            } else {
                switch(propertyType) {
                    case PropertyType.DATE:
                        newValues.add(source.getSession().getValueFactory().createValue(value, propertyType));
                        break;
                    default:
                        newValues.add(source.getSession().getValueFactory().createValue(value));
                }
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
        Node source = rootSession.getNodeByIdentifier(offspring.getIdentity());
        return duplicate(source, "Copy of "+source.getName());
    }

    public Document duplicate(String relPath, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = subject.getNode(relPath);
        return duplicate(source, "Copy of "+arguments.get("name"));
    }

    public Document duplicate(Document offspring, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node source = rootSession.getNodeByUUID(offspring.getIdentity());
        String targetName = arguments.get("name");
        return duplicate(source, targetName);
    }

    private Document duplicate(Node source, String targetName) throws WorkflowException, RepositoryException {
        if (subject.hasNode(targetName)) {
            throw new WorkflowException("Cannot duplicate document when duplicate already exists");
        }
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            Node handle = subject.addNode(targetName, HippoNodeType.NT_HANDLE);
            handle.addMixin(HippoNodeType.NT_HARDHANDLE);

            Node document = copyDocument(targetName, Collections.EMPTY_MAP, source, handle);

            renameChildDocument(handle);
            rootSession.save();
            return new Document(document);
        } else {
            renameChildDocument(JcrUtils.copy(source, targetName, subject));
            subject.save();
            return new Document(subject.getNode(targetName));
        }
    }

    /*
     * implemented copy semantics:
     * - when offspring is a variant, create a new bonsai tree with a copy of the variant
     * - when it is a handle, copy it with all its variants; the target folder should be different
     * - otherwise, copy recursively
     */

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
        return copyFrom(new Document(source), new Document(target), absPath.substring(absPath.lastIndexOf("/")+1), arguments);
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
        return moveFrom(new Document(source), new Document(target), absPath.substring(absPath.lastIndexOf("/")+1), arguments);
    }
    public Document move(Document offspring, Document targetFolder, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return moveFrom(offspring, targetFolder, targetName, arguments);
    }

    public Document copyFrom(Document offspring, Document targetFolder, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        if (targetName == null || targetName.equals("")) {
            throw new WorkflowException("No target name given");
        }
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        Node destination = rootSession.getNodeByUUID(targetFolder.getIdentity());
        Node source = rootSession.getNodeByUUID(offspring.getIdentity());
        if (folder.isSame(destination)) {
            //throw new WorkflowException("Cannot copy document to same folder, use duplicate instead");
            return duplicate(source, targetName);
        }
        if (source.getAncestor(folder.getDepth()).isSame(folder)) {
            return ((EmbedWorkflow)workflowContext.getWorkflowContext(null).getWorkflow("embedded", new Document(destination))).copyTo(new Document(subject), offspring, targetName, arguments);
        }
        return null;
    }

    public Document copyTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        if (targetName == null || targetName.equals("")) {
            throw new WorkflowException("No target name given");
        }
        if (folder.hasNode(targetName)) {
            throw new WorkflowException("Cannot copy document when document with same name exists");
        }
        Node source = rootSession.getNodeByUUID(offspring.getIdentity());
        if (!folder.isCheckedOut()) {
            folder.checkout();
        }
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            Node handle = folder.addNode(targetName, HippoNodeType.NT_HANDLE);
            handle.addMixin(HippoNodeType.NT_HARDHANDLE);

            Node document = copyDocument(targetName, (arguments == null ? Collections.<String, String>emptyMap() : arguments), source, handle);
            renameChildDocument(handle);

            folder.save();
            return new Document(document);
        } else {
            renameChildDocument(JcrUtils.copy(source, targetName, folder));
            folder.save();
            return new Document(folder.getNode(targetName));
        }
    }

    protected Node copyDocument(String targetName, Map<String, String> arguments, Node source, Node handle)
            throws WorkflowException, ValueFormatException, RepositoryException {
        RepositoryMap config = workflowContext.getWorkflowConfiguration();
        Object modifyOnCopy = config.get("modify-on-copy");
        Map<String, String[]> renames = new TreeMap<String, String[]>();
        if (arguments.containsKey("name")) {
            log.warn("Arguments key 'name' ({}) is ignored, using targetName ({}) instead", arguments.get("name"), targetName);
        }
        renames.put("name", new String[] { targetName });
        if (modifyOnCopy != null) {
            String[] params;
            if (modifyOnCopy instanceof String[]) {
                params = (String[]) modifyOnCopy;
            } else if (modifyOnCopy instanceof String) {
                params = new String[] {(String) modifyOnCopy};
            } else {
                throw new WorkflowException("Invalid workflow configuration; expected multi-valued String for property modify-on-copy");
            }
            populateRenames(renames, params, handle, arguments);
        }
        return copy(source, handle, renames, ".");
    }

    public Document copyOver(Node destination, Document offspring, Document result, Map<String,String> arguments) {
        return result;
    }

    public Document moveFrom(Document offspring, Document targetFolder, String targetName, Map<String,String> arguments)
        throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        Node destination = rootSession.getNodeByUUID(targetFolder.getIdentity());
        if (folder.isSame(destination)) {
            throw new WorkflowException("Cannot move document to same folder");
        }
        Node source = rootSession.getNodeByUUID(offspring.getIdentity());
        if (!folder.isCheckedOut()) {
            folder.checkout();
        }
        if (source.getAncestor(folder.getDepth()).isSame(folder)) {
            ((EmbedWorkflow)workflowContext.getWorkflow("internal", new Document(destination))).moveTo(new Document(subject), offspring, targetName, arguments);
        }
        return null;
    }

    public Document moveTo(Document sourceFolder, Document offspring, String targetName, Map<String,String> arguments) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String path = subject.getPath();
        Node folder = rootSession.getNode(path);
        if (folder.hasNode(targetName)) {
            throw new WorkflowException("Cannot move document when document with same name exists");
        }
        Node source = rootSession.getNodeByIdentifier(offspring.getIdentity());
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
            source = source.getParent();
        }

        JcrUtils.ensureIsCheckedOut(folder, false);

        folder.getSession().move(source.getPath(), folder.getPath() + "/" + targetName);
        renameChildDocument(folder, targetName);
        rootSession.save();
        ((EmbedWorkflow)workflowContext.getWorkflow("embedded", sourceFolder)).moveOver(folder, offspring, new Document(folder.getNode(targetName)), arguments);
        return new Document(folder.getNode(targetName));
    }

    public Document moveOver(Node destination, Document offspring, Document result, Map<String,String> arguments) {
        return result;
    }
}
