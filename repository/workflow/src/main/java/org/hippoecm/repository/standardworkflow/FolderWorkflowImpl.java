/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Folder;
import org.hippoecm.repository.api.HierarchyResolver;
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
import org.onehippo.repository.util.DateMathParser;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_CHANNEL_ID;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_EXCLUDE_PRIMARY_TYPES;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_MODIFY;
import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.hippoecm.repository.HippoStdNodeType.NT_XPAGE_FOLDER;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROTOTYPE;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.onehippo.repository.security.StandardPermissionNames.HIPPO_AUTHOR;

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
 *   <li><pre>$now: the current time
 *         optional math functions may be used along with the $now option
 *         time units supported include:
 *                Y   - Year
 *                M   - Month
 *                D   - Day
 *                H   - Hour
 *                MIN - Minute
 *                SEC - Second
 *                MIL - Millisecond
 *         Some example usages are:
 *           $now/H
 *              ... Round to the start of the current hour
 *           $now/D
 *              ... Round to the start of the current day
 *           $now+2Y
 *              ... Exactly two years in the future from now
 *           $now-1D
 *              ... Exactly 1 day prior to now
 *           $now/D+6M+3D
 *              ... 6 months and 3 days in the future from the start of
 *                  the current day
 *           $now+6M+3D/D
 *              ... 6 months and 3 days in the future from now, rounded
 *                  down to nearest day</pre>
 *   <li>$holder: the user of the session that invoked the workflow
 *   <li>$inherit: the value under the same path in the parent document.
 *     Can be overridden by providing the property name as an argument.
 *   <li>$&lt;other&gt;: value specified as an argument
 * </ul>
 */
public class FolderWorkflowImpl implements FolderWorkflow, EmbedWorkflow, InternalWorkflow {

    public static final String ATTIC = "attic";
    public static final String COPY_OF = "Copy of ";
    public static final String USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER = "User lacks author permission in destination folder";
    private static final Logger log = LoggerFactory.getLogger(FolderWorkflowImpl.class);
    private static final long serialVersionUID = 1L;
    private static final String TEMPLATES_PATH = "/hippo:configuration/hippo:queries/hippo:templates";

    private final Session userSession;
    private final Session rootSession;
    private final WorkflowContext workflowContext;
    private final Node subject;
    private final JCRFolderDAO folderDAO;

    public FolderWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
            throws RepositoryException {
        this.workflowContext = context;
        this.userSession = userSession;
        this.rootSession = rootSession;
        this.subject = rootSession.getNodeByIdentifier(subject.getIdentifier());
        this.folderDAO = new JCRFolderDAO(NT_FOLDER, NT_DIRECTORY);
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

    public Map<String, Serializable> hints() throws RepositoryException {
        Map<String, Serializable> info = new TreeMap<>();
        final Session subjectSession = workflowContext.getSubjectSession();
        final boolean hasSubjectAuthorPermission = subjectSession.hasPermission(subject.getPath(), HIPPO_AUTHOR);
        if (hasSubjectAuthorPermission) {
            info.put("add", true);
            info.put("archive", true);
            info.put("delete", true);
            info.put("rename", true);
            info.put("duplicate", true);
            info.put("move", true);
        }
        info.put("list", false);
        info.put("copy", true);
        info.put("prototypes", (Serializable) prototypes());
        info.put("edit-display-order", true);

        if (subject.getPrimaryNodeType().hasOrderableChildNodes()) {
            boolean isEnabled = false;
            for (Node node : new NodeIterable(subject.getNodes())) {
                if (node.isNodeType(NT_HANDLE) || node.isNodeType(NT_FOLDER)) {
                    isEnabled = true;
                    break;
                }
            }
            if (hasSubjectAuthorPermission) {
                info.put("reorder", isEnabled);
            }
        }
        if (subject.isNodeType(NT_XPAGE_FOLDER)){
            info.put("channelId",  JcrUtils.getStringProperty(subject, HIPPOSTD_CHANNEL_ID, null));
        }
        return info;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Map<String, Set<String>> list() {
        return prototypes();
    }

    protected Map<String, Set<String>> prototypes() {
        Map<String, Set<String>> types = new LinkedHashMap<>();
        try {
            QueryManager qmgr = userSession.getWorkspace().getQueryManager();
            Vector<Node> foldertypes = new Vector<>();
            Node templates = userSession.getNode(TEMPLATES_PATH);
            Value[] foldertypeRefs = null;
            if (subject.hasProperty("hippostd:foldertype")) {
                try {
                    foldertypeRefs = subject.getProperty("hippostd:foldertype").getValues();
                    for (final Value foldertypeRef : foldertypeRefs) {
                        String foldertype = foldertypeRef.getString();
                        if (templates.hasNode(foldertype)) {
                            foldertypes.add(templates.getNode(foldertype));
                        } else {
                            log.warn("Unknown folder type '{}'", foldertype);
                        }
                    }
                } catch (PathNotFoundException | ValueFormatException ex) {
                    foldertypeRefs = null;
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
            if (foldertypeRefs == null) {
                try {
                    for (NodeIterator iter = templates.getNodes(); iter.hasNext(); ) {
                        foldertypes.add(iter.nextNode());
                    }
                } catch (PathNotFoundException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }

            for (Node foldertype : foldertypes) {
                try {
                    Set<String> prototypes = new TreeSet<>();
                    if (foldertype.isNodeType("nt:query")) {
                        Query query = qmgr.getQuery(foldertype);
                        query = qmgr.createQuery(foldertype.getProperty("jcr:statement").getString(), query.getLanguage()); // HREPTWO-1266
                        QueryResult rs = query.execute();

                        final Set<String> excludePrimaryTypesSet =
                                Arrays.stream(getMultipleStringProperty(foldertype, HIPPOSTD_EXCLUDE_PRIMARY_TYPES, new String[0]))
                                        .collect(Collectors.toSet());

                        for (NodeIterator iter = rs.getNodes(); iter.hasNext(); ) {
                            Node typeNode = iter.nextNode();
                            if (excludePrimaryTypesSet.contains(typeNode.getPrimaryNodeType().getName())) {
                                log.info("Skip prototype '{}' since excluded by '{}'", typeNode.getPath(),
                                        foldertype.getPath() + "/@" + HIPPOSTD_EXCLUDE_PRIMARY_TYPES);

                            } else if (typeNode.getName().equals(HIPPO_PROTOTYPE)) {
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
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        return types;
    }

    private void populateRenames(Map<String, String[]> renames, String[] values, Node target,
                                 Map<String, String> arguments) throws IllegalStateException, RepositoryException {
        String name = arguments.get("name");
        String currentTime;
        for (int i = 0; i + 1 < values.length; i += 2) {
            String newValue = values[i + 1];
            String[] newValues = null;
            if (newValue == null) {
                continue;
            } else if (newValue.equals("$name")) {
                newValues = new String[]{name};
            } else if (newValue.equals("$holder")) {
                newValues = new String[]{workflowContext.getUserIdentity()};
            } else if (newValue.startsWith("$now")) {
                try {
                    Date dateVal = DateMathParser.parseMath(newValue.substring(4)).getTime();
                    currentTime = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(dateVal);
                    currentTime = currentTime.substring(0, currentTime.length() - 2) + ":"
                            + currentTime.substring(currentTime.length() - 2);
                    newValues = new String[]{currentTime};
                } catch (Exception ex) {
                    log.error("error while populating default date/time value for: {} , property: {}", name, values[i],
                            ex);
                }
            } else if (newValue.startsWith("$inherit")) {
                String relpath = values[i];
                String propPath = relpath.substring(relpath.lastIndexOf('/') + 1);
                if (arguments.containsKey(propPath)) {
                    newValues = new String[]{arguments.get(propPath)};
                } else {
                    final HierarchyResolver hr = ((HippoWorkspace) rootSession.getWorkspace()).getHierarchyResolver();
                    Property parentProperty = hr.getProperty(target, propPath);
                    if (parentProperty == null) {
                        continue;
                    } else {
                        if (parentProperty.isMultiple()) {
                            final Value[] parentPropertyValues = parentProperty.getValues();
                            newValues = new String[parentPropertyValues.length];
                            for (int j = 0; j < parentPropertyValues.length; j++) {
                                newValues[j] = parentPropertyValues[j].getString();
                            }
                        } else {
                            newValues = new String[]{parentProperty.getValue().getString()};
                        }
                    }
                }
            } else if (newValue.startsWith("$uuid")) {
                newValues = new String[]{UUID.randomUUID().toString()};
            } else if (newValue.startsWith("$")) {
                String key = newValue.substring(1);
                if (arguments.containsKey(key)) {
                    newValues = new String[]{arguments.get(key)};
                }
            } else {
                newValues = new String[]{newValue};
            }
            if (newValues != null) {
                String[] oldValues = renames.get(values[i]);
                if (oldValues != null) {
                    String[] tmpValues = newValues;
                    newValues = new String[oldValues.length + newValues.length];
                    System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                    System.arraycopy(tmpValues, 0, newValues, oldValues.length, tmpValues.length);
                }
                renames.put(values[i], newValues);
            }
        }
    }

    public String add(final String category, final String type, final String relPath) throws WorkflowException, RepositoryException, RemoteException {
        return add(category, type, relPath, null);
    }

    @Override
    public String add(final String category, final String type, final String relPath, final JcrTemplateNode jcrTemplateNode) throws WorkflowException, RepositoryException, RemoteException {
        Map<String, String> arguments = new TreeMap<>();
        arguments.put("name", relPath);
        return add(category, type, arguments, jcrTemplateNode);
    }

    public String add(final String category, final String type, final Map<String, String> arguments) throws WorkflowException, RepositoryException {
        return add(category, type, arguments, null);
    }

    public String add(final String category, final String type, final Map<String, String> arguments, final JcrTemplateNode jcrTemplateNode) throws WorkflowException, RepositoryException {
        String name = arguments.get("name");

        final QueryManager qmgr = userSession.getWorkspace().getQueryManager();
        final Node queryFolder = userSession.getNode(TEMPLATES_PATH);
        if (!queryFolder.hasNode(category)) {
            throw new WorkflowException("No template query called '" + category + "' at " + TEMPLATES_PATH);
        }
        final Node templateQuery = queryFolder.getNode(category);
        Query query = qmgr.getQuery(templateQuery);
        query = qmgr.createQuery(templateQuery.getProperty("jcr:statement").getString(), query.getLanguage());
        QueryResult rs = query.execute();

        Node result = null;
        final Node target = rootSession.getNodeByIdentifier(subject.getIdentifier());
        Map<String, String[]> renames = new TreeMap<>();
        if (templateQuery.hasProperty(HIPPOSTD_MODIFY)) {
            Value[] values = templateQuery.getProperty(HIPPOSTD_MODIFY).getValues();
            String[] params = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                params[i] = values[i].getString();
            }
            populateRenames(renames, params, target, arguments);
        }

        try {
            Node handleNode = null;
            for (Node prototypeNode : new NodeIterable(rs.getNodes())) {
                prototypeNode = rootSession.getNodeByIdentifier(prototypeNode.getIdentifier());
                if (prototypeNode.getName().equals(HIPPO_PROTOTYPE)) {
                    String documentType = prototypeNode.getPrimaryNodeType().getName();
                    if (documentType.equals(type)) {
                        // create handle ourselves, if not already exists
                        if (!target.hasNode(name) || !target.getNode(name).isNodeType(NT_HANDLE)
                                || target.getNode(name).hasNode(name)) {
                            result = target.addNode(name, NT_HANDLE);
                            result.addMixin(JcrConstants.MIX_REFERENCEABLE);
                        } else {
                            result = target.getNode(name);
                        }
                        handleNode = result;
                        renames.put("./_name", new String[]{name});
                        final ExpandingCopyHandler handler = new ExpandingCopyHandler(handleNode, renames, rootSession.getValueFactory());
                        result = JcrUtils.copyTo(prototypeNode, handler);
                        if (!result.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                            result.addMixin(JcrConstants.MIX_REFERENCEABLE);
                        }
                        break;
                    }
                } else if (prototypeNode.getName().equals(type)) {
                    final ExpandingCopyHandler handler = new ExpandingCopyHandler(target, renames, rootSession.getValueFactory());
                    result = JcrUtils.copyTo(prototypeNode, handler);
                    copyXPageFolderMixinAndChannelIdToChild(result);
                    if (result.isNodeType(NT_HANDLE)) {
                        handleNode = result;
                        if (!handleNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                            handleNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                        }
                        if (result.hasNode(result.getName())) {
                            result = result.getNode(result.getName());
                            if (!result.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                                result.addMixin(JcrConstants.MIX_REFERENCEABLE);
                            }
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
                append(result, jcrTemplateNode);
                rootSession.save();
                return result.getPath();
            } else if (handleNode != null) {
                return handleNode.getPath();
            } else {
                throw new WorkflowException("No document or folder was added: the query at " + TEMPLATES_PATH + "/" + category
                        + " did not return a matching prototype for '" + type + "'");
            }
        } finally {
            rootSession.refresh(false);
        }
    }

    private void copyXPageFolderMixinAndChannelIdToChild(final Node result) throws RepositoryException {
        if (subject.isNodeType(NT_XPAGE_FOLDER)) {
            result.addMixin(NT_XPAGE_FOLDER);
            if (subject.hasProperty(HippoStdNodeType.HIPPOSTD_CHANNEL_ID)) {
                String channelId = subject.getProperty(HippoStdNodeType.HIPPOSTD_CHANNEL_ID).getString();
                result.setProperty(HippoStdNodeType.HIPPOSTD_CHANNEL_ID, channelId);
            }
        }
    }

    private void append(Node current, final JcrTemplateNode jcrTemplateNode) throws RepositoryException {
        if (jcrTemplateNode == null) {
            return;
        }
        addMixinsAndProperties(current, jcrTemplateNode);
        for (JcrTemplateNode child : jcrTemplateNode.getChildren()) {
            append(current.addNode(child.getNodeName(), child.getPrimaryNodeType()), child);
        }
    }

    private void addMixinsAndProperties(final Node jcrNode, final JcrTemplateNode jcrTemplateNode) throws RepositoryException {
        for (String mixin : jcrTemplateNode.getMixinNames()) {
            jcrNode.addMixin(mixin);
        }
        for (Map.Entry<String, Value[]> entry : jcrTemplateNode.getMultiValuedProperties().entrySet()) {
            jcrNode.setProperty(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Value> entry : jcrTemplateNode.getSingleValuedProperties().entrySet()) {
            jcrNode.setProperty(entry.getKey(), entry.getValue());
        }
    }


    private void doArchive(final Node handle, final String atticPath) throws RepositoryException {
        rootSession.move(handle.getPath(), atticPath + "/" + atticName(atticPath, handle));
        // make sure to save the changes, otherwise the an InvalidItemStateException is thrown by
        // versionManager.checkpoint below
        rootSession.save();
        try {
            if (handle.isNodeType(NT_HANDLE)) {
                for (final Node child : new NodeIterable(handle.getNodes(handle.getName()))) {
                    if (child.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                        final VersionManager versionManager = rootSession.getWorkspace().getVersionManager();
                        versionManager.checkpoint(child.getPath());
                        clear(child);
                        child.setPrimaryType(HippoNodeType.NT_DELETED);
                        child.setProperty(HippoNodeType.HIPPO_DELETED_DATE, Calendar.getInstance());
                        child.setProperty(HippoNodeType.HIPPO_DELETED_BY, userSession.getUserID());
                    } else {
                        child.remove();
                    }
                    rootSession.save();
                }
            }
        } catch (RepositoryException ex) {
            log.error("error while deleting document variants from attic", ex);
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
            if (!nodeType.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                node.removeMixin(nodeType.getName());
            }
        }
    }

    public void archive(String name) throws WorkflowException, RepositoryException {
        String atticPath = getAtticPath();
        if (!rootSession.nodeExists(atticPath)) {
            throw new WorkflowException("Attic " + atticPath + " for archivation does not exist");
        }
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        String path = subject.getPath();
        Node folder = rootSession.getNode(path);
        if (folder.hasNode(name)) {
            Node offspring = folder.getNode(name);
            if (offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(NT_HANDLE)) {
                offspring = offspring.getParent();
            }
            if (subject.getPath().equals(atticPath)) {
                offspring.remove();
                rootSession.save();
            } else {
                doArchive(offspring, atticPath);
            }
        }
    }

    private String getAtticPath() throws WorkflowException {
        String atticPath = null;
        RepositoryMap config = workflowContext.getWorkflowConfiguration();
        if (config.exists() && config.get(ATTIC) instanceof String) {
            atticPath = (String) config.get(ATTIC);
        }
        if (atticPath == null) {
            throw new WorkflowException("No attic for archivation defined");
        }
        return atticPath;
    }

    public void archive(Document document) throws WorkflowException, RepositoryException, RemoteException {
        String atticPath = getAtticPath();
        String path = subject.getPath();
        Node documentNode = document.getNode(rootSession);
        if (documentNode.getPath().startsWith(path + "/")) {
            if (documentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && documentNode.getParent().isNodeType(NT_HANDLE)) {
                documentNode = documentNode.getParent();
            }
            if (subject.getPath().equals(atticPath)) {
                documentNode.remove();
                rootSession.save();
            } else {
                doArchive(documentNode, atticPath);
            }
        }
    }

    private String atticName(String atticPath, Node handle) throws RepositoryException {
        String handleId = handle.getIdentifier();
        String elt1 = handleId.substring(0, 1);
        String elt2 = handleId.substring(1, 2);
        String elt3 = handleId.substring(2, 3);
        String elt4 = handleId.substring(3, 4);
        Node parent = rootSession.getNode(atticPath);
        for (String pathElement : new String[]{elt1, elt2, elt3, elt4}) {
            if (!parent.hasNode(pathElement)) {
                parent = parent.addNode(pathElement, JcrConstants.NT_UNSTRUCTURED);
            } else {
                parent = parent.getNode(pathElement);
            }
        }
        return elt1 + "/" + elt2 + "/" + elt3 + "/" + elt4 + "/" + handle.getName();
    }

    public void reorder(List<String> newOrder) throws RepositoryException {
        Node folder = rootSession.getNodeByIdentifier(subject.getIdentifier());
        Session session = folder.getSession();

        LinkedList<String> list = new LinkedList<>();
        for (String nodeName : newOrder) {
            Node node = folder.getNode(nodeName);
            list.addFirst(node.getIdentifier());
        }

        for (String identifier : list) {
            Node headNode = folder.getNodes().nextNode();

            if (!headNode.getIdentifier().equals(identifier)) {
                Node srcNode = session.getNodeByIdentifier(identifier);
                String srcNodeName = srcNode.getName() + (srcNode.getIndex() > 1 ? "[" + srcNode.getIndex() + "]" : "");
                String headNodeName = headNode.getName() + (headNode.getIndex() > 1 ? "[" + headNode.getIndex() + "]" : "");
                folder.orderBefore(srcNodeName, headNodeName);
            }
        }
        rootSession.save();
    }

    public void delete(String name) throws WorkflowException, RepositoryException {
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
        if (offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(NT_HANDLE)) {
            offspring = offspring.getParent();
        }
        if (!offspring.isNodeType(NT_HANDLE)) {
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

    public void delete(Document document) throws WorkflowException, RepositoryException {
        String path = subject.getPath().substring(1);
        Node folderNode = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        Node documentNode = document.getNode(rootSession);
        if (documentNode.getPath().startsWith(folderNode.getPath() + "/")) {
            delete(folderNode, documentNode);
        }
    }

    private void renameChildDocument(Node folderNode, String newName) throws RepositoryException {
        Node documentNode = folderNode.getSession().getRootNode().getNode(folderNode.getPath().substring(1) + "/" + newName);
        renameChildDocument(documentNode);
    }

    private void renameChildDocument(Node documentNode) throws RepositoryException {
        if (documentNode.isNodeType(NT_HANDLE)) {
            JcrUtils.ensureIsCheckedOut(documentNode);
            for (NodeIterator children = documentNode.getNodes(); children.hasNext(); ) {
                Node child = children.nextNode();
                if (child != null && child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    JcrUtils.ensureIsCheckedOut(child);
                    documentNode.getSession().move(child.getPath(), documentNode.getPath() + "/" + documentNode.getName());
                }
            }
        }
    }

    public void rename(String name, String newName) throws WorkflowException, RepositoryException {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        Node folder = rootSession.getNode(subject.getPath());
        if (folder.hasNode(name)) {
            if (folder.hasNode(newName)) {
                throw new WorkflowException("Cannot rename document to same name");
            }
            Node offspring = folder.getNode(name);
            if (offspring.isNodeType(HippoNodeType.NT_DOCUMENT) && offspring.getParent().isNodeType(NT_HANDLE)) {
                offspring = offspring.getParent();
            }
            orderBeforeNextSibbling(newName,
                    folder,
                    offspring);
            rootSession.save();
        }
    }

    private void orderBeforeNextSibbling(final String newName,
                                         final Node folder,
                                         final Node offspring) throws RepositoryException {
        checkout(offspring);
        String nextSiblingRelPath =
                folder.getPrimaryNodeType().hasOrderableChildNodes() ? findNextSiblingRelPath(offspring) : null;
        rootSession.move(offspring.getPath(),
                folder.getPath() + "/" + newName);
        renameChildDocument(folder,
                newName);
        if (nextSiblingRelPath != null) {
            String offspringRelPath = offspring.getName() + "[" + offspring.getIndex() + "]";
            folder.orderBefore(offspringRelPath,
                    nextSiblingRelPath);
        }
    }

    public void rename(Document document, String newName) throws WorkflowException, RepositoryException {
        Node folderNode = rootSession.getNode(subject.getPath());
        Node documentNode = document.getNode(rootSession);
        if (documentNode.isNodeType(HippoNodeType.NT_DOCUMENT) && documentNode.getParent().isNodeType(NT_HANDLE)) {
            documentNode = documentNode.getParent();
        }
        if (documentNode.getPath().startsWith(folderNode.getPath() + "/")) {
            if (folderNode.hasNode(newName)) {
                throw new WorkflowException("Cannot rename document to same name");
            }
            orderBeforeNextSibbling(newName,
                    folderNode,
                    documentNode);
            rootSession.save();
        }
    }

    public Document duplicate(String relPath)
            throws WorkflowException, RepositoryException {
        Node source = subject.getNode(relPath);
        return duplicate(source, COPY_OF + source.getName());
    }

    public Document duplicate(Document offspring)
            throws WorkflowException, RepositoryException {
        Node source = offspring.getNode(rootSession);
        return duplicate(source, COPY_OF + source.getName());
    }

    public Document duplicate(String relPath, Map<String, String> arguments)
            throws WorkflowException, RepositoryException {
        Node source = subject.getNode(relPath);
        return duplicate(source, COPY_OF + arguments.get("name"));
    }

    public Document duplicate(Document offspring, Map<String, String> arguments)
            throws WorkflowException, RepositoryException {
        Node source = offspring.getNode(rootSession);
        String targetName = arguments.get("name");
        return duplicate(source, targetName);
    }

    private Document duplicate(Node source, String targetName) throws WorkflowException, RepositoryException {
        final Session subjectSession = workflowContext.getSubjectSession();
        if (!subjectSession.hasPermission(subject.getPath(), HIPPO_AUTHOR)) {
            throw new AccessDeniedException(USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER);
        }
        if (subject.hasNode(targetName)) {
            throw new WorkflowException("Cannot duplicate document when duplicate already exists");
        }
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(NT_HANDLE)) {
            Node targetHandle = subject.addNode(targetName, NT_HANDLE);
            targetHandle.addMixin(JcrConstants.MIX_REFERENCEABLE);

            Node document = copyDocument(targetName, Collections.emptyMap(), source, targetHandle);

            renameChildDocument(targetHandle);
            removeBranchRelatedMixins(targetHandle);
            rootSession.save();
            return new Document(document);
        } else {
            final Node target = JcrUtils.copy(source, targetName, subject);
            if (target != null){
                renameChildDocument(target);
                removeBranchRelatedMixins(target);
            }
            rootSession.save();
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
            throws WorkflowException, RepositoryException, RemoteException {
        return copy(relPath, absPath, null);
    }

    public Document copy(Document offspring, Document targetFolder, String targetName)
            throws WorkflowException, RepositoryException, RemoteException {
        return copy(offspring, targetFolder, targetName, null);
    }

    public Document copy(String relPath, String absPath, Map<String, String> arguments)
            throws WorkflowException, RepositoryException, RemoteException {
        Node source = subject.getNode(relPath);
        Node target = validateSourceAndDestination(absPath,
                source);
        return copyFrom(new Document(source), new Document(target), absPath.substring(absPath.lastIndexOf('/') + 1), arguments);
    }

    private Node validateSourceAndDestination(final String absPath,
                                              final Node source) throws RepositoryException {
        if (!source.isNodeType(HippoNodeType.NT_DOCUMENT) && !source.isNodeType(NT_HANDLE)) {
            throw new MappingException("copied item is not a document");
        }
        Node target = subject.getSession().getRootNode().getNode(absPath.substring(1,
                absPath.lastIndexOf('/')));
        if (!target.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            throw new MappingException("copied destination is not a document");
        }
        return target;
    }

    public Document copy(Document offspring, Document targetFolder, String targetName, Map<String, String> arguments)
            throws WorkflowException, RepositoryException, RemoteException {
        return copyFrom(offspring, targetFolder, targetName, arguments);
    }

    public Document move(String relPath, String absPath)
            throws WorkflowException, RepositoryException, RemoteException {
        return move(relPath, absPath, null);
    }

    public Document move(Document offspring, Document targetFolder, String targetName)
            throws WorkflowException, RepositoryException, RemoteException {
        return move(offspring, targetFolder, targetName, null);
    }

    public Document move(String relPath, String absPath, Map<String, String> arguments)
            throws WorkflowException, RepositoryException, RemoteException {
        Node source = subject.getNode(relPath);
        Node target = validateSourceAndDestination(absPath,
                source);
        return moveFrom(new Document(source), new Document(target), absPath.substring(absPath.lastIndexOf('/') + 1), arguments);
    }

    public Document move(Document offspring, Document targetFolder, String targetName, Map<String, String> arguments)
            throws WorkflowException, RepositoryException, RemoteException {
        return moveFrom(offspring, targetFolder, targetName, arguments);
    }

    @Override
    public Folder get() throws WorkflowException {
        try {
            final Node folderNode = rootSession.getNodeByIdentifier(subject.getIdentifier());
            return folderDAO.get(folderNode);
        } catch (RepositoryException e) {
            final String message = String.format("Failed to read folder node: { path: %s}"
                    , JcrUtils.getNodePathQuietly(subject));
            throw new WorkflowException(message);
        }
    }

    @Override
    public Folder update(final Folder folder) throws WorkflowException {
        if (folder == null) {
            final String message = String.format("Folder should not be null, cannot update node: { path : {%s} }"
                    , JcrUtils.getNodePathQuietly(subject));
            throw new WorkflowException(message);
        }
        try {
            final Node folderNode = rootSession.getNodeByIdentifier(subject.getIdentifier());
            folderDAO.update(folder, folderNode);
            rootSession.save();
            log.debug("update backing node : { path: {}} with folder : {}", subject.getPath(), folder);
            return folder;
        } catch (RepositoryException e) {
            final String message = String.format("Could not update node : { path : {%s} } from folder : %s"
                    , JcrUtils.getNodePathQuietly(subject), folder);
            throw new WorkflowException(message);
        }
    }

    public Document copyFrom(Document offspring, Document targetFolder, String targetName, Map<String, String> arguments)
            throws WorkflowException, RepositoryException, RemoteException {
        if (targetName == null || targetName.equals("")) {
            throw new WorkflowException("No target name given");
        }
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        Node destination = targetFolder.getNode(rootSession);
        Node source = offspring.getNode(rootSession);
        if (folder.isSame(destination)) {
            return duplicate(source, targetName);
        }
        if (source.getAncestor(folder.getDepth()).isSame(folder)) {
            return ((EmbedWorkflow) workflowContext.getWorkflow("embedded", new Document(destination))).copyTo(new Document(subject), offspring, targetName, arguments);
        }
        return null;
    }

    public Document copyTo(Document sourceFolder, Document offspring, String targetName, Map<String, String> arguments) throws WorkflowException, RepositoryException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        final Session subjectSession = workflowContext.getSubjectSession();
        if (!subjectSession.hasPermission(folder.getPath(), HIPPO_AUTHOR)) {
            throw new AccessDeniedException(USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER);
        }
        if (targetName == null || targetName.equals("")) {
            throw new WorkflowException("No target name given");
        }
        if (folder.hasNode(targetName)) {
            throw new WorkflowException("Cannot copy document when document with same name exists");
        }
        Node source = offspring.getNode(rootSession);
        Document copy;
        if (!folder.isCheckedOut()) {
            checkout(folder);
        }
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(NT_HANDLE)) {
            Node handle = folder.addNode(targetName, NT_HANDLE);
            handle.addMixin(JcrConstants.MIX_REFERENCEABLE);

            Node document = copyDocument(targetName, (arguments == null ? Collections.emptyMap() : arguments), source, handle);
            renameChildDocument(handle);
            removeBranchRelatedMixins(handle);
            copy = new Document(document);
        } else {
            final Node target = JcrUtils.copy(source, targetName, folder);
            if (target!=null){
                renameChildDocument(target);
                removeBranchRelatedMixins(target);
            }
            copy = new Document(folder.getNode(targetName));
        }
        rootSession.save();
        return copy;
    }

    protected Node copyDocument(String targetName, Map<String, String> arguments, Node source, Node handle)
            throws WorkflowException, RepositoryException {
        RepositoryMap config = workflowContext.getWorkflowConfiguration();
        Object modifyOnCopy = config.get("modify-on-copy");
        Map<String, String[]> renames = new TreeMap<>();
        if (arguments.containsKey("name")) {
            log.warn("Arguments key 'name' ({}) is ignored, using targetName ({}) instead", arguments.get("name"), targetName);
        }
        renames.put("name", new String[]{targetName});
        if (modifyOnCopy != null) {
            String[] params;
            if (modifyOnCopy instanceof String[]) {
                params = (String[]) modifyOnCopy;
            } else if (modifyOnCopy instanceof String) {
                params = new String[]{(String) modifyOnCopy};
            } else {
                throw new WorkflowException("Invalid workflow configuration; expected multi-valued String for property modify-on-copy");
            }
            populateRenames(renames, params, handle, arguments);
        }

        final ExpandingCopyHandler handler = new ExpandingCopyHandler(handle, renames, rootSession.getValueFactory());
        return JcrUtils.copyTo(source, handler);
    }

    public Document copyOver(Node destination, Document offspring, Document result, Map<String, String> arguments) {
        return result;
    }

    public Document moveFrom(Document offspring, Document targetFolder, String targetName, Map<String, String> arguments)
            throws WorkflowException, RepositoryException, RemoteException {
        String path = subject.getPath().substring(1);
        Node folder = (path.equals("") ? rootSession.getRootNode() : rootSession.getRootNode().getNode(path));
        Node destination = targetFolder.getNode(rootSession);
        if (folder.isSame(destination)) {
            throw new WorkflowException("Cannot move document to same folder");
        }
        Node source = offspring.getNode(rootSession);
        if (!folder.isCheckedOut()) {
            checkout(folder);
        }
        if (source.getAncestor(folder.getDepth()).isSame(folder)) {
            ((EmbedWorkflow) workflowContext.getWorkflow("internal", new Document(destination))).moveTo(new Document(subject), offspring, targetName, arguments);
        }
        return null;
    }

    private void checkout(final Node folder) throws RepositoryException {
        if (folder.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            final VersionManager versionManager = rootSession.getWorkspace().getVersionManager();
            versionManager.checkout(folder.getPath());
        } else {
            log.warn("Unable to checkout node, node : { path : {} } is not versionable", JcrUtils.getNodePathQuietly(subject));
        }
    }

    public Document moveTo(Document sourceFolder, Document offspring, String targetName, Map<String, String> arguments) throws WorkflowException, RepositoryException, RemoteException {
        String path = subject.getPath();
        Node folder = rootSession.getNode(path);
        final Session subjectSession = workflowContext.getSubjectSession();
        if (!subjectSession.hasPermission(folder.getPath(), HIPPO_AUTHOR)) {
            throw new AccessDeniedException(USER_LACKS_AUTHOR_PERMISSION_IN_DESTINATION_FOLDER);
        }
        if (folder.hasNode(targetName)) {
            throw new WorkflowException("Cannot move document when document with same name exists");
        }
        Node source = offspring.getNode(rootSession);
        if (source.isNodeType(HippoNodeType.NT_DOCUMENT) && source.getParent().isNodeType(NT_HANDLE)) {
            source = source.getParent();
        }

        JcrUtils.ensureIsCheckedOut(folder);

        folder.getSession().move(source.getPath(), folder.getPath() + "/" + targetName);
        renameChildDocument(folder, targetName);
        rootSession.save();
        ((EmbedWorkflow) workflowContext.getWorkflow("embedded", sourceFolder)).moveOver(folder, offspring, new Document(folder.getNode(targetName)), arguments);
        return new Document(folder.getNode(targetName));
    }

    public Document moveOver(Node destination, Document offspring, Document result, Map<String, String> arguments) {
        return result;
    }

    private void removeBranchRelatedMixins(Node source) throws RepositoryException {
        if (source.isNodeType(HippoNodeType.NT_HANDLE)) {
            try {
                removeVersionInfoMixin(source);
                removeBranchInfoMixins(source);
            } catch (RepositoryException e) {
                log.warn("Could not remove {} mixin and/or {} mixin and related properties", HippoNodeType.HIPPO_MIXIN_BRANCH_INFO, HippoNodeType.NT_HIPPO_VERSION_INFO);
            }
        } else {
            for (Node child : new NodeIterable(source.getNodes())) {
                removeBranchRelatedMixins(child);
            }
        }
    }

    private void removeBranchInfoMixins(final Node handle) throws RepositoryException {
        final NodeIterator nodes = handle.getNodes(handle.getName());
        while (nodes.hasNext()) {
            final Node variant = nodes.nextNode();
            if (variant.isNodeType(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO)) {
                variant.getProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID).remove();
                variant.getProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME).remove();
                variant.removeMixin(HippoNodeType.HIPPO_MIXIN_BRANCH_INFO);
            }
        }
    }

    private void removeVersionInfoMixin(final Node handle) throws RepositoryException {
        if (handle.isNodeType(HippoNodeType.NT_HIPPO_VERSION_INFO)) {
            handle.removeMixin(HippoNodeType.NT_HIPPO_VERSION_INFO);
        }
    }

}
