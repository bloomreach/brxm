/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.hippoecm.repository.LocalHippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.MavenComparableVersion;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.bootstrap.util.BootstrapConstants;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.onehippo.repository.xml.ImportResult;
import org.slf4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NAME;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.SV_NODE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTDELETE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPADD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPDELETE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPSET;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTEXTPATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAMESPACE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NODETYPES;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NODETYPESRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELOADONSTARTUP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SEQUENCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_TIMESTAMP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_UPSTREAMITEMS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_WEBRESOURCEBUNDLE;
import static org.hippoecm.repository.util.RepoUtils.getClusterNodeId;
import static org.onehippo.repository.util.JcrConstants.MIX_LOCKABLE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.COMBINE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.ENHANCED_IMPORT_URI;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.MERGE;
import static org.onehippo.repository.xml.EnhancedSystemViewConstants.OVERLAY;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class InitializationProcessorImpl implements InitializationProcessor {

    private static final String INIT_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH;
    private static final long LOCK_TIMEOUT = Long.getLong("repo.bootstrap.lock.timeout", 60 * 5);
    private static final long LOCK_ATTEMPT_INTERVAL = 1000 * 2;

    private static final String[] INIT_ITEM_PROPERTIES = {
            HIPPO_SEQUENCE,
            HIPPO_NAMESPACE,
            HIPPO_NODETYPESRESOURCE,
            HIPPO_NODETYPES,
            HIPPO_CONTENTRESOURCE,
            HIPPO_CONTENT,
            HIPPO_CONTENTROOT,
            HIPPO_CONTENTDELETE,
            HIPPO_CONTENTPROPDELETE,
            HIPPO_CONTENTPROPSET,
            HIPPO_CONTENTPROPADD,
            HIPPO_RELOADONSTARTUP,
            HIPPO_VERSION,
            HIPPO_WEBRESOURCEBUNDLE
    };

    private final static String GET_INITIALIZE_ITEMS = String.format(
            "SELECT * FROM hipposys:initializeitem " +
            "WHERE %s = 'pending' ORDER BY %s ASC", HIPPO_STATUS, HIPPO_SEQUENCE);

    private final static String GET_MISSING_INITIALIZE_ITEMS = String.format(
            "SELECT * FROM hipposys:initializeitem " +
            "WHERE %s IS NULL OR %s < %%s", HIPPO_TIMESTAMP, HIPPO_TIMESTAMP);

    private static final double NO_HIPPO_SEQUENCE = -1.0;

    private static final Comparator<Node> initializeItemComparator = new Comparator<Node>() {

        @Override
        public int compare(final Node n1, final Node n2) {
            try {
                final Double s1 = JcrUtils.getDoubleProperty(n1, HIPPO_SEQUENCE, NO_HIPPO_SEQUENCE);
                final Double s2 = JcrUtils.getDoubleProperty(n2, HIPPO_SEQUENCE, NO_HIPPO_SEQUENCE);
                final int result = s1.compareTo(s2);
                if (result != 0) {
                    return result;
                }
                return n1.getName().compareTo(n2.getName());
            } catch (RepositoryException e) {
                log.error("Error comparing initialize item nodes", e);
            }
            return 0;
        }
    };


    public InitializationProcessorImpl() {}

    public InitializationProcessorImpl(Logger logger) {
        if (logger != null) {
            BootstrapConstants.log = logger;
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
    public List<PostStartupTask> processInitializeItems(Session session) {
        try {
            final List<Node> initializeItems = new ArrayList<>();
            final Query getInitializeItems = session.getWorkspace().getQueryManager().createQuery(GET_INITIALIZE_ITEMS, Query.SQL);
            final NodeIterator nodes = getInitializeItems.execute().getNodes();
            while(nodes.hasNext()) {
                initializeItems.add(nodes.nextNode());
            }
            return doProcessInitializeItems(session, initializeItems);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    public List<PostStartupTask> processInitializeItems(Session session, List<Node> initializeItems) {
        return doProcessInitializeItems(session, initializeItems);
    }

    @Override
    public void setLogger(final Logger logger) {
        BootstrapConstants.log = logger;
    }

    @Override
    public boolean lock(final Session session) throws RepositoryException {
        ensureIsLockable(session, INIT_PATH);
        final LockManager lockManager = session.getWorkspace().getLockManager();
        final long t1 = System.currentTimeMillis();
        while (true) {
            log.debug("Attempting to obtain lock");
            try {
                lockManager.lock(INIT_PATH, false, false, LOCK_TIMEOUT, getClusterNodeId(session));
                log.debug("Lock successfully obtained");
                return true;
            } catch (LockException e) {
                if (System.currentTimeMillis() - t1 < LOCK_TIMEOUT * 1000) {
                    log.debug("Obtaining lock failed, reattempting in {} ms", LOCK_ATTEMPT_INTERVAL);
                    try {
                        Thread.sleep(LOCK_ATTEMPT_INTERVAL);
                    } catch (InterruptedException ignore) {
                    }
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public void unlock(final Session session) throws RepositoryException {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        try {
            log.debug("Attempting to release lock");
            session.refresh(false);
            lockManager.unlock(INIT_PATH);
            log.debug("Lock successfully released");
        } catch (LockException e) {
            log.warn("Current session no longer holds a lock, please set a longer repo.bootstrap.lock.timeout");
        }
    }

    private List<PostStartupTask> doProcessInitializeItems(final Session session, final List<Node> initializeItems) {
        Collections.sort(initializeItems, initializeItemComparator);
        final List<PostStartupTask> postStartupTasks = new ArrayList<>();
        try {
            session.refresh(false);
            for (Node initializeItemNode : initializeItems) {
                InitializeItem initializeItem = new InitializeItem(initializeItemNode);
                try {
                    initializeItem.validate();
                    postStartupTasks.addAll(initializeItem.process());
                    session.save();
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.error("Failed to initialize item {}", initializeItem.getName(), e);
                    } else {
                        log.error("Failed to process initialize item {}: {}", initializeItem.getName(), e.toString());
                    }
                    session.refresh(false);
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage(), e);
        }
        return postStartupTasks;
    }

    public List<Node> loadExtensions(Session session, Node initializationFolder, boolean cleanup) throws IOException, RepositoryException {
        final Set<String> reloadItems = new HashSet<>();
        final long now = System.currentTimeMillis();
        final List<URL> extensions = scanForExtensions();
        final List<Node> initializeItems = new ArrayList<>();
        for(final URL configurationURL : extensions) {
            initializeItems.addAll(loadExtension(configurationURL, session, initializationFolder, reloadItems));
        }
        if (cleanup) {
            markMissingInitializeItems(session, now);
        }
        initializeItems.addAll(markReloadDownstreamItems(session, reloadItems));
        return initializeItems;
    }

    List<Node> markReloadDownstreamItems(final Session session, final Set<String> reloadItems) throws RepositoryException {
        List<Node> initializeItems = new ArrayList<>();
        for (String reloadItem : reloadItems) {
            final Node initItemNode = session.getNodeByIdentifier(reloadItem);
            for (Node downStreamItem : resolveDownstreamItems(session, initItemNode)) {
                log.info("Marking item {} pending because downstream from {}", new Object[] { downStreamItem.getName(), initItemNode.getName() });
                downStreamItem.setProperty(HIPPO_STATUS, "pending");
                Value[] upstreamItems;
                if (downStreamItem.hasProperty(HIPPO_UPSTREAMITEMS)) {
                    List<Value> values = new ArrayList<>(Arrays.asList(downStreamItem.getProperty(HIPPO_UPSTREAMITEMS).getValues()));
                    values.add(session.getValueFactory().createValue(reloadItem));
                    upstreamItems = values.toArray(new Value[values.size()]);
                } else {
                    upstreamItems = new Value[] { session.getValueFactory().createValue(reloadItem) };
                }
                downStreamItem.setProperty(HIPPO_UPSTREAMITEMS, upstreamItems);
                initializeItems.add(downStreamItem);
            }
        }
        session.save();
        return initializeItems;
    }

    private void markMissingInitializeItems(final Session session, final long markBefore) throws RepositoryException {
        try {
            final String statement = String.format(GET_MISSING_INITIALIZE_ITEMS, String.valueOf(markBefore));
            final Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);
            for (Node node : new NodeIterable(query.execute().getNodes())) {
                if (node != null) {
                    log.info("Marking missing initialize item {}", node.getName());
                    node.setProperty(HIPPO_STATUS, "missing");
                }
            }
            session.save();
        } catch (RepositoryException e) {
            log.error("Exception occurred while marking missing initialize items", e);
            session.refresh(false);
        }
    }

    private List<Node> loadExtension(final URL configurationURL, final Session session, final Node initializationFolder, final Set<String> reloadItems) throws RepositoryException, IOException {
        List<Node> initializeItems = new ArrayList<>();
        log.info("Initializing extension "+configurationURL);
        try {
            initializeNodecontent(session, "/hippo:configuration/hippo:temporary", configurationURL.openStream(), configurationURL);
            final Node tempInitFolderNode = session.getNode("/hippo:configuration/hippo:temporary/hippo:initialize");
            final String moduleVersion = getModuleVersion(configurationURL);
            for (final Node tempInitItemNode : new NodeIterable(tempInitFolderNode.getNodes())) {
                initializeItems.addAll(initializeInitializeItem(tempInitItemNode, initializationFolder, moduleVersion, configurationURL, reloadItems));

            }
            if(tempInitFolderNode.hasProperty(HIPPO_VERSION)) {
                Set<String> tags = new TreeSet<>();
                if (initializationFolder.hasProperty(HIPPO_VERSION)) {
                    for (Value value : initializationFolder.getProperty(HIPPO_VERSION).getValues()) {
                        tags.add(value.getString());
                    }
                }
                Value[] added = tempInitFolderNode.getProperty(HIPPO_VERSION).getValues();
                for (Value value : added) {
                    tags.add(value.getString());
                }
                initializationFolder.setProperty(HIPPO_VERSION, tags.toArray(new String[tags.size()]));
            }
            tempInitFolderNode.remove();
            session.save();
        } catch (PathNotFoundException ex) {
            log.error("Rejected old style configuration content", ex);
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

        log.debug("Initializing item: " + tempInitItemNode.getName());

        final List<Node> initializeItems = new ArrayList<Node>();
        Node initItemNode = JcrUtils.getNodeIfExists(initializationFolder, tempInitItemNode.getName());
        final String deprecatedExistingModuleVersion = initItemNode != null ? JcrUtils.getStringProperty(initItemNode, HippoNodeType.HIPPO_EXTENSIONBUILD, null) : null;
        final String existingModuleVersion = initItemNode != null ? JcrUtils.getStringProperty(initItemNode, HippoNodeType.HIPPO_EXTENSIONVERSION, deprecatedExistingModuleVersion) : deprecatedExistingModuleVersion;
        final String existingItemVersion = initItemNode != null ? JcrUtils.getStringProperty(initItemNode, HIPPO_VERSION, null) : null;
        final String itemVersion = JcrUtils.getStringProperty(tempInitItemNode, HIPPO_VERSION, null);

        final boolean isReload = initItemNode != null &&
                shouldReload(tempInitItemNode, initItemNode, moduleVersion, existingModuleVersion, itemVersion, existingItemVersion);

        if (isReload) {
            log.info("Item {} needs to be reloaded", tempInitItemNode.getName());
            initItemNode.remove();
            initItemNode = null;
        }

        if (initItemNode == null) {
            log.info("Item {} set to status pending", tempInitItemNode.getName());
            initItemNode = initializationFolder.addNode(tempInitItemNode.getName(), HippoNodeType.NT_INITIALIZEITEM);
            initItemNode.setProperty(HIPPO_STATUS, "pending");
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

        ContentFileInfo info = initItemNode.hasProperty(HIPPO_CONTENTRESOURCE) ? readContentFileInfo(initItemNode) : null;
        if (info != null) {
            initItemNode.setProperty(HIPPO_CONTEXTPATHS, info.contextPaths.toArray(new String[info.contextPaths.size()]));
            initItemNode.setProperty(HippoNodeType.HIPPOSYS_DELTADIRECTIVE, info.deltaDirective);
            if (isReload) {
                reloadItems.add(initItemNode.getIdentifier());
            }
        }

        initItemNode.setProperty(HIPPO_TIMESTAMP, System.currentTimeMillis());
        final String status = JcrUtils.getStringProperty(initItemNode, HIPPO_STATUS, null);
        if ("missing".equals(status)) {
            initItemNode.getProperty(HIPPO_STATUS).remove();
        }

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
            log.debug("Item {} is not reloadable", temp.getName());
            return false;
        }
        if (itemVersion != null) {
            final boolean isNewer = isNewerVersion(itemVersion, existingItemVersion);
            log.debug("Comparing item versions of item {}: new version = {}; old version = {}; newer = {}", temp.getName(), itemVersion, existingItemVersion, isNewer);
            if (!isNewer) {
                return false;
            }
        } else {
            final boolean isNewer = isNewerVersion(moduleVersion, existingModuleVersion);
            log.debug("Comparing module versions of item {}: new module version {}; old module version = {}; newer = {}", temp.getName(), moduleVersion, existingModuleVersion, isNewer);
            if (!isNewer) {
                return false;
            }
        }
        if ("disabled".equals(JcrUtils.getStringProperty(existing, HIPPO_STATUS, null))) {
            log.debug("Item {} is disabled", temp.getName());
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
            log.error("Invalid version: " + version + " or existing: " + existingVersion);
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

    List<Node> resolveDownstreamItems(final Session session, final Node upstreamItem) throws RepositoryException {
        final List<Node> downStreamItems = new ArrayList<>();
        final String[] contextPaths = JcrUtils.getMultipleStringProperty(upstreamItem, HIPPO_CONTEXTPATHS, null);
        if (contextPaths != null && contextPaths.length > 0) {
            final String contextPath = contextPaths[0];
            downStreamItems.addAll(resolveContentResourceDownstreamItems(session, contextPath, upstreamItem));
            downStreamItems.addAll(resolveContentPropSetAndAddDownstreamItems(session, contextPath, upstreamItem));
            downStreamItems.addAll(resolveContentDeleteAndContentPropDeleteDownstreamItems(session, contextPath, upstreamItem));
        }
        return downStreamItems;
    }

    /**
     * contentresource items operate on the context path
     */
    private List<Node> resolveContentResourceDownstreamItems(final Session session, final String contextPath, final Node upstreamItem) throws RepositoryException {
        final List<Node> downStreamItems = new ArrayList<>();
        final String statement = String.format(
                "SELECT * FROM hipposys:initializeitem WHERE " +
                "jcr:path = '/hippo:configuration/hippo:initialize/%%' AND (" +
                "%s LIKE '%s/%%' OR %s = '%s') AND %s <> 'missing'",
                HIPPO_CONTEXTPATHS, contextPath, HIPPO_CONTEXTPATHS, contextPath,
                HIPPO_STATUS);
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final QueryResult result = queryManager.createQuery(statement, Query.SQL).execute();
        for (Node item : new NodeIterable(result.getNodes())) {
            if (!upstreamItem.isSame(item)) {
                downStreamItems.add(item);
            }
        }
        return downStreamItems;
    }

    /**
     * contentpropset, contentpropset operate directly on the content root
     */
    private List<Node> resolveContentPropSetAndAddDownstreamItems(final Session session, final String contextPath, final Node upstreamItem) throws RepositoryException {
        final List<Node> downStreamItems = new ArrayList<>();
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(
                "SELECT * FROM hipposys:initializeitem WHERE " +
                        "jcr:path = '/hippo:configuration/hippo:initialize/%' AND (" +
                        HIPPO_CONTENTROOT + " LIKE '" + contextPath + "/%' OR " +
                        HIPPO_CONTENTROOT + " = '" + contextPath + "') AND " +
                        HIPPO_CONTENTRESOURCE + " IS NULL AND " +
                        HIPPO_STATUS + " <> 'missing'", Query.SQL
        ).execute();
        for (Node item : new NodeIterable(result.getNodes())) {
            if (!upstreamItem.isSame(item)) {
                downStreamItems.add(item);
            }
        }
        return downStreamItems;
    }

    /**
     * contentdelete, contentpropdelete operate use neither contextpath nor contentroot
     */
    private List<Node> resolveContentDeleteAndContentPropDeleteDownstreamItems(final Session session, final String contextPath, final Node upstreamItem) throws RepositoryException {
        final List<Node> downStreamItems = new ArrayList<>();
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(
                "SELECT * FROM hipposys:initializeitem WHERE " +
                        "jcr:path = '/hippo:configuration/hippo:initialize/%' AND (" +
                        HIPPO_CONTENTDELETE + " LIKE '" + contextPath + "/%' OR " +
                        HIPPO_CONTENTDELETE + " = '" + contextPath + "' OR " +
                        HIPPO_CONTENTPROPDELETE + " LIKE '" + contextPath + "/%') AND " +
                        HIPPO_STATUS + " <> 'missing'", Query.SQL
        ).execute();
        for (Node item : new NodeIterable(result.getNodes())) {
            if (!upstreamItem.isSame(item)) {
                downStreamItems.add(item);
            }
        }
        return downStreamItems;

    }

    ContentFileInfo readContentFileInfo(final Node item) {
        ContentFileInfoReader contentFileInfoReader = null;
        InputStream in = null;
        try {
            final String contentResource = StringUtils.trim(item.getProperty(HIPPO_CONTENTRESOURCE).getString());
            if (contentResource.endsWith(".zip") || contentResource.endsWith(".jar")) {
                return null;
            }
            final String contentRoot = StringUtils.trim(item.getProperty(HIPPO_CONTENTROOT).getString());
            contentFileInfoReader = new ContentFileInfoReader(contentRoot);
            URL contentResourceURL = getResource(item, contentResource);
            if (contentResourceURL != null) {
                in = contentResourceURL.openStream();
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
                factory.newSAXParser().parse(new InputSource(in), contentFileInfoReader);
            }
        } catch (ContentFileInfoReadingShortCircuitException ignore) {
        } catch (FactoryConfigurationError | SAXException | ParserConfigurationException | IOException | RepositoryException e) {
            log.error("Could not read root node name from content file", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return contentFileInfoReader != null ? contentFileInfoReader.getContentFileInfo() : null;
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

    public ImportResult initializeNodecontent(Session session, String parentAbsPath, InputStream istream, URL location) {
        return BootstrapUtils.initializeNodecontent(session, parentAbsPath, istream, location, false);
    }

    /**
     * Returns a {@link java.io.File} object which bases the input JAR / ZIP file URL.
     * <P>
     * For example, if the <code>url</code> represents "file:/a/b/c.jar!/d/e/f.xml", then
     * this method will return a File object representing "file:/a/b/c.jar" from the input.
     * </P>
     * @param url
     * @return
     * @throws URISyntaxException
     */
    protected File getBaseZipFileFromURL(final URL url) throws URISyntaxException {
        String file = url.getFile();
        int offset = file.indexOf(".jar!");

        if (offset == -1) {
            throw new IllegalArgumentException("Not a jar or zip url: " + url);
        }

        file = file.substring(0, offset + 4);

        if (!file.startsWith("file:")) {
            if (file.startsWith("/")) {
                file = "file://" + file;
            } else {
                file = "file:///" + file;
            }
        }

        return new File(URI.create(file));
    }

    private boolean isReloadable(Node item) throws RepositoryException {
        if (JcrUtils.getBooleanProperty(item, HIPPO_RELOADONSTARTUP, false)) {
            final String deltaDirective = StringUtils.trim(JcrUtils.getStringProperty(item, HippoNodeType.HIPPOSYS_DELTADIRECTIVE, null));
            if (deltaDirective != null && (deltaDirective.equals("combine") || deltaDirective.equals("overlay"))) {
                log.error("Cannot reload initialize item {} because it is a combine or overlay delta", item.getName());
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

    private void ensureIsLockable(final Session session, final String absPath) throws RepositoryException {
        final Node node = session.getNode(absPath);
        if (!node.isNodeType(MIX_LOCKABLE)) {
            node.addMixin(MIX_LOCKABLE);
            session.save();
        }
    }

    static class ContentFileInfo {

        final List<String> contextPaths;
        final String deltaDirective;

        private ContentFileInfo(final List<String> contextPaths, final String deltaDirective) {
            this.contextPaths = contextPaths;
            this.deltaDirective = deltaDirective;
        }
    }

    private static class ContentFileInfoReader extends DefaultHandler {

        private final Stack<String> path = new Stack<>();
        private final List<String> contextPaths = new ArrayList<>();
        private final String contentRoot;
        private String deltaDirective;
        private int depth = -1;

        private ContentFileInfoReader(final String contentRoot) {
            this.contentRoot = contentRoot.equals("/") ? "" : contentRoot;
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes atts) throws SAXException {
            final Name name = NameFactoryImpl.getInstance().create(uri, localName);
            if (name.equals(SV_NODE)) {
                if (skip()) {
                    depth++;
                    return;
                }
                final String svName = atts.getValue(SV_NAME.getNamespaceURI(), SV_NAME.getLocalName());
                final String esvMerge = atts.getValue(ENHANCED_IMPORT_URI, MERGE);
                if (contextPaths.isEmpty()) {
                    path.push(svName);
                    contextPaths.add(getCurrenContextPath());
                    deltaDirective = esvMerge;
                    if (!isMergeCombine(esvMerge)) {
                        throw new ContentFileInfoReadingShortCircuitException();
                    }
                } else {
                    if (isMergeCombine(esvMerge)) {
                        path.push(svName);
                        contextPaths.add(getCurrenContextPath());
                    } else {
                        depth++;
                    }
                }
            }
        }


        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            final Name name = NameFactoryImpl.getInstance().create(uri, localName);
            if (name.equals(SV_NODE)) {
                if (skip()) {
                    depth--;
                } else {
                    path.pop();
                }
            }
        }

        private boolean isMergeCombine(final String esvMerge) {
            return COMBINE.equals(esvMerge) || OVERLAY.equals(esvMerge);
        }

        private ContentFileInfo getContentFileInfo() {
            if (!contextPaths.isEmpty()) {
                return new ContentFileInfo(contextPaths, deltaDirective);
            }
            return null;
        }

        private String getCurrenContextPath() {
            StringBuilder sb = new StringBuilder(contentRoot);
            for (String pathElement : path) {
                sb.append("/").append(pathElement);
            }
            return sb.toString();
        }

        private boolean skip() {
            return depth > -1;
        }
    }

    private static class ContentFileInfoReadingShortCircuitException extends SAXException {}
}
