/*
 *  Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.MavenComparableVersion;
import org.onehippo.cm.model.definition.Definition;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ConfigSourceParser;
import org.onehippo.cm.model.parser.ModuleDescriptorParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.onehippo.cm.engine.Constants.HCM_ACTIONS;
import static org.onehippo.cm.engine.Constants.HCM_BASELINE;
import static org.onehippo.cm.engine.Constants.HCM_BASELINE_PATH;
import static org.onehippo.cm.engine.Constants.HCM_BUNDLES_DIGESTS;
import static org.onehippo.cm.engine.Constants.HCM_BUNDLE_NODE_PATH;
import static org.onehippo.cm.engine.Constants.HCM_CND;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_NODE_PATH;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_ORDER_BEFORE;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_ORDER_BEFORE_FIRST;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_PATH;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_PATHS_APPLIED;
import static org.onehippo.cm.engine.Constants.HCM_DIGEST;
import static org.onehippo.cm.engine.Constants.HCM_HSTROOT;
import static org.onehippo.cm.engine.Constants.HCM_LAST_EXECUTED_ACTION;
import static org.onehippo.cm.engine.Constants.HCM_LAST_UPDATED;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_DESCRIPTOR;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_SEQUENCE;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;
import static org.onehippo.cm.engine.Constants.HCM_SITES;
import static org.onehippo.cm.engine.Constants.HCM_YAML;
import static org.onehippo.cm.engine.Constants.NT_HCM_ACTIONS;
import static org.onehippo.cm.engine.Constants.NT_HCM_BASELINE;
import static org.onehippo.cm.engine.Constants.NT_HCM_BINARY;
import static org.onehippo.cm.engine.Constants.NT_HCM_BUNDLES;
import static org.onehippo.cm.engine.Constants.NT_HCM_CND;
import static org.onehippo.cm.engine.Constants.NT_HCM_CONFIG_FOLDER;
import static org.onehippo.cm.engine.Constants.NT_HCM_CONTENT;
import static org.onehippo.cm.engine.Constants.NT_HCM_CONTENT_FOLDER;
import static org.onehippo.cm.engine.Constants.NT_HCM_CONTENT_SOURCE;
import static org.onehippo.cm.engine.Constants.NT_HCM_DEFINITIONS;
import static org.onehippo.cm.engine.Constants.NT_HCM_DESCRIPTOR;
import static org.onehippo.cm.engine.Constants.NT_HCM_GROUP;
import static org.onehippo.cm.engine.Constants.NT_HCM_MODULE;
import static org.onehippo.cm.engine.Constants.NT_HCM_PROJECT;
import static org.onehippo.cm.engine.Constants.NT_HCM_SITE;
import static org.onehippo.cm.engine.Constants.NT_HCM_SITES;
import static org.onehippo.cm.model.Constants.ACTIONS_YAML;
import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_MODULE_YAML;
import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_FIRST;
import static org.onehippo.cm.model.impl.ConfigurationModelImpl.mergeWithSourceModules;

public class ConfigurationBaselineService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationBaselineService.class);

    private final ConfigurationLockManager configurationLockManager;

    public ConfigurationBaselineService(final ConfigurationLockManager configurationLockManager) {
        this.configurationLockManager = configurationLockManager;
    }

    /**
     * Store and session save a merged configuration model as a baseline configuration in the JCR.
     * The provided ConfigurationModel is assumed to be fully formed and validated for the core model and
     * for any HCM Site with at least one module present in the model. Other HCM sites may not have been loaded yet.
     *
     * @param model   the configuration model to store as the new baseline
     * @param session the session for processing the model
     */
    public void storeBaseline(final ConfigurationModelImpl model, final Session session)
            throws RepositoryException, IOException, ParserException {
        configurationLockManager.lock();
        try {
            // Ensure/force cluster synchronization in case another instance just modified the baseline
            session.refresh(true);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            // assume that HCM_ROOT_PATH already exists
            final Node hcmRootNode = session.getNode(HCM_ROOT_PATH);

            // create storage nodes for main baseline and webfile bundles baseline, if necessary
            Node baselineNode = createNodeIfNecessary(hcmRootNode, HCM_BASELINE, NT_HCM_BASELINE, false);
            createNodeIfNecessary(hcmRootNode, NT_HCM_BUNDLES, NT_HCM_BUNDLES, false);

            // set lastupdated date to now
            baselineNode.setProperty(HCM_LAST_UPDATED, Calendar.getInstance());

            final Node hcmSitesNode = createNodeIfNecessary(baselineNode, HCM_SITES, NT_HCM_SITES, false);


            // TODO: implement a smarter partial-update process instead of brute-force removal
            // clear existing group nodes before creating new ones
            // clear any module whose HCM site is present in the model, plus any core module
            // this gives us a clean slate for storing the new baseline for core and HCM sites that are available now
            //TODO SS: Remove all hcm sites configs at once?
            for (String hcmSiteName: model.getSiteNames()) {
                if (hcmSitesNode.hasNode(hcmSiteName)) {
                    hcmSitesNode.getNode(hcmSiteName).remove();
                }
            }

            // for each group node under this baseline
            for (NodeIterator nodes = baselineNode.getNodes(); nodes.hasNext();) {
                final Node groupNode = nodes.nextNode();
                if (groupNode.getPrimaryNodeType().isNodeType(NT_HCM_GROUP)) {
                    groupNode.remove();
                }
            }

            // compute and store digest from model manifest
            // Note: we've decided not to worry about processing data twice, since we don't expect large files
            //       in the config portion, and content is already optimized to use content path instead of digest
            // TODO: compute a separate digest for core and each hcm site; for now, we just care about core
            String modelDigestString = model.getDigest(null);
            baselineNode.setProperty(HCM_DIGEST, modelDigestString);

            // create group, project, and module nodes, if necessary
            // foreach group

            final List<ModuleImpl> siteModules = model.getModulesStream().filter(ModuleImpl::isNotCore)
                    .collect(toList());
            for (ModuleImpl module: siteModules) {
                storeSiteModule(module, hcmSitesNode, session);
            }

            for (GroupImpl group : model.getSortedGroups()) {
                Node groupNode = createNodeIfNecessary(baselineNode, group.getName(), NT_HCM_GROUP, true);

                // foreach project
                for (ProjectImpl project : group.getProjects()) {
                    Node projectNode = createNodeIfNecessary(groupNode, project.getName(), NT_HCM_PROJECT, true);

                    // foreach module
                    for (ModuleImpl module : project.getModules()) {
                        if (!module.isNotCore()) {
                            Node moduleNode = createNodeIfNecessary(projectNode, module.getName(), NT_HCM_MODULE, true);
                            // process each core module in detail
                            storeBaselineModule(module, moduleNode, session, false);
                        }

                    }
                }
            }

            // clean up empty nodes
            removeEmptyGroupsAndProjects(baselineNode);

            session.save();
            stopWatch.stop();
            log.info("ConfigurationModel stored as baseline configuration in {}", stopWatch);
        } catch (RepositoryException | IOException e) {
            log.error("Failed to store baseline configuration", e);
            throw e;
        } finally {
            configurationLockManager.unlock();
        }
    }


    /**
     * Store a baseline for a specific HCM site represented by the given modules.
     *
     * @param hcmSiteName the name of the site
     * @param model ConfigurationModel containing the site
     * @param session the JCR session for processing the model
     * @throws RepositoryException
     * @throws IOException
     */
    public void storeSite(final String hcmSiteName, final ConfigurationModelImpl model, final Session session)
            throws RepositoryException, IOException {
        configurationLockManager.lock();
        try {

            final List<ModuleImpl> siteModules = model.getModulesStream()
                    .filter(m -> hcmSiteName.equals(m.getSiteName())).collect(toList());
            log.debug("storing baseline for site: {}, modules: {}", hcmSiteName, siteModules);


            session.refresh(true);
            final Node baselineNode = session.getNode(HCM_BASELINE_PATH);
            final Node hcmSitesCatalogNode = baselineNode.getNode(HCM_SITES);

            // set lastupdated date to now
            baselineNode.setProperty(HCM_LAST_UPDATED, Calendar.getInstance());

            if (hcmSitesCatalogNode.hasNode(hcmSiteName)) {
                hcmSitesCatalogNode.getNode(hcmSiteName).remove();
            }

            for (ModuleImpl module: siteModules) {
                storeSiteModule(module, hcmSitesCatalogNode, session);
            }
            session.save();

        } finally {
            configurationLockManager.unlock();
        }
    }


    //TODO SS: Add javadocs
    private void storeSiteModule(final ModuleImpl module, final Node parentNode, final Session session)
            throws RepositoryException, IOException {
        if (!module.isNotCore()) {
            throw new ConfigurationRuntimeException(String.format("Module %s does not belong to an HCM site", module));
        }
        if (module.getHstRoot() == null) {
            throw new ConfigurationRuntimeException(String.format("Module %s for HCM site %s has no hstRoot", module, module.getSiteName()));
        }
        final Node hcmSiteNode = createNodeIfNecessary(parentNode, module.getSiteName(), NT_HCM_SITE, false);
        final String hstRoot = JcrUtils.getStringProperty(hcmSiteNode, HCM_HSTROOT, null);
        if (hstRoot != null && !module.getHstRoot().equals(hstRoot)) {
            throw new ConfigurationRuntimeException(String.format("Module %s for hcm site %s has different hstRoot %s than in baseline (%s)"
                    , module, module.getSiteName(), module.getHstRoot(), hstRoot));
        }
        if (hstRoot == null) {
            hcmSiteNode.setProperty(HCM_HSTROOT, module.getHstRoot().toString());
        }
        final ProjectImpl project = module.getProject();
        final GroupImpl group = project.getGroup();

        final Node groupNode = createNodeIfNecessary(hcmSiteNode, group.getName(), NT_HCM_GROUP, true);
        final Node projectNode = createNodeIfNecessary(groupNode, project.getName(), NT_HCM_PROJECT, true);
        final Node moduleNode = createNodeIfNecessary(projectNode, module.getName(), NT_HCM_MODULE, true);
        storeBaselineModule(module, moduleNode, session, false);
    }

    /**
     * Update and session saves the stored baseline for a set of modules as an atomic operation.
     * This is primarily used by auto-export, which frequently updates existing modules.
     * This method assumes that the modules already exist and that it is
     * safe to call session.save() at any time without regard to the calling context.
     * @param modules the modules to be updated in the stored baseline
     * @param baselineModel
     * @param session
     */
    protected ConfigurationModelImpl updateBaselineModules(final Collection<ModuleImpl> modules,
                                                           final ConfigurationModelImpl baselineModel,
                                                           final Session session)
            throws RepositoryException, IOException, ParserException {
        configurationLockManager.lock();
        try {
            // Ensure/force cluster synchronization in case another instance just modified the baseline
            session.refresh(true);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            final Node hcmRootNode = session.getNode(HCM_ROOT_PATH);
            final Node baseline = hcmRootNode.getNode(HCM_BASELINE);
            final Node hcmSitesNode = baseline.getNode(HCM_SITES);

            final Set<ModuleImpl> newBaselineModules = new HashSet<>();
            for (final ModuleImpl module : modules) {
                log.debug("Updating module in baseline configuration: {}", module.getFullName());

                // set lastupdated date to now
                baseline.setProperty(HCM_LAST_UPDATED, Calendar.getInstance());

                Node moduleNode;
                if (module.isNotCore()) {
                    final Node hcmSiteNode = hcmSitesNode.getNode(module.getSiteName());
                    moduleNode = getModuleNode(hcmSiteNode, module);
                } else {
                    moduleNode = getModuleNode(baseline, module);
                }
                storeBaselineModule(module, moduleNode, session, true);

                // now that we've saved the baseline, we should clear the dirty flags on all sources
                for (SourceImpl source : module.getSources()) {
                    source.markUnchanged();
                }

                final ModuleImpl reloadedModule = loadModuleDescriptor(moduleNode, module.getSiteName());
                if (reloadedModule != null) {
                    reloadedModule.setHstRoot(module.getHstRoot());
                    parseSources(singletonList(reloadedModule));
                    newBaselineModules.add(reloadedModule);
                }
            }

            // update digest
            // TODO: use separate digests for core and hcm sites; for now, we just care about core
            ConfigurationModelImpl newBaseline = mergeWithSourceModules(newBaselineModules, baselineModel);
            baseline.setProperty(HCM_DIGEST, newBaseline.getDigest(null));

            session.save();
            stopWatch.stop();
            log.info("Updated module in baseline configuration in {}", stopWatch);

            return newBaseline;
        }
        catch (RepositoryException|IOException|ParserException e) {
            log.error("Failed to store baseline configuration", e);
            throw e;
        }
        finally {
            configurationLockManager.unlock();
        }
    }

    /**
     * Helper to get the JCR node within the baseline for a given module, with proper JCR path encoding.
     * @param baseline the root node of the baseline
     * @param module the module whose baseline node we want to find
     * @return the node representing the given module within the baseline
     * @throws RepositoryException because always with JCR ...
     */
    private static Node getModuleNode(final Node baseline, final ModuleImpl module) throws RepositoryException {
        Node groupNode = baseline.getNode(NodeNameCodec.encode(module.getProject().getGroup().getName(), true));
        Node projectNode = groupNode.getNode(NodeNameCodec.encode(module.getProject().getName(), true));
        return projectNode.getNode(NodeNameCodec.encode(module.getName(), true));
    }

    /**
     * Store a single Module into the configuration baseline. This method assumes the locking and session context
     * managed in storeBaseline().
     * @param module the module to store
     * @param moduleNode the JCR node destination for the module
     * @see #storeBaseline(ConfigurationModelImpl, Session)
     */
    protected void storeBaselineModule(final ModuleImpl module, final Node moduleNode, final Session session, final boolean incremental)
            throws RepositoryException, IOException {

        // get the resource input provider, which provides access to raw data for module content
        ResourceInputProvider rip = module.getConfigResourceInputProvider();

        // store the content action sequence number
        final String lastExecutedAction = module.getLastExecutedAction();
        if (StringUtils.isNotBlank(lastExecutedAction)) {
            moduleNode.setProperty(HCM_LAST_EXECUTED_ACTION, lastExecutedAction);
        }

        // create descriptor node, if necessary
        Node descriptorNode = createNodeIfNecessary(moduleNode, HCM_MODULE_DESCRIPTOR, NT_HCM_DESCRIPTOR, false);

        // AFAIK, a module MUST have a descriptor, but check here for a malformed package or special case
        // TODO the "/../" is an ugly hack because RIP actually treats absolute paths as relative to config base, not module base
        if (rip.hasResource(null, "/../" + HCM_MODULE_YAML)) {
            // open descriptor InputStream
            InputStream is = rip.getResourceInputStream(null, "/../" + HCM_MODULE_YAML);

            // store yaml and digest (this call will close the input stream)
            storeString(is, descriptorNode, HCM_YAML);
        }
        else {
            // a descriptor must exist, or this isn't a valid baseline
            // TODO: throw an appropriate exception if this is to be forbidden
//            throw new IllegalStateException("Module descriptor does not exist for module: "+module.getFullName());

            // if descriptor doesn't exist,
            String dummyDescriptor = module.compileDummyDescriptor();

            // write that back to the YAML property and digest it
            storeString(IOUtils.toInputStream(dummyDescriptor, StandardCharsets.UTF_8), descriptorNode, HCM_YAML);
        }

        // if this Module has an actions file...
        // TODO the "/../" is an ugly hack because RIP actually treats absolute paths as relative to config base, not module base
        if (rip.hasResource(null, "/../" + ACTIONS_YAML)) {
            // create actions node, if necessary
            Node actionsNode = createNodeIfNecessary(moduleNode, HCM_ACTIONS, NT_HCM_ACTIONS, false);

            // open actions InputStream
            InputStream is = rip.getResourceInputStream(null, "/../" + ACTIONS_YAML);

            // store yaml and digest (this call will close the input stream)
            storeString(is, actionsNode, HCM_YAML);
        }

        // always create the config root node, since we need it to setup the RIP, and that's needed later
        // TODO this is an ugly hack because source.getPath() is actually relative to config root, not module root
        Node configRootNode = createNodeIfNecessary(moduleNode, HCM_CONFIG_FOLDER, NT_HCM_CONFIG_FOLDER, false);

        // delete removed resources, which might include removed sources

        if (incremental) {
            log.debug("removing config resources: \n\t{}", String.join("\n\t", module.getRemovedConfigResources()));
            log.debug("removing content resources: \n\t{}", String.join("\n\t", module.getRemovedContentResources()));

            for (String removed : module.getRemovedConfigResources()) {
                final String relPath = removed.substring(1);
                if (configRootNode.hasNode(relPath)) {
                    configRootNode.getNode(relPath).remove();
                }
            }
            if (moduleNode.hasNode(HCM_CONTENT_FOLDER)) {
                final Node contentRootNode = moduleNode.getNode(HCM_CONTENT_FOLDER);
                for (String removed : module.getRemovedContentResources()) {
                    final String relPath = removed.substring(1);
                    if (contentRootNode.hasNode(relPath)) {
                        contentRootNode.getNode(relPath).remove();
                    }
                }
            }
        }

        // foreach content source
        for (ContentSourceImpl source : module.getContentSources()) {
            // short-circuit processing if we're in incremental update mode and the source hasn't changed
            if (incremental && !source.hasChangedSinceLoad()) {
                continue;
            }

            // TODO this is an ugly hack because source.getPath() is actually relative to content root, not module root
            // create the content root node, if necessary
            Node contentRootNode = createNodeIfNecessary(moduleNode, HCM_CONTENT_FOLDER, NT_HCM_CONTENT_FOLDER, false);

            // create folder nodes, if necessary
            Node sourceNode = createNodeAndParentsIfNecessary(source.getPath(), contentRootNode,
                    NT_HCM_CONTENT_FOLDER, NT_HCM_CONTENT_SOURCE);

            // assume that there is exactly one content definition here, as required
            ContentDefinitionImpl firstDef = source.getContentDefinition();

            // set content path property and order before
            sourceNode.setProperty(HCM_CONTENT_PATH, firstDef.getNode().getPath());
            String orderBefore = firstDef.getNode().getOrderBefore();
            String encodedOrderBefore =
                    META_ORDER_BEFORE_FIRST.equals(orderBefore) ? HCM_CONTENT_ORDER_BEFORE_FIRST : orderBefore;
            sourceNode.setProperty(HCM_CONTENT_ORDER_BEFORE, encodedOrderBefore);

            if (incremental) {
                final String contentNodePath = firstDef.getNode().getPath();
                final boolean nodeAlreadyProcessed = getAppliedContentPaths(session).contains(contentNodePath);
                if (!nodeAlreadyProcessed) {
                    addAppliedContentPath(contentNodePath, session);
                }

            }
        }

        // foreach config source
        for (ConfigSourceImpl source : module.getConfigSources()) {
            // short-circuit processing if we're in incremental update mode and the source hasn't changed
            if (incremental && !source.hasChangedSinceLoad()) {
                continue;
            }

            // process in detail ...
            storeBaselineConfigSource(source, configRootNode, rip);
        }
    }

    /**
     * Store a single config definition Source into the baseline. This method assumes the locking and session context
     * managed in storeBaseline().
     * @param source the Source to store
     * @param configRootNode the JCR node destination for the config Sources and resources
     * @param rip provides access to raw data streams
     * @see #storeBaseline(ConfigurationModelImpl, Session)
     */
    protected void storeBaselineConfigSource(final ConfigSourceImpl source, final Node configRootNode, final ResourceInputProvider rip)
            throws RepositoryException, IOException {

        // create folder nodes, if necessary
        String sourcePath = source.getPath();
        Node sourceNode = createNodeAndParentsIfNecessary(sourcePath, configRootNode,
                NT_HCM_CONFIG_FOLDER, NT_HCM_DEFINITIONS);

        // open source yaml InputStream
        InputStream is = rip.getResourceInputStream(null, "/" + sourcePath);

        // store yaml and digest (this call will close the input stream)
        storeString(is, sourceNode, HCM_YAML);

        // foreach definition
        for (Definition def : source.getDefinitions()) {
            switch (def.getType()) {
                case NAMESPACE:
                    NamespaceDefinitionImpl namespaceDefinition = (NamespaceDefinitionImpl) def;
                    if (namespaceDefinition.getCndPath() != null) {
                        String cndPath = namespaceDefinition.getCndPath().getString();

                        // create folder nodes, if necessary
                        Node cndNode = createNodeAndParentsIfNecessary(cndPath, baseForPath(cndPath, sourceNode, configRootNode),
                                NT_HCM_CONFIG_FOLDER, NT_HCM_CND);

                        // open cnd resource InputStream
                        InputStream cndIS = rip.getResourceInputStream(source, cndPath);

                        // store cnd and digest (this call will close the input stream)
                        storeString(cndIS, cndNode, HCM_CND);
                    }
                case WEBFILEBUNDLE:
                    // no special processing required
                    break;
                case CONTENT:
                    // this shouldn't exist here anymore, but we'll let the verifier handle it
                    break;
                case CONFIG:
                    ConfigDefinitionImpl configDef = (ConfigDefinitionImpl) def;

                    // recursively find all resources, make nodes, store binary and digest
                    configDef.getNode().visitResources(value ->
                            storeBinaryResourceIfNecessary(value, sourceNode, configRootNode));
                    break;
            }
        }
    }

    /**
     * For the given Value, check if it is a resource reference, and if so, store the referenced resource content
     * as an appropriate binary resource node in the baseline.
     * @param value the Value to check for a resource reference
     * @param sourceNode the JCR Node where source is stored in the baseline
     * @param configRootNode the JCR node destination for the config Sources and resources
     */
    protected void storeBinaryResourceIfNecessary(ValueImpl value, Node sourceNode, Node configRootNode)
            throws RepositoryException, IOException {
        if (value.isResource()) {
            // create nodes, if necessary
            String path = value.getString();
            Node resourceNode = createNodeAndParentsIfNecessary(path, baseForPath(path, sourceNode, configRootNode),
                    NT_HCM_CONFIG_FOLDER, NT_HCM_BINARY);

            // open cnd resource InputStream
            InputStream is = value.getResourceInputStream();

            // store binary and digest
            storeBinary(is, resourceNode);
        }
    }

    /**
     * Determine if this is a module or source-relative path (check leading / or not).
     * @param path the path to check
     * @param sourceNode base node for source
     * @param configRootNode the JCR node destination for the config Sources and resources
     * @return either moduleNode iff path has a leading /, else sourceNode.getParent()
     */
    private Node baseForPath(String path, Node sourceNode, Node configRootNode) throws RepositoryException {
        if (path.startsWith("/")) {
            return configRootNode;
        }
        else {
            return sourceNode.getParent();
        }
    }

    /**
     * Check for the existence of a node by name and return or create-and-return it, as necessary.
     * @param parent the parent node of the node we need
     * @param name the name of the node we need
     * @param type the JCR primary type of the node we need -- NOTE: existing nodes of any type will be accepted
     * @param encode iff true, encode the name of the node to make it safe for use as a JCR Name
     * @return the existing or new node of interest
     * @throws RepositoryException because everything in the JCR API throws this ...
     */
    protected Node createNodeIfNecessary(Node parent, String name, String type, boolean encode) throws RepositoryException {
        if (encode) {
            name = NodeNameCodec.encode(name);
        }
        if (!parent.hasNode(name)) {
            parent.addNode(name, type);
        }
        return parent.getNode(name);
    }

    /**
     * Create JCR nodes for a Source or resource file and its parents, as necessary, for storing a baseline.
     * @param sourcePath the path of the item to be stored
     * @param baseNode the base node from which the sourcePath is relative
     * @param folderType the JCR primary type for folder parents of the item
     * @param sourceType the JCR primary type for the item itself
     * @return the Node for the item
     * @throws RepositoryException because everything in the JCR API throws this ...
     */
    protected Node createNodeAndParentsIfNecessary(String sourcePath, Node baseNode, String folderType, String sourceType) throws RepositoryException {
        // Strip leading / and treat all paths as relative to baseNode
        sourcePath = StringUtils.stripStart(sourcePath, "/");
        String[] sourceSegments = sourcePath.split("/");
        Node parentNode = baseNode;

        // the final segment is the item itself, which needs a different node type
        for (int i = 0; i < sourceSegments.length-1; i++) {
            String segment = sourceSegments[i];
            parentNode = createNodeIfNecessary(parentNode, segment, folderType, true);
        }

        // create the item node, if necessary
        return createNodeIfNecessary(parentNode, sourceSegments[sourceSegments.length-1], sourceType, true);
    }

    /**
     * Stores the content of the given InputStream into a binary "jcr:data" property of the given Node, then closes
     * the InputStream.
     * @param is the InputStream whose contents will be stored
     * @param resourceNode the JCR Node where the content will be stored
     */
    protected void storeBinary(InputStream is, Node resourceNode) throws IOException, RepositoryException {
        // store content as Binary
        BufferedInputStream bis = new BufferedInputStream(is);
        Binary bin = resourceNode.getSession().getValueFactory().createBinary(bis);
        resourceNode.setProperty(JcrConstants.JCR_DATA, bin);
    }

    /**
     * Stores the content of the given InputStream into a String property of the given Node, then closes
     * the InputStream.
     * @param is the InputStream whose contents will be stored
     * @param resourceNode the JCR Node where the content will be stored
     * @param propName the property name where the content will be stored
     */
    protected void storeString(InputStream is, Node resourceNode, String propName)
            throws IOException, RepositoryException {
        // use try-with-resource to close the reader and therefore the input stream
        try (Reader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            // store content as String
            String txt = IOUtils.toString(isr);
            resourceNode.setProperty(propName, txt);
        }
    }

    /**
     * Load a (partial) ConfigurationModel from the stored configuration baseline in the JCR. This model will not contain
     * content definitions, which are not stored in the baseline.
     * @param session the session to load the baseline with
     * @param hcmSites the set of HCM Sites to include when loading the baseline (may be null or empty to include only core)
     * @throws Exception
     */
    public ConfigurationModelImpl loadBaseline(final Session session, Set<String> hcmSites) throws RepositoryException, ParserException, IOException {
        ConfigurationModelImpl result;

        final Node hcmRootNode = session.getNode(HCM_ROOT_PATH);
        // if the baseline node doesn't exist yet...
        if (!hcmRootNode.hasNode(HCM_BASELINE)) {
            // ... there's nothing to load
            log.info("Attempted to load baseline, but no baseline node exists yet");
            result = null;
        }
        else {
            configurationLockManager.lock();
            try {
                // Ensure/force cluster synchronization in case another instance just modified the baseline
                session.refresh(true);
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                // otherwise, if the baseline node DOES exist...
                final Node baselineNode = hcmRootNode.getNode(HCM_BASELINE);

                // make sure we load core modules, in addition to specified HCM Sites
                // First phase: load and parse core module descriptors
                final List<ModuleImpl> modules = parseDescriptors(baselineNode, null);

                if (isNotEmpty(hcmSites) && baselineNode.hasNode(HCM_SITES)) {
                    final Node hcmSiteCatalogNode = baselineNode.getNode(HCM_SITES);
                    for (NodeIterator eni = hcmSiteCatalogNode.getNodes(); eni.hasNext();) {
                        final Node hcmSiteNode = eni.nextNode();
                        final String hcmSiteName = hcmSiteNode.getName();
                        if (hcmSites.contains(hcmSiteName)) {
                            final String hstRoot = hcmSiteNode.getProperty(HCM_HSTROOT).getString();
                            final List<ModuleImpl> hcmSiteModules = parseDescriptors(hcmSiteNode, hcmSiteName);
                            hcmSiteModules.forEach(module -> module.setHstRoot(JcrPaths.getPath(hstRoot)));
                            modules.addAll(hcmSiteModules);
                        }
                    }
                }

                // Second phase: load and parse config Sources, load and mockup content Sources
                parseSources(modules);

                // build the final merged model
                try (ConfigurationModelImpl model = new ConfigurationModelImpl()) {
                    modules.forEach(model::addModule);
                    result = model.build();
                    stopWatch.stop();
                }
                log.info("ConfigurationModel loaded from baseline configuration in {}", stopWatch);
            }
            catch (RepositoryException|ParserException|IOException e) {
                log.error("Failed to load baseline configuration", e);
                throw e;
            }
            finally {
                configurationLockManager.unlock();
            }
        }
        return result;
    }

    /**
     * First phase of loading a baseline: loading and parsing module descriptors. Accumulates results in rips and groups.
     * @param baselineNode the base node for the entire stored configuration baseline
     * @return List of configuration modules loaded here
     * @throws RepositoryException
     * @throws ParserException
     */
    protected List<ModuleImpl> parseDescriptors(final Node baselineNode, final String hcmSiteName)
            throws RepositoryException, ParserException {
        // groups accumulator
        final List<ModuleImpl> modules = new ArrayList<>();

        // for each module node under this baseline
        for (Node moduleNode : findModuleNodes(baselineNode)) {
            final ModuleImpl module = loadModuleDescriptor(moduleNode, hcmSiteName);
            if (module != null) {
                modules.add(module);
            }
        }

        log.debug("After parsing descriptors, we have {} modules", modules.size());

        return modules;
    }

    /**
     * Helper for {@link #parseDescriptors(Node,String)} -- loads one module descriptor from a given module baseline node.
     * @param moduleNode the JCR node where the baseline for a module has been previously stored
     * @throws RepositoryException
     * @throws ParserException
     */
    protected ModuleImpl loadModuleDescriptor(final Node moduleNode, final String hcmSiteName) throws RepositoryException, ParserException {
        final ModuleImpl module;

        Node descriptorNode = moduleNode.getNode(HCM_MODULE_DESCRIPTOR);

        // if descriptor exists
        final String descriptor = descriptorNode.getProperty(HCM_YAML).getString();
        if (StringUtils.isNotEmpty(descriptor)) {
            // parse descriptor with ModuleDescriptorParser
            InputStream is = IOUtils.toInputStream(descriptor, StandardCharsets.UTF_8);
            module = new ModuleDescriptorParser(DEFAULT_EXPLICIT_SEQUENCING)
                    .parse(is, moduleNode.getPath(), hcmSiteName);

            // TODO: determine and set actual HCM Site name

            final JcrPath modulePath = JcrPaths.getPath(moduleNode.getPath());
            final JcrPath expectedPath = modulePath.subpath(0, modulePath.getSegmentCount() - 3)
                    .resolve(module.getProject().getGroup().getName())
                    .resolve(module.getProject().getName())
                    .resolve(module.getName());

            if (!expectedPath.equals(modulePath)) {
                log.error("Detected baseline module path inconsistency, expected module location: '{}', but actual is: '{}', skipping",
                        expectedPath, moduleNode.getPath());
                return null;
            }

            final String lastExecutedAction = getLastExecutedAction(moduleNode);
            module.setLastExecutedAction(lastExecutedAction);

            log.debug("Building module from descriptor: {}/{}/{}",
                    module.getProject().getGroup().getName(), module.getProject().getName(), module.getName());
        }
        else {
            // this should no longer happen, since we generate dummy descriptors when saving the baseline
            throw new ConfigurationRuntimeException("Module found in baseline with empty descriptor: " + moduleNode.getPath());
        }

        // store RIPs for later use
        if (moduleNode.hasNode(HCM_CONFIG_FOLDER)) {
            // note: we need this to always be true, so that we can load the descriptor etc
            module.setConfigResourceInputProvider(new BaselineResourceInputProvider(moduleNode.getNode(HCM_CONFIG_FOLDER)));
        }
        if (moduleNode.hasNode(HCM_CONTENT_FOLDER)) {
            module.setContentResourceInputProvider(new BaselineResourceInputProvider(moduleNode.getNode(HCM_CONTENT_FOLDER)));
        }

        return module;
    }

    private String getLastExecutedAction(final Node moduleNode) throws RepositoryException {
        if (moduleNode.hasProperty(HCM_LAST_EXECUTED_ACTION)) {
            return moduleNode.getProperty(HCM_LAST_EXECUTED_ACTION).getString();
        }
        // backwards compatibility with baselines stored during v12-beta
        else if (moduleNode.hasProperty(HCM_MODULE_SEQUENCE)) {
            return Double.toString(moduleNode.getProperty(HCM_MODULE_SEQUENCE).getDouble());
        } else {
            return null;
        }
    }

    /**
     * Second phase of loading a baseline: loading and parsing config Sources and reconstructing minimal content
     * Source mockups (containing only the root definition path).
     * @param modules accumulator list from first phase
     * @throws RepositoryException
     * @throws IOException
     * @throws ParserException
     */
    protected void parseSources(final List<ModuleImpl> modules) throws RepositoryException, IOException, ParserException {
        // for each module
        for (ModuleImpl module : modules) {
            final ProjectImpl project = module.getProject();
            final GroupImpl group = project.getGroup();

            log.debug("Parsing sources from baseline for {}/{}/{}",
                    group.getName(), project.getName(), module.getName());

            BaselineResourceInputProvider rip = (BaselineResourceInputProvider) module.getConfigResourceInputProvider();
            if (rip == null) {
                log.debug("No {} folder in {}/{}/{}", HCM_CONFIG_FOLDER,
                        group.getName(), project.getName(), module.getName());
            }
            else {
                ConfigSourceParser parser = new ConfigSourceParser(rip);
                Node configFolderNode = rip.getBaseNode();

                // for each config source
                final List<Node> configSourceNodes = rip.getConfigSourceNodes();
                log.debug("Found {} config sources in {}/{}/{}", configSourceNodes.size(),
                        group.getName(), project.getName(), module.getName());

                for (Node configNode : configSourceNodes) {
                    // compute config-root-relative path
                    String sourcePath = StringUtils.removeStart(configNode.getPath(), configFolderNode.getPath() + "/");

                    // unescape JCR-illegal chars here, since resource paths are intended to be filesystem style paths
                    sourcePath = NodeNameCodec.decode(sourcePath);

                    log.debug("Loading config from {} in {}/{}/{}", sourcePath,
                            group.getName(), project.getName(), module.getName());

                    // get InputStream
                    // TODO adding the slash here is a silly hack to load a source path without needing the source first
                    InputStream is = rip.getResourceInputStream(null, "/" + sourcePath);

                    // parse config source
                    parser.parse(is, sourcePath, configNode.getPath(), module);
                }
            }

            // for each content source
            rip = (BaselineResourceInputProvider) module.getContentResourceInputProvider();
            if (rip == null) {
                log.debug("No {} folder in {}/{}/{}", HCM_CONTENT_FOLDER,
                        group.getName(), project.getName(), module.getName());
            }
            else {
                Node contentFolderNode = rip.getBaseNode();

                final List<Node> contentSourceNodes = rip.getContentSourceNodes();
                log.debug("Found {} content sources in {}/{}/{}", contentSourceNodes.size(),
                        group.getName(), project.getName(), module.getName());

                for (Node contentNode : contentSourceNodes) {
                    // compute content-root-relative path
                    String sourcePath = StringUtils.removeStart(contentNode.getPath(), contentFolderNode.getPath() + "/");

                    // unescape JCR-illegal chars here, since resource paths are intended to be filesystem style paths
                    sourcePath = NodeNameCodec.decode(sourcePath);

                    log.debug("Building content def from {} in {}/{}/{}", sourcePath,
                            group.getName(), project.getName(), module.getName());

                    // get content path and order before from JCR Node
                    String contentPath = contentNode.getProperty(HCM_CONTENT_PATH).getString();
                    String orderBefore = getOrderBefore(contentNode);

                    // create Source
                    // create ContentDefinition with a single definition node and just the node path and the order before
                    module.addContentSource(sourcePath).addContentDefinition(contentPath, orderBefore);
                }
            }
        }
    }

    private String getOrderBefore(final Node node) throws RepositoryException {
        if (!node.hasProperty(HCM_CONTENT_ORDER_BEFORE)) {
            return null;
        }

        final String orderBefore = node.getProperty(HCM_CONTENT_ORDER_BEFORE).getString();

        if (HCM_CONTENT_ORDER_BEFORE_FIRST.equals(orderBefore)) {
            return META_ORDER_BEFORE_FIRST;
        }

        return orderBefore;
    }

    /**
     * Helper method to find all hcm:module nodes under a hcm:baseline node.
     * @param baselineNode the base under which to search
     * @return a List of Nodes of hcm:module type
     * @throws RepositoryException
     */
    protected List<Node> findModuleNodes(Node baselineNode) throws RepositoryException {
        List<Node> moduleNodes = new ArrayList<>();

        // for each group node
        for (NodeIterator gni = baselineNode.getNodes(); gni.hasNext();) {
            Node possibleGroup = gni.nextNode();

            if (possibleGroup.getPrimaryNodeType().isNodeType(NT_HCM_GROUP)) {
                // for each project node
                for (NodeIterator pni = possibleGroup.getNodes(); pni.hasNext(); ) {
                    Node possibleProject = pni.nextNode();
                    if (possibleProject.getPrimaryNodeType().isNodeType(NT_HCM_PROJECT)) {
                        // for each module node
                        for (NodeIterator mni = possibleProject.getNodes(); mni.hasNext(); ) {
                            Node possibleModule = mni.nextNode();
                            if (possibleModule.getPrimaryNodeType().isNodeType(NT_HCM_MODULE)) {
                                // accumulate
                                moduleNodes.add(possibleModule);
                            }
                        }
                    }
                }
            }
        }

        log.debug("Found {} modules in baseline", moduleNodes.size());
        return moduleNodes;
    }

    /**
     * Clean up empty groups and projects after storing a new baseline.
     * @param baselineNode the root node of the baseline
     * @throws RepositoryException
     */
    protected void removeEmptyGroupsAndProjects(Node baselineNode) throws RepositoryException {
        // for each group node
        for (NodeIterator gni = baselineNode.getNodes(); gni.hasNext();) {
            Node possibleGroup = gni.nextNode();
            if (possibleGroup.getPrimaryNodeType().isNodeType(NT_HCM_GROUP)) {
                // for each project node
                for (NodeIterator pni = possibleGroup.getNodes(); pni.hasNext(); ) {
                    Node possibleProject = pni.nextNode();
                    if (possibleProject.getPrimaryNodeType().isNodeType(NT_HCM_PROJECT)) {
                        // if project is empty, remove it
                        if (!possibleProject.hasNodes()) {
                            possibleProject.remove();
                        }
                    }
                }

                // if group is empty, remove it
                if (!possibleGroup.hasNodes()) {
                    possibleGroup.remove();
                }
            }
        }
    }

    /**
     * Obtain a flat set of all content paths applied in the past
     */
    Set<String> getAppliedContentPaths(final Session session) throws RepositoryException {
        final Set<String> appliedContentPaths = new LinkedHashSet<>();

        if (session.nodeExists(HCM_CONTENT_NODE_PATH)) {
            final Node hcmContentRoot = session.getNode(HCM_CONTENT_NODE_PATH);
            if (hcmContentRoot.hasProperty(HCM_CONTENT_PATHS_APPLIED)) {
                final Property pathsApplied = hcmContentRoot.getProperty(HCM_CONTENT_PATHS_APPLIED);
                for (final javax.jcr.Value value : pathsApplied.getValues()) {
                    appliedContentPaths.add(value.getString());
                }
            }
        }
        return appliedContentPaths;
    }

    /**
     * Obtain a bundle digests stored at baseline
     */
    public Map<String, String> getBundlesDigests(final Session session) throws RepositoryException {
        final Map<String, String> bundles = new LinkedHashMap<>();

        if (session.nodeExists(HCM_BUNDLE_NODE_PATH)) {
            final Node hcmContentRoot = session.getNode(HCM_BUNDLE_NODE_PATH);
            if (hcmContentRoot.hasProperty(HCM_BUNDLES_DIGESTS)) {
                final Property bundleDigests = hcmContentRoot.getProperty(HCM_BUNDLES_DIGESTS);
                for (final javax.jcr.Value value : bundleDigests.getValues()) {
                    bundles.put(StringUtils.substringBefore(value.getString(), ":"), StringUtils.substringAfter(value.getString(), ":"));
                }
            }
        }
        return bundles;
    }

    /**
     * Get digest for web bundle
     * @param bundleName name of the bundle
     * @param session the session to use
     */
    public Optional<String> getBaselineBundleDigest(final String bundleName, final Session session) throws RepositoryException {
        final Map<String, String> bundlesDigests = getBundlesDigests(session);
        final String digest = bundlesDigests.get(bundleName);
        return StringUtils.isNotEmpty(digest) ? Optional.of(digest) : Optional.empty();
    }

    /**
     * Adds or updates bundle digest at baseline
     * @param bundleName webfile bundle name
     * @param bundleDigest webfile bundle digest
     * @param session the session to use
     */
    void addOrUpdateBundleDigest(final String bundleName, final String bundleDigest, final Session session) throws RepositoryException {
        final Map<String, String> bundlesDigests = getBundlesDigests(session);
        bundlesDigests.put(bundleName, bundleDigest);
        final List<String> bundles = bundlesDigests.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(toList());
        final String[] newPaths = bundles.toArray(new String[bundles.size()]);
        session.getNode(HCM_BUNDLE_NODE_PATH).setProperty(HCM_BUNDLES_DIGESTS, newPaths);
        //TODO SS: In case of save failure, refresh session without keeping changes
        session.save();
    }

    /**
     * Adds and session saves a content path to (the end of) the set of all content paths applied.
     * Note: will <em>always</em> save the outstanding session changes, even if the path already is on the set.
     * @param path the path to add
     * @param session the session to use
     */
    void addAppliedContentPath(final String path, final Session session) throws RepositoryException {
        final Set<String> appliedPaths = getAppliedContentPaths(session);
        if (!appliedPaths.contains(path)) {
            appliedPaths.add(path);
            final String[] newPaths = appliedPaths.toArray(new String[appliedPaths.size()]);
            session.getNode(HCM_CONTENT_NODE_PATH).setProperty(HCM_CONTENT_PATHS_APPLIED, newPaths);
        }
        session.save();
    }

    /**
     * Create the hcm:content node for registering applied content paths.
     *
     * @param session the JCR Session to use
     */
    void createContentNode(final Session session) throws RepositoryException {
        session.getNode(HCM_ROOT_PATH).addNode(NT_HCM_CONTENT, NT_HCM_CONTENT);
        session.save();
    }

    /**
     * Check if the content node already exists
     *
     * @param session the JCR Session to use
     * @return true if the node exists, false otherwise
     */
    boolean contentNodeExists(final Session session) throws RepositoryException {
        return session.nodeExists(HCM_CONTENT_NODE_PATH);
    }

    /**
     * Updates and session saves a module's stored last-executed-action string to indicate that all content actions have been processed
     * Note: will <em>always</em> save the outstanding session changes, even if the module didn't have an action string to store.
     */
    void updateLastExecutedActionForModule(final ModuleImpl module, final Session session) throws RepositoryException {
        final Optional<String> latestAction = module.getActionsMap().keySet().stream().map(MavenComparableVersion::new)
                .max(MavenComparableVersion::compareTo).map(Object::toString);
        if (latestAction.isPresent() && StringUtils.isNotBlank(latestAction.get())) {
            module.setLastExecutedAction(latestAction.get());

            Node moduleNode = getModuleNode(session.getNode(HCM_BASELINE_PATH), module);
            moduleNode.setProperty(HCM_LAST_EXECUTED_ACTION, latestAction.get());
        }
        session.save();
    }
}
