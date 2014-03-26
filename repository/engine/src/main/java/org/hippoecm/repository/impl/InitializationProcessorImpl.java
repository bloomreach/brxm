/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;
import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.jackrabbit.HippoCompactNodeTypeDefReader;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.MavenComparableVersion;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.api.ContentResourceLoader;
import org.onehippo.repository.util.FileContentResourceLoader;
import org.onehippo.repository.util.ZipFileContentResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class InitializationProcessorImpl implements InitializationProcessor {


    private static final Logger log = LoggerFactory.getLogger(InitializationProcessorImpl.class);

    private static final String INIT_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH;

    private static final String[] INIT_ITEM_PROPERTIES = new String[] {
            HippoNodeType.HIPPO_SEQUENCE,
            HippoNodeType.HIPPO_NAMESPACE,
            HippoNodeType.HIPPO_NODETYPESRESOURCE,
            HippoNodeType.HIPPO_NODETYPES,
            HippoNodeType.HIPPO_CONTENTRESOURCE,
            HippoNodeType.HIPPO_CONTENT,
            HippoNodeType.HIPPO_CONTENTROOT,
            HippoNodeType.HIPPO_CONTENTDELETE,
            HippoNodeType.HIPPO_CONTENTPROPSET,
            HippoNodeType.HIPPO_CONTENTPROPADD,
            HippoNodeType.HIPPO_RELOADONSTARTUP,
            HippoNodeType.HIPPO_VERSION };

    private final static String GET_INITIALIZE_ITEMS =
            "SELECT * FROM hipposys:initializeitem " +
                    "WHERE jcr:path = '/hippo:configuration/hippo:initialize/%' AND " +
                    HippoNodeType.HIPPO_STATUS + " = 'pending' " +
                    "ORDER BY " + HippoNodeType.HIPPO_SEQUENCE + " ASC";

    private final static String GET_OLD_INITIALIZE_ITEMS = "SELECT * FROM hipposys:initializeitem " +
            "WHERE jcr:path = '/hippo:configuration/hippo:initialize/%' AND (" +
            HippoNodeType.HIPPO_TIMESTAMP + " IS NULL OR " +
            HippoNodeType.HIPPO_TIMESTAMP + " < {})";

    private static XmlPullParserFactory factory;

    static {
        try {
            factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            log.error("Could not get xpp factory instance: " + e.getMessage());
        }
    }

    private Logger logger;

    public InitializationProcessorImpl() {}

    public InitializationProcessorImpl(Logger logger) {
        this.logger = logger;
    }

    public void dryRun(Session session) {
        try {
            Node initializeFolder = session.getRootNode().addNode("initialize");
            session.save();
            loadExtensions(session, initializeFolder, false);
            session.save();
            final List<Node> initializeItems = new ArrayList<Node>();
            final Query getInitializeItems = session.getWorkspace().getQueryManager().createQuery(
                    "SELECT * FROM hipposys:initializeitem " +
                            "WHERE jcr:path = '/initialize/%' AND " +
                            HippoNodeType.HIPPO_STATUS + " = 'pending' " +
                            "ORDER BY " + HippoNodeType.HIPPO_SEQUENCE + " ASC", Query.SQL);
            final NodeIterator nodes = getInitializeItems.execute().getNodes();
            while (nodes.hasNext()) {
                initializeItems.add(nodes.nextNode());
            }
            processInitializeItems(session, initializeItems, true);
            session.refresh(false);
            initializeFolder.remove();
            session.save();
        } catch (IOException | RepositoryException ex) {
            getLogger().error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    @Override
    public List<Node> loadExtensions(final Session session) throws RepositoryException, IOException {
        return loadExtensions(session, session.getNode(INITIALIZATION_FOLDER), true);
    }

    @Override
    public List<Node> loadExtension(final Session session, final URL extension) throws RepositoryException, IOException {
        return loadExtension(extension, session, session.getNode(INITIALIZATION_FOLDER), new HashSet<String>());
    }

    @Override
    public void processInitializeItems(Session session) {
        try {
            final List<Node> initializeItems = new ArrayList<Node>();
            final Query getInitializeItems = session.getWorkspace().getQueryManager().createQuery(GET_INITIALIZE_ITEMS, Query.SQL);
            final NodeIterator nodes = getInitializeItems.execute().getNodes();
            while(nodes.hasNext()) {
                initializeItems.add(nodes.nextNode());
            }
            processInitializeItems(session, initializeItems, false);
        } catch (RepositoryException ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }

    @Override
    public void processInitializeItems(Session session, List<Node> initializeItems) {
        processInitializeItems(session, initializeItems, false);
    }

    @Override
    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    private void processInitializeItems(Session session, List<Node> initializeItems, boolean dryRun) {

        try {
            if (session == null || !session.isLive()) {
                getLogger().warn("Unable to refresh initialize nodes, no session available");
                return;
            }

            session.refresh(false);

            for (Node initializeItem : initializeItems) {
                getLogger().info("Initializing configuration from " + initializeItem.getName());
                try {

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_NAMESPACE)) {
                        processNamespaceItem(initializeItem, session, dryRun);
                    }

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)) {
                        processNodeTypesFromFile(initializeItem, session, dryRun);
                    }

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_NODETYPES)) {
                        processNodeTypesFromNode(initializeItem, session, dryRun);
                    }

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTDELETE)) {
                        processContentDelete(initializeItem, session, dryRun);
                    }

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTPROPDELETE)) {
                        processContentPropDelete(initializeItem, session, dryRun);
                    }

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE)) {
                        processContentFromFile(initializeItem, session, dryRun);
                    }

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENT)) {
                        processContentFromNode(initializeItem, session, dryRun);
                    }

                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTPROPSET)) {
                        processContentPropSet(initializeItem, session, dryRun);
                    }
                    if (initializeItem.hasProperty(HippoNodeType.HIPPO_CONTENTPROPADD)) {
                        processContentPropAdd(initializeItem, session, dryRun);
                    }

                    if (dryRun) {
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("configuration as specified by " + initializeItem.getName());
                            for (NodeIterator iter = ((HippoSession)session).pendingChanges(); iter.hasNext();) {
                                Node pendingNode = iter.nextNode();
                                getLogger().debug("configuration as specified by " + initializeItem.getName() + " modified node " + pendingNode.getPath());
                            }
                        }
                        session.refresh(false);
                    } else {
                        initializeItem.setProperty(HippoNodeType.HIPPO_STATUS, "done");
                        session.save();
                    }

                } catch (IOException | ParseException | RepositoryException ex) {
                    getLogger().error("configuration as specified by " + initializeItem.getPath() + " failed", ex);
                } finally {
                    session.refresh(false);
                }
            }
        } catch (RepositoryException ex) {
            getLogger().error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    private void processContentPropSet(final Node node, final Session session, final boolean dryRun) throws RepositoryException {
        Property contentSetProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTPROPSET);
        String contentRoot = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_CONTENTROOT, "/"));
        getLogger().info("Contentpropset property " + contentRoot);

        if (session.propertyExists(contentRoot)) {
            final Property property = session.getProperty(contentRoot);
            if (property.isMultiple()) {
                property.setValue(contentSetProperty.getValues());
            } else {
                final Value[] values = contentSetProperty.getValues();
                if (values.length == 0) {
                    property.remove();
                } else if (values.length == 1) {
                    property.setValue(values[0]);
                } else {
                    log.warn("Initialize item {} wants to set multiple values on single valued property", node.getName());
                }
            }
        } else {
            final int offset = contentRoot.lastIndexOf('/');
            final String targetNodePath = offset == 0 ? "/" : contentRoot.substring(0, offset);
            final String propertyName = contentRoot.substring(offset+1);
            final Value[] values = contentSetProperty.getValues();
            if (values.length == 1) {
                session.getNode(targetNodePath).setProperty(propertyName, values[0]);
            } else {
                session.getNode(targetNodePath).setProperty(propertyName, values);
            }
        }
    }

    private void processContentPropAdd(final Node node, final Session session, final boolean dryRun) throws RepositoryException {
        Property contentAddProperty = node.getProperty(HippoNodeType.HIPPO_CONTENTPROPADD);
        String contentRoot = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_CONTENTROOT, "/"));
        getLogger().info("Contentpropadd property " + contentRoot);

        final Property property = session.getProperty(contentRoot);
        if (property.isMultiple()) {
            final List<Value> values = new ArrayList<>(Arrays.asList(property.getValues()));
            values.addAll(Arrays.asList(contentAddProperty.getValues()));
            property.setValue(values.toArray(new Value[values.size()]));
        } else {
            log.warn("Cannot add values to a single valued property");
        }
    }

    private void processContentFromNode(final Node node, final Session session, final boolean dryRun) throws RepositoryException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Found content configuration");
        }

        Property contentProperty = node.getProperty(HippoNodeType.HIPPO_CONTENT);
        String contentName = "<<internal>>";
        InputStream contentStream = contentProperty.getStream();

        if (contentStream == null) {
            getLogger().error("Cannot locate content configuration '" + contentName + "', initialization skipped");
            return;
        }

        final String root = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_CONTENTROOT, "/"));
        if (root.startsWith(INIT_PATH)) {
            getLogger().error("Bootstrapping content to " + INIT_PATH + " is no supported");
            return;
        }
        initializeNodecontent(session, root, contentStream, contentName + ":" + node.getPath());
    }

    public void processContentFromFile(final Node node, final Session session, final boolean dryRun) throws RepositoryException, IOException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Found content resource configuration");
        }

        String contentResource = StringUtils.trim(node.getProperty(HippoNodeType.HIPPO_CONTENTRESOURCE).getString());
        URL contentURL = getResource(node, contentResource);
        boolean pckg = contentResource.endsWith(".zip") || contentResource.endsWith(".jar");

        if (contentURL == null) {
            getLogger().error("Cannot locate content configuration '" + contentResource + "', initialization skipped");
            return;
        }

        final String contentRoot = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_CONTENTROOT, "/"));
        if (contentRoot.startsWith(INIT_PATH)) {
            getLogger().error("Bootstrapping content to " + INIT_PATH + " is not supported");
            return;
        }

        if (isReloadable(node)) {
            final String contextNodeName = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_CONTEXTNODENAME, null));
            if (contextNodeName != null ) {
                final String contextNodePath = contentRoot.equals("/") ? contentRoot + contextNodeName : contentRoot + "/" + contextNodeName;
                final int index = getNodeIndex(session, contextNodePath);
                if (removeNode(session, contextNodePath, false)) {
                    InputStream is = null;
                    BufferedInputStream bis = null;
                    try {
                        is = contentURL.openStream();
                        bis = new BufferedInputStream(is); 
                        initializeNodecontent(session, contentRoot, bis, contentURL.toString(), pckg);
                    } finally {
                        IOUtils.closeQuietly(bis);
                        IOUtils.closeQuietly(is);
                    }
                    if (index != -1) {
                        reorderNode(session, contextNodePath, index);
                    }
                } else {
                    getLogger().error("Cannot reload item {}: removing node failed", node.getName());
                }
            } else {
                getLogger().error("Cannot reload item {} because context node could not be determined", node.getName());
            }
        } else {
            InputStream is = null;
            BufferedInputStream bis = null;
            try {
                is = contentURL.openStream();
                bis = new BufferedInputStream(is); 
                initializeNodecontent(session, contentRoot, bis, contentURL.toString(), pckg);
            } finally {
                IOUtils.closeQuietly(bis);
                IOUtils.closeQuietly(is);
            }
        }
    }

    private void reorderNode(final Session session, final String nodePath, final int index) throws RepositoryException {
        final Node node = session.getNode(nodePath);
        final String srcChildRelPath = node.getName() + "[" + node.getIndex() + "]";
        final Node parent = node.getParent();
        final NodeIterator nodes = parent.getNodes();
        nodes.skip(index);
        if (nodes.hasNext()) {
            final Node destChild = nodes.nextNode();
            String destChildRelPath = destChild.getName() + "[" + destChild.getIndex() + "]";
            if (!srcChildRelPath.equals(destChildRelPath)) {
                parent.orderBefore(srcChildRelPath, destChildRelPath);
            }
        }
    }

    private int getNodeIndex(final Session session, final String nodePath) throws RepositoryException {
        final Node node = JcrUtils.getNodeIfExists(nodePath, session);
        if (node != null && node.getParent().getPrimaryNodeType().hasOrderableChildNodes()) {
            final NodeIterator nodes = node.getParent().getNodes();
            int index = 0;
            while (nodes.hasNext()) {
                if (nodes.nextNode().isSame(node)) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    private void processContentDelete(final Node node, final Session session, final boolean dryRun) throws RepositoryException {
        final String path = StringUtils.trim(node.getProperty(HippoNodeType.HIPPO_CONTENTDELETE).getString());
        final boolean immediateSave = !node.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE) && !node.hasProperty(HippoNodeType.HIPPO_CONTENT);
        getLogger().info("Delete content in initialization: {} {}", node.getName(), path);
        final boolean success = removeNode(session, path, immediateSave && !dryRun);
        if (!success) {
            getLogger().error("Content delete in item {} failed", node.getName());
        }
    }

    private void processContentPropDelete(final Node node, final Session session, final boolean dryRun) throws RepositoryException {
        final String path = StringUtils.trim(node.getProperty(HippoNodeType.HIPPO_CONTENTPROPDELETE).getString());
        getLogger().info("Delete content in initialization: {} {}", node.getName(), path);
        final boolean success = removeProperty(session, path, !dryRun);
        if (!success) {
            getLogger().error("Property delete in item {} failed", node.getName());
        }
    }

    private void processNodeTypesFromNode(final Node node, final Session session, final boolean dryRun) throws RepositoryException, ParseException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Found nodetypes configuration");
        }
        String cndName = "<<internal>>";
        InputStream cndStream = node.getProperty(HippoNodeType.HIPPO_NODETYPES).getStream();
        if (cndStream == null) {
            getLogger().error("Cannot get stream for nodetypes definition property.");
        } else {
            getLogger().info("Initializing node types from nodetypes property.");
            if (!dryRun) {
                initializeNodetypes(session.getWorkspace(), cndStream, cndName);
            }
        }
    }

    private void processNodeTypesFromFile(final Node node, final Session session, final boolean dryRun) throws RepositoryException, IOException, ParseException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Found nodetypes resource configuration");
        }
        String cndResource = StringUtils.trim(node.getProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE).getString());
        URL cndURL = getResource(node, cndResource);
        if (cndURL == null) {
            getLogger().error("Cannot locate nodetype configuration '" + cndResource + "', initialization skipped");
        } else {
            if (!dryRun) {
                InputStream is = null;
                BufferedInputStream bis = null;
                try {
                    is = cndURL.openStream();
                    bis = new BufferedInputStream(is);
                    initializeNodetypes(session.getWorkspace(), bis, cndURL.toString());
                } finally {
                    IOUtils.closeQuietly(bis);
                    IOUtils.closeQuietly(is);
                }
            }
        }
    }

    private void processNamespaceItem(final Node node, final Session session, final boolean dryRun) throws RepositoryException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Found namespace configuration");
        }
        String namespace = StringUtils.trim(node.getProperty(HippoNodeType.HIPPO_NAMESPACE).getString());
        getLogger().info("Initializing namespace: " + node.getName() + " " + namespace);
        if (!dryRun) {
            initializeNamespace(session.getWorkspace().getNamespaceRegistry(), node.getName(), namespace);
        }
    }

    public List<Node> loadExtensions(Session session, Node initializationFolder, boolean cleanup) throws IOException, RepositoryException {
        final Set<String> reloadItems = new HashSet<String>();
        final long now = System.currentTimeMillis();
        final List<URL> extensions = scanForExtensions();
        final List<Node> initializeItems = new ArrayList<Node>();
        for(final URL configurationURL : extensions) {
            initializeItems.addAll(loadExtension(configurationURL, session, initializationFolder, reloadItems));
        }
        if (cleanup) {
            cleanupInitializeItems(session, now);
        }
        initializeItems.addAll(markReloadDownstreamItems(session, reloadItems));
        return initializeItems;
    }

    private List<Node> markReloadDownstreamItems(final Session session, final Set<String> reloadItems) throws RepositoryException {
        List<Node> initializeItems = new ArrayList<Node>();
        for (String reloadItem : reloadItems) {
            final Node initItemNode = session.getNodeByIdentifier(reloadItem);
            final String contextNodeName = StringUtils.trim(JcrUtils.getStringProperty(initItemNode, HippoNodeType.HIPPO_CONTEXTNODENAME, null));
            final String contentRoot = StringUtils.trim(JcrUtils.getStringProperty(initItemNode, HippoNodeType.HIPPO_CONTENTROOT, "/"));
            for (Node downStreamItem : getDownstreamItems(session, contentRoot, contextNodeName)) {
                getLogger().info("Marking item {} pending because downstream from {}", new Object[] { downStreamItem.getName(), initItemNode.getName() });
                downStreamItem.setProperty(HippoNodeType.HIPPO_STATUS, "pending");
                initializeItems.add(downStreamItem);
            }
        }
        session.save();
        return initializeItems;
    }

    private void cleanupInitializeItems(final Session session, final long cleanBefore) throws RepositoryException {
        try {
            final String statement = GET_OLD_INITIALIZE_ITEMS.replace("{}", String.valueOf(cleanBefore));
            final Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
            for (Node node : new NodeIterable(query.execute().getNodes())) {
                if (node != null) {
                    log.info("Removing old initialize item {}", node.getName());
                    node.remove();
                }
            }
            session.save();
        } catch (RepositoryException e) {
            log.error("Exception occurred while cleaning up old initialize items", e);
            session.refresh(false);
        }
    }

    public List<Node> loadExtension(final URL configurationURL, final Session session, final Node initializationFolder, final Set<String> reloadItems) throws RepositoryException, IOException {
        List<Node> initializeItems = new ArrayList<Node>();
        getLogger().info("Initializing extension "+configurationURL);
        try {
            initializeNodecontent(session, "/hippo:configuration/hippo:temporary", configurationURL.openStream(), configurationURL.getPath());
            final Node tempInitFolderNode = session.getNode("/hippo:configuration/hippo:temporary/hippo:initialize");
            final String moduleVersion = getModuleVersion(configurationURL);
            for (final Node tempInitItemNode : new NodeIterable(tempInitFolderNode.getNodes())) {
                initializeItems.addAll(initializeInitializeItem(tempInitItemNode, initializationFolder, moduleVersion, configurationURL, reloadItems));

            }
            if(tempInitFolderNode.hasProperty(HippoNodeType.HIPPO_VERSION)) {
                Set<String> tags = new TreeSet<String>();
                if (initializationFolder.hasProperty(HippoNodeType.HIPPO_VERSION)) {
                    for (Value value : initializationFolder.getProperty(HippoNodeType.HIPPO_VERSION).getValues()) {
                        tags.add(value.getString());
                    }
                }
                Value[] added = tempInitFolderNode.getProperty(HippoNodeType.HIPPO_VERSION).getValues();
                for (Value value : added) {
                    tags.add(value.getString());
                }
                initializationFolder.setProperty(HippoNodeType.HIPPO_VERSION, tags.toArray(new String[tags.size()]));
            }
            tempInitFolderNode.remove();
            session.save();
        } catch (PathNotFoundException ex) {
            getLogger().error("Rejected old style configuration content", ex);
            for(NodeIterator removeTempIter = session.getRootNode().getNode("hippo:configuration/hippo:temporary").getNodes(); removeTempIter.hasNext(); ) {
                removeTempIter.nextNode().remove();
            }
            session.save();
        } catch (RepositoryException ex) {
            throw new RepositoryException("Initializing extension " + configurationURL.getPath() + " failed", ex);
        }
        return initializeItems;
    }

    private List<Node> initializeInitializeItem(final Node tempInitItemNode, final Node initializationFolder, final String moduleVersion, final URL configurationURL, final Set<String> reloadItems) throws RepositoryException {

        getLogger().info("Initializing item: " + tempInitItemNode.getName());

        final List<Node> initializeItems = new ArrayList<Node>();
        Node initItemNode = JcrUtils.getNodeIfExists(initializationFolder, tempInitItemNode.getName());
        final String deprecatedExistingModuleVersion = initItemNode != null ? JcrUtils.getStringProperty(initItemNode, HippoNodeType.HIPPO_EXTENSIONBUILD, null) : null;
        final String existingModuleVersion = initItemNode != null ? JcrUtils.getStringProperty(initItemNode, HippoNodeType.HIPPO_EXTENSIONVERSION, deprecatedExistingModuleVersion) : deprecatedExistingModuleVersion;
        final String existingItemVersion = initItemNode != null ? JcrUtils.getStringProperty(initItemNode, HippoNodeType.HIPPO_VERSION, null) : null;
        final String itemVersion = JcrUtils.getStringProperty(tempInitItemNode, HippoNodeType.HIPPO_VERSION, null);

        final boolean isReload = initItemNode != null && shouldReload(tempInitItemNode, initItemNode, moduleVersion, existingModuleVersion, itemVersion, existingItemVersion);

        if (isReload) {
            getLogger().info("Item " + tempInitItemNode.getName() + " needs to be reloaded");
            initItemNode.remove();
            initItemNode = null;
        }

        if (initItemNode == null) {
            initItemNode = initializationFolder.addNode(tempInitItemNode.getName(), HippoNodeType.NT_INITIALIZEITEM);
            initItemNode.setProperty(HippoNodeType.HIPPO_STATUS, "pending");
            initializeItems.add(initItemNode);
        }

        if (isExtension(configurationURL)) {
            initItemNode.setProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE, configurationURL.toString());
            if (moduleVersion != null) {
                initItemNode.setProperty(HippoNodeType.HIPPO_EXTENSIONVERSION, moduleVersion);
            }
        }

        for (String propertyName : INIT_ITEM_PROPERTIES) {
            copyProperty(tempInitItemNode, initItemNode, propertyName);
        }

        ContentFileInfo info = initItemNode.hasProperty(HippoNodeType.HIPPO_CONTENTRESOURCE) ? readContentFileInfo(initItemNode) : null;
        if (info != null) {
            initItemNode.setProperty(HippoNodeType.HIPPO_CONTEXTNODENAME, info.contextNodeName);
            initItemNode.setProperty(HippoNodeType.HIPPOSYS_DELTADIRECTIVE, info.deltaDirective);
            if (isReload) {
                reloadItems.add(initItemNode.getIdentifier());
            }
        }

        initItemNode.setProperty(HippoNodeType.HIPPO_TIMESTAMP, System.currentTimeMillis());

        return initializeItems;
    }

    private boolean isExtension(final URL configurationURL) {
        return configurationURL.getFile().endsWith("hippoecm-extension.xml");
    }

    private List<URL> scanForExtensions() throws IOException {
        final List<URL> extensions = new LinkedList<URL>();
        Enumeration<URL> iter = Thread.currentThread().getContextClassLoader().getResources("org/hippoecm/repository/extension.xml");
        while (iter.hasMoreElements()) {
            extensions.add(iter.nextElement());
        }
        iter = Thread.currentThread().getContextClassLoader().getResources("hippoecm-extension.xml");
        while (iter.hasMoreElements()) {
            extensions.add(iter.nextElement());
        }
        return extensions;
    }

    private boolean shouldReload(final Node temp, final Node existing, final String moduleVersion, final String existingModuleVersion, final String itemVersion, final String existingItemVersion) throws RepositoryException {
        if (!isReloadable(temp)) {
            return false;
        }
        if (itemVersion != null && !isNewerVersion(itemVersion, existingItemVersion)) {
            return false;
        }
        if (itemVersion == null && !isNewerVersion(moduleVersion, existingModuleVersion)) {
            return false;
        }
        if (existing.hasProperty(HippoNodeType.HIPPO_STATUS) && existing.getProperty(HippoNodeType.HIPPO_STATUS).getString().equals("disabled")) {
            return false;
        }
        return true;
    }

    private boolean isNewerVersion(final String version, final String existingVersion) {
        if (version == null) {
            return false;
        }
        if (existingVersion == null) {
            return true;
        }
        try {
            return new MavenComparableVersion(version).compareTo(new MavenComparableVersion(existingVersion)) > 0;
        } catch (RuntimeException e) {
            // version could not be parsed
            getLogger().error("Invalid version: " + version + " or existing: " + existingVersion);
        }
        return false;
    }

    private String getModuleVersion(URL configurationURL) {
        String configurationURLString = configurationURL.toString();
        if (configurationURLString.endsWith("hippoecm-extension.xml")) {
            String manifestUrlString = configurationURLString.substring(0, configurationURLString.length() - "hippoecm-extension.xml".length()) + "META-INF/MANIFEST.MF";
            try {
                Manifest manifest = new Manifest(new URL(manifestUrlString).openStream());
                return manifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Build"));
            } catch (IOException ex) {
                // deliberate ignore, manifest file not available so no build number can be obtained
            }
        }
        return null;
    }

    public Iterable<Node> getDownstreamItems(final Session session, final String contentRoot, final String contextNodeName) throws RepositoryException {
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(
                "SELECT * FROM hipposys:initializeitem WHERE " +
                        "jcr:path = '/hippo:configuration/hippo:initialize/%' AND " +
                        HippoNodeType.HIPPO_CONTENTROOT + " LIKE '" + contentRoot + "%'", Query.SQL
        ).execute();
        final List<Node> downStreamItems = new ArrayList<>();
        final String contextNodePath = contentRoot.equals("/") ? contentRoot + contextNodeName : contentRoot + "/" + contextNodeName;
        for (Node node : new NodeIterable(result.getNodes())) {
            final String dsContentRoot = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_CONTENTROOT, null));
            final String dsContextNodeName = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_CONTEXTNODENAME, null));
            final String dsContextNodePath = dsContentRoot.equals("/") ? dsContentRoot + dsContextNodeName : dsContentRoot + "/" + dsContextNodeName;
            if (contextNodePath.equals(dsContextNodePath) || dsContextNodePath.startsWith(contextNodePath + "/")) {
                downStreamItems.add(node);
            }
        }
        return downStreamItems;
    }

    public ContentFileInfo readContentFileInfo(final Node item) {
        if (factory == null) {
            return null;
        }
        try {
            final String contentResource = StringUtils.trim(item.getProperty(HippoNodeType.HIPPO_CONTENTRESOURCE).getString());
            if (contentResource.endsWith(".zip") || contentResource.endsWith(".jar")) {
                return null;
            }
            URL contentURL = getResource(item, contentResource);
            if (contentURL != null) {
                InputStream is = null;
                BufferedInputStream bis = null;
                try {
                    // inspect the xml file to find out if it is a delta xml and to read the name of the context node we must remove
                    String contextNodeName = null;
                    String deltaDirective = null;
                    XmlPullParser xpp = factory.newPullParser();
                    is = contentURL.openStream();
                    bis = new BufferedInputStream(is);
                    xpp.setInput(bis, null);
                    while(xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                        if (xpp.getEventType() == XmlPullParser.START_TAG) {
                            contextNodeName = xpp.getAttributeValue("http://www.jcp.org/jcr/sv/1.0", "name");
                            deltaDirective = xpp.getAttributeValue("http://www.onehippo.org/jcr/xmlimport", "merge");
                            break;
                        }
                        xpp.next();
                    }
                    return new ContentFileInfo(contextNodeName, deltaDirective);
                } finally {
                    IOUtils.closeQuietly(bis);
                    IOUtils.closeQuietly(is);
                }
            }
        } catch (RepositoryException | XmlPullParserException | IOException e) {
            getLogger().error("Could not read root node name from content file", e);
        }
        return null;
    }

    private URL getResource(final Node item, String resourcePath) throws RepositoryException, IOException {
        if (resourcePath.startsWith("file:")) {
            return URI.create(resourcePath).toURL();
        } else {
            if (item.hasProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE)) {
                URL resource = new URL(item.getProperty(HippoNodeType.HIPPO_EXTENSIONSOURCE).getString());
                resource = new URL(resource, resourcePath);
                return resource;
            } else {
                return LocalHippoRepository.class.getResource(resourcePath);
            }
        }
    }

    public void initializeNamespace(NamespaceRegistry nsreg, String prefix, String uri) throws RepositoryException {
        try {

            /* Try to remap namespace if a namespace already exists and the uri is similar.
             * This assumes a convention to use in the namespace URI.  It should end with a version
             * number of the nodetypes, such as in http://www.sample.org/nt/1.0.0
             */
            try {
                String currentURI = nsreg.getURI(prefix);
                if (currentURI.equals(uri)) {
                    getLogger().debug("Namespace already exists: " + prefix + ":" + uri);
                    return;
                }
                String uriPrefix = currentURI.substring(0, currentURI.lastIndexOf("/") + 1);
                if(!uriPrefix.equals(uri.substring(0,uri.lastIndexOf("/")+1))) {
                    getLogger().error("Prefix already used for different namespace: " + prefix + ":" + uri);
                    return;
                }
                // do not remap namespace, the upgrading infrastructure must take care of this
                return;
            } catch (NamespaceException ex) {
                if (!ex.getMessage().endsWith("is not a registered namespace prefix.")) {
                    getLogger().error(ex.getMessage() +" For: " + prefix + ":" + uri);
                }
            }

            nsreg.registerNamespace(prefix, uri);

        } catch (NamespaceException ex) {
            if (ex.getMessage().endsWith("mapping already exists")) {
                getLogger().error("Namespace already exists: " + prefix + ":" + uri);
            } else {
                getLogger().error(ex.getMessage()+" For: " + prefix + ":" + uri);
            }
        }
    }

    public void initializeNodetypes(Workspace workspace, InputStream cndStream, String cndName) throws ParseException, RepositoryException {
        getLogger().info("Initializing nodetypes from: " + cndName);
        CompactNodeTypeDefReader<QNodeTypeDefinition,NamespaceMapping> cndReader = new HippoCompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(new InputStreamReader(cndStream), cndName, workspace.getNamespaceRegistry(), new QDefinitionBuilderFactory());
        List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        for (QNodeTypeDefinition ntd : ntdList) {
            try {
                ntreg.registerNodeType(ntd);
                getLogger().info("Registered node type: " + ntd.getName().getLocalName());
            } catch (NamespaceException ex) {
                getLogger().error(ex.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), ex);
            } catch (InvalidNodeTypeDefException ex) {
                if (ex.getMessage().endsWith("already exists")) {
                    try {
                        ntreg.reregisterNodeType(ntd);
                        getLogger().info("Replaced node type: " + ntd.getName().getLocalName());
                    } catch (NamespaceException e) {
                        getLogger().error(e.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                    } catch (InvalidNodeTypeDefException e) {
                        getLogger().info(e.getMessage() + ". In " + cndName + " for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                    } catch (RepositoryException e) {
                        if (!e.getMessage().equals("not yet implemented")) {
                            getLogger().warn(e.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), e);
                        }
                    }
                } else {
                    getLogger().error(ex.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), ex);
                }
            } catch (RepositoryException ex) {
                if (!ex.getMessage().equals("not yet implemented")) {
                    getLogger().warn(ex.getMessage() + ". In " + cndName + " error for " + ntd.getName().getNamespaceURI() + ":" + ntd.getName().getLocalName(), ex);
                }
            }
        }
    }

    public boolean removeNode(Session session, String absPath, boolean save) {
        if (!absPath.startsWith("/")) {
            getLogger().warn("Not an absolute path: {}", absPath);
            return false;
        }
        if ("/".equals(absPath)) {
            getLogger().warn("Not allowed to delete rootNode from initialization");
            return false;
        }

        try {
            if (session.nodeExists(absPath)) {
                final int offset = absPath.lastIndexOf('/');
                final String nodeName = absPath.substring(offset+1);
                final String parentPath = offset == 0 ? "/" : absPath.substring(0, offset);
                final Node parent = session.getNode(parentPath);
                if (parent.getNodes(nodeName).getSize() > 1) {
                    getLogger().warn("Removing same name sibling is not supported: not removing {}", absPath);
                } else {
                    session.getNode(absPath).remove();
                }
                if (save) {
                    session.save();
                }
            }
            return true;
        } catch (RepositoryException ex) {
            if (getLogger().isDebugEnabled()) {
                getLogger().error("Error while removing node '" + absPath + "' : " + ex.getMessage(), ex);
            } else {
                getLogger().error("Error while removing node '" + absPath + "' : " + ex.getMessage());
            }
        }
        return false;
    }

    private boolean removeProperty(final Session session, final String absPath, final boolean save) {
        if (!absPath.startsWith("/")) {
            getLogger().warn("Not an absolute path: {}", absPath);
            return false;
        }
        try {
            if (session.propertyExists(absPath)) {
                session.getProperty(absPath).remove();
                if (save) {
                    session.save();
                }
            }
            return true;
        } catch (RepositoryException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().error("Error while removing property '" + absPath + "' : " + e.getMessage(), e);
            } else {
                getLogger().error("Error while removing property '" + absPath + "' : " + e.getMessage());
            }
        }
        return false;
    }

    public void initializeNodecontent(Session session, String parentAbsPath, InputStream istream, String location) {
        initializeNodecontent(session, parentAbsPath, istream, location, false);
    }

    public void initializeNodecontent(Session session, String parentAbsPath, InputStream istream, String location, boolean pckg) {
        getLogger().info("Initializing content from: " + location + " to " + parentAbsPath);
        File tempFile = null;
        ZipFile zipFile = null;
        InputStream esvIn = null;
        FileOutputStream out = null;
        try {
            String relpath = (parentAbsPath.startsWith("/") ? parentAbsPath.substring(1) : parentAbsPath);
            if (relpath.length() > 0 && !session.getRootNode().hasNode(relpath)) {
                session.getRootNode().addNode(relpath);
            }
            if (session instanceof HippoSession) {
                HippoSession hippoSession = (HippoSession) session;
                int uuidBehaviour = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
                int referenceBehaviour = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
                int mergeBehaviour = ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP;
                if (pckg) {
                    tempFile = File.createTempFile("package", ".zip");
                    out = new FileOutputStream(tempFile);
                    IOUtils.copy(istream, out);
                    out.close();
                    out = null;
                    zipFile = new ZipFile(tempFile);
                    ContentResourceLoader contentResourceLoader = new ZipFileContentResourceLoader(zipFile);
                    esvIn = contentResourceLoader.getResourceAsStream("esv.xml");
                    hippoSession.importDereferencedXML(parentAbsPath, esvIn, contentResourceLoader,
                            uuidBehaviour, referenceBehaviour, mergeBehaviour);
                }
                else {
                    ContentResourceLoader contentResourceLoader = null;
                    if ((StringUtils.startsWith(location, "jar:file:") || StringUtils.startsWith(location, "file:")) && StringUtils.contains(location, "!")) {
                        File sourceFile = new File(URI.create(StringUtils.removeStart(StringUtils.substringBefore(location, "!"), "jar:")));
                        zipFile = new ZipFile(sourceFile);
                        contentResourceLoader = new ZipFileContentResourceLoader(zipFile);
                    } else if (StringUtils.startsWith(location, "file:")) {
                        File sourceFile = new File(URI.create(location));
                        contentResourceLoader = new FileContentResourceLoader(sourceFile.getParentFile());
                    }
                    hippoSession.importDereferencedXML(parentAbsPath, istream, contentResourceLoader,
                            uuidBehaviour, referenceBehaviour, mergeBehaviour);
                }
            } else {
                session.importXML(parentAbsPath, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            }
        } catch (IOException | RepositoryException e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().error("Error initializing content for " + location + " in '" + parentAbsPath + "' : " + e.getClass().getName() + ": " + e.getMessage(), e);
            } else {
                getLogger().error("Error initializing content for "+location+" in '" + parentAbsPath + "' : " + e.getClass().getName() + ": " + e.getMessage());
            }
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(esvIn);
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception ignore) {
                }
            }
            FileUtils.deleteQuietly(tempFile);
        }
    }

    private boolean isReloadable(Node node) throws RepositoryException {
        if (JcrUtils.getBooleanProperty(node, HippoNodeType.HIPPO_RELOADONSTARTUP, false)) {
            final String deltaDirective = StringUtils.trim(JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_DELTADIRECTIVE, null));
            if (deltaDirective != null && (deltaDirective.equals("combine") || deltaDirective.equals("overlay"))) {
                getLogger().error("Cannot reload initialize item {} because it is a combine or overlay delta", node.getName());
                return false;
            }
            return true;
        }
        return false;
    }

    private void copyProperty(Node source, Node target, String propertyName) throws RepositoryException {
        final Property property = JcrUtils.getPropertyIfExists(source, propertyName);
        if (property != null) {
            if (property.getDefinition().isMultiple()) {
                target.setProperty(propertyName, property.getValues(), property.getType());
            } else {
                target.setProperty(propertyName, property.getValue());
            }
        }
    }

    private Logger getLogger() {
        if (logger != null) {
            return logger;
        }
        return log;
    }

    private static class ContentFileInfo {

        private final String contextNodeName;
        private final String deltaDirective;

        private ContentFileInfo(final String contextNodeName, final String deltaDirective) {
            this.contextNodeName = contextNodeName;
            this.deltaDirective = deltaDirective;
        }
    }
}
