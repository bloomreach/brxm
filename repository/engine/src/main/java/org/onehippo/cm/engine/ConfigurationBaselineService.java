/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.ConfigDefinition;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.NamespaceDefinition;
import org.onehippo.cm.model.Project;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.parser.ConfigSourceParser;
import org.onehippo.cm.model.parser.AggregatedModulesDescriptorParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.HCM_ACTIONS;
import static org.onehippo.cm.engine.Constants.HCM_BASELINE;
import static org.onehippo.cm.engine.Constants.HCM_BASELINE_PATH;
import static org.onehippo.cm.engine.Constants.HCM_CND;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_NODE_PATH;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_PATH;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_PATHS_APPLIED;
import static org.onehippo.cm.engine.Constants.HCM_DIGEST;
import static org.onehippo.cm.engine.Constants.HCM_LAST_UPDATED;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_DESCRIPTOR;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_SEQUENCE;
import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;
import static org.onehippo.cm.engine.Constants.HCM_YAML;
import static org.onehippo.cm.engine.Constants.NT_HCM_ACTIONS;
import static org.onehippo.cm.engine.Constants.NT_HCM_BASELINE;
import static org.onehippo.cm.engine.Constants.NT_HCM_BINARY;
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
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;
import static org.onehippo.cm.model.Constants.ACTIONS_YAML;
import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_MODULE_YAML;

public class ConfigurationBaselineService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationBaselineService.class);

    private final Session configurationServiceSession;
    private final ConfigurationLockManager configurationLockManager;

    public ConfigurationBaselineService(final Session configurationServiceSession, final ConfigurationLockManager configurationLockManager) {
        this.configurationServiceSession = configurationServiceSession;
        this.configurationLockManager = configurationLockManager;
    }

    /**
     * Store a merged configuration model as a baseline configuration in the JCR.
     * The provided ConfigurationModel is assumed to be fully formed and validated.
     * @param model the configuration model to store as the new baseline
     */
    public void storeBaseline(final ConfigurationModel model) throws RepositoryException, IOException {
        configurationLockManager.lock();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            final Node hcmRootNode = configurationServiceSession.getNode(HCM_ROOT_PATH);

            Node baseline = createNodeIfNecessary(hcmRootNode, HCM_BASELINE, NT_HCM_BASELINE, false);

            // TODO: implement a smarter partial-update process instead of brute-force removal
            // clear existing group nodes before creating new ones
            for (NodeIterator nodes = baseline.getNodes(); nodes.hasNext();) {
                Node groupNode = nodes.nextNode();
                groupNode.remove();
            }

            // set lastupdated date to now
            baseline.setProperty(HCM_LAST_UPDATED, Calendar.getInstance());

            // compute and store digest from model manifest
            // Note: we've decided not to worry about processing data twice, since we don't expect large files
            //       in the config portion, and content is already optimized to use content path instead of digest
            String modelDigestString = model.getDigest();
            baseline.setProperty(HCM_DIGEST, modelDigestString);

            // create group, project, and module nodes, if necessary
            // foreach group
            for (Group group : model.getSortedGroups()) {
                Node groupNode = createNodeIfNecessary(baseline, group.getName(), NT_HCM_GROUP, true);

                // foreach project
                for (Project project : group.getProjects()) {
                    Node projectNode = createNodeIfNecessary(groupNode, project.getName(), NT_HCM_PROJECT, true);

                    // foreach module
                    for (Module module : project.getModules()) {
                        Node moduleNode = createNodeIfNecessary(projectNode, module.getName(), NT_HCM_MODULE, true);

                        // process each module in detail
                        storeBaselineModule(module, moduleNode);
                    }
                }
            }

            configurationServiceSession.save();
            stopWatch.stop();
            log.info("ConfigurationModel stored as baseline configuration in {}", stopWatch.toString());
        }
        catch (RepositoryException|IOException e) {
            log.error("Failed to store baseline configuration", e);
            throw e;
        }
        finally {
            configurationLockManager.unlock();
        }
    }

    /**
     * Update the stored baseline for a single module as an atomic operation. This is primarily used by auto-export,
     * which frequently updates existing modules. This method assumes that the module already exists and that it is
     * save to call session.save() at any time without regard to the calling context.
     * @param module the module to be updated in the stored baseline
     */
    protected ConfigurationModelImpl updateBaselineModules(final Collection<ModuleImpl> modules)
            throws RepositoryException, IOException, ParserException {
        configurationLockManager.lock();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            final Node hcmRootNode = configurationServiceSession.getNode(HCM_ROOT_PATH);
            Node baseline = hcmRootNode.getNode(HCM_BASELINE);

            for (final ModuleImpl module : modules) {
                log.debug("Updating module in baseline configuration: {}", module.getFullName());

                // set lastupdated date to now
                baseline.setProperty(HCM_LAST_UPDATED, Calendar.getInstance());

                Node groupNode = baseline.getNode(NodeNameCodec.encode(module.getProject().getGroup().getName()));
                Node projectNode = groupNode.getNode(NodeNameCodec.encode(module.getProject().getName()));
                Node moduleNode = projectNode.getNode(NodeNameCodec.encode(module.getName()));

                storeBaselineModule(module, moduleNode);
            }

            // update digest
            ConfigurationModelImpl newBaseline = loadBaseline();
            baseline.setProperty(HCM_DIGEST, newBaseline.getDigest());

            configurationServiceSession.save();
            stopWatch.stop();
            log.info("Updated module in baseline configuration in {}", stopWatch.toString());

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
     * Store a single Module into the configuration baseline. This method assumes the locking and session context
     * managed in storeBaseline().
     * @param module the module to store
     * @param moduleNode the JCR node destination for the module
     * @see #storeBaseline(ConfigurationModel)
     */
    protected void storeBaselineModule(Module module, Node moduleNode) throws RepositoryException, IOException {

        // get the resource input provider, which provides access to raw data for module content
        ResourceInputProvider rip = module.getConfigResourceInputProvider();

        // create descriptor node, if necessary
        Node descriptorNode = createNodeIfNecessary(moduleNode, HCM_MODULE_DESCRIPTOR, NT_HCM_DESCRIPTOR, false);

        final Double sequenceNumber = module.getSequenceNumber();
        if (sequenceNumber != null) {
            moduleNode.setProperty(HCM_MODULE_SEQUENCE, sequenceNumber);
        }

        // AFAIK, a module MUST have a descriptor, but check here for a malformed package or special case
        // TODO the "/../" is an ugly hack because RIP actually treats absolute paths as relative to config base, not module base
        if (rip.hasResource(null, "/../" + HCM_MODULE_YAML)) {
            // open descriptor InputStream
            InputStream is = rip.getResourceInputStream(null, "/../" + HCM_MODULE_YAML);

            // store yaml and digest (this call will close the input stream)
            storeString(is, descriptorNode, HCM_YAML);
        }
        else {
            // if descriptor doesn't exist,
            // TODO: throw an appropriate exception if this is to be forbidden, once demo config is reorganized
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

        // foreach content source
        for (Source source : module.getContentSources()) {
            // TODO this is an ugly hack because source.getPath() is actually relative to content root, not module root
            // create the content root node, if necessary
            Node contentRootNode = createNodeIfNecessary(moduleNode, HCM_CONTENT_FOLDER, NT_HCM_CONTENT_FOLDER, false);

            // create folder nodes, if necessary
            Node sourceNode = createNodeAndParentsIfNecessary(source.getPath(), contentRootNode,
                    NT_HCM_CONTENT_FOLDER, NT_HCM_CONTENT_SOURCE);

            // assume that there is exactly one content definition here, as required
            ContentDefinition firstDef = (ContentDefinition) source.getDefinitions().get(0);

            // set content path property
            sourceNode.setProperty(HCM_CONTENT_PATH, firstDef.getNode().getPath());
        }

        // foreach config source
        for (Source source : module.getConfigSources()) {
            // TODO this is an ugly hack because source.getPath() is actually relative to config root, not module root
            // create the config root node, if necessary
            Node configRootNode = createNodeIfNecessary(moduleNode, HCM_CONFIG_FOLDER, NT_HCM_CONFIG_FOLDER, false);

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
     * @see #storeBaseline(ConfigurationModel)
     */
    protected void storeBaselineConfigSource(final Source source, final Node configRootNode, final ResourceInputProvider rip)
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
                    NamespaceDefinition namespaceDefinition = (NamespaceDefinition)def;
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
                    ConfigDefinition configDef = (ConfigDefinition) def;

                    // recursively find all resources, make nodes, store binary and digest
                    storeResourcesForNode(configDef.getNode(), sourceNode, configRootNode);
                    break;
            }
        }
    }

    /**
     * Recursively find resource references in properties of a DefinitionNode and its children nodes and store the
     * referenced resource content in the configuration baseline.
     * @param defNode a DefinitionNode to search for resource references
     * @param sourceNode the JCR Node where source is stored in the baseline
     * @param configRootNode the JCR node destination for the config Sources and resources
     */
    protected void storeResourcesForNode(DefinitionNode defNode, Node sourceNode, Node configRootNode)
            throws RepositoryException, IOException {

        // find resource values
        for (DefinitionProperty dp : defNode.getProperties().values()) {
            switch (dp.getType()) {
                case SINGLE:
                    storeBinaryResourceIfNecessary(dp.getValue(), sourceNode, configRootNode);
                    break;
                case SET:
                case LIST:
                    for (Value value : dp.getValues()) {
                        storeBinaryResourceIfNecessary(value, sourceNode, configRootNode);
                    }
                    break;
            }
        }

        // recursively visit child definition nodes
        for (DefinitionNode dn : defNode.getNodes().values()) {
            storeResourcesForNode(dn, sourceNode, configRootNode);
        }
    }

    /**
     * For the given Value, check if it is a resource reference, and if so, store the referenced resource content
     * as an appropriate binary resource node in the baseline.
     * @param value the Value to check for a resource reference
     * @param sourceNode the JCR Node where source is stored in the baseline
     * @param configRootNode the JCR node destination for the config Sources and resources
     */
    protected void storeBinaryResourceIfNecessary(Value value, Node sourceNode, Node configRootNode)
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
        Binary bin = configurationServiceSession.getValueFactory().createBinary(bis);
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
     * @throws Exception
     */
    public ConfigurationModelImpl loadBaseline() throws RepositoryException, ParserException, IOException {
        ConfigurationModelImpl result;

        final Node hcmRootNode = configurationServiceSession.getNode(HCM_ROOT_PATH);
        // if the baseline node doesn't exist yet...
        if (!hcmRootNode.hasNode(HCM_BASELINE)) {
            // ... there's nothing to load
            result = null;
        }
        else {
            configurationLockManager.lock();
            try {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                // otherwise, if the baseline node DOES exist...
                final Node baselineNode = hcmRootNode.getNode(HCM_BASELINE);
                final ConfigurationModelImpl model = new ConfigurationModelImpl();
                final List<GroupImpl> groups = new ArrayList<>();

                // First phase: load and parse module descriptors
                parseDescriptors(baselineNode, groups);

                // Second phase: load and parse config Sources, load and mockup content Sources
                parseSources(groups);

                // build the final merged model
                groups.forEach(model::addGroup);
                result = model.build();
                stopWatch.stop();
                log.info("ConfigurationModel loaded from baseline configuration in {}", stopWatch.toString());
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
     * @param groups accumulator object for configuration Groups
     * @throws RepositoryException
     * @throws ParserException
     */
    protected void parseDescriptors(final Node baselineNode, final List<GroupImpl> groups)
            throws RepositoryException, ParserException {
        // for each module node under this baseline
        for (Node moduleNode : findModuleNodes(baselineNode)) {
            Collection<GroupImpl> moduleGroups;
            ModuleImpl module;

            Node descriptorNode = moduleNode.getNode(HCM_MODULE_DESCRIPTOR);

            // if descriptor exists
            // TODO when demo project is restructured, we should assume this exists
            final String descriptor = descriptorNode.getProperty(HCM_YAML).getString();
            if (StringUtils.isNotEmpty(descriptor)) {
                // parse descriptor with ModuleDescriptorParser
                // todo switch to single-module alternate parser
                InputStream is = IOUtils.toInputStream(descriptor, StandardCharsets.UTF_8);
                moduleGroups = new AggregatedModulesDescriptorParser(DEFAULT_EXPLICIT_SEQUENCING)
                        .parse(is, moduleNode.getPath());

                // This should always produce exactly one module!
                module = moduleGroups.stream()
                        .flatMap(g -> g.getProjects().stream())
                        .flatMap(p -> p.getModules().stream())
                        .findFirst().get();

                final double sequenceNumber = moduleNode.hasProperty(HCM_MODULE_SEQUENCE)
                        ? moduleNode.getProperty(HCM_MODULE_SEQUENCE).getDouble()
                        : 0.0;
                module.setSequenceNumber(sequenceNumber);

                log.debug("Building module from descriptor: {}/{}/{}",
                        module.getProject().getGroup().getName(), module.getProject().getName(), module.getName());
            }
            else {
                // this should no longer happen, since we generate dummy descriptors when saving the baseline
                throw new ConfigurationRuntimeException("Module found in baseline with empty descriptor: " + moduleNode.getPath());
            }

            // store RIPs for later use
            if (moduleNode.hasNode(HCM_CONFIG_FOLDER)) {
                module.setConfigResourceInputProvider(new BaselineResourceInputProvider(moduleNode.getNode(HCM_CONFIG_FOLDER)));
            }
            if (moduleNode.hasNode(HCM_CONTENT_FOLDER)) {
                module.setContentResourceInputProvider(new BaselineResourceInputProvider(moduleNode.getNode(HCM_CONTENT_FOLDER)));
            }

            // accumulate all groups
            groups.addAll(moduleGroups);
        }

        log.debug("After parsing descriptors, we have {} groups and {} modules", groups.size(),
                groups.stream().flatMap(g -> g.getProjects().stream()).flatMap(p -> p.getModules().stream()).count());
    }

    /**
     * Second phase of loading a baseline: loading and parsing config Sources and reconstructing minimal content
     * Source mockups (containing only the root definition path).
     * @param groups accumulator object from first phase
     * @throws RepositoryException
     * @throws IOException
     * @throws ParserException
     */
    protected void parseSources(final List<GroupImpl> groups) throws RepositoryException, IOException, ParserException {
        // for each group
        for (GroupImpl group : groups) {
            // for each project
            for (ProjectImpl project : group.getProjects()) {
                // for each module
                for (ModuleImpl module : project.getModules()) {
                    log.debug("Parsing sources from baseline for {}/{}/{}",
                            group.getName(), project.getName(), module.getName());

                    BaselineResourceInputProvider rip = (BaselineResourceInputProvider) module.getConfigResourceInputProvider();
                    if (rip == null) {
                        log.debug("No {} folder in {}/{}/{}", HCM_CONFIG_FOLDER,
                                group.getName(), project.getName(), module.getName());
                    }
                    else {
                        ConfigSourceParser parser = new ConfigSourceParser(rip, true, DEFAULT_EXPLICIT_SEQUENCING);
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

                            // get content path from JCR Node
                            String contentPath = contentNode.getProperty(HCM_CONTENT_PATH).getString();

                            // create Source
                            // create ContentDefinition with a single definition node and just the node path
                            module.addContentSource(sourcePath).addContentDefinition(contentPath);
                        }
                    }
                }
            }
        }
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
     * Obtain a flat set of all content paths applied in the past
     */
    Set<String> getAppliedContentPaths() throws RepositoryException {
        final Set<String> appliedContentPaths = new LinkedHashSet<>();

        if (configurationServiceSession.nodeExists(HCM_CONTENT_NODE_PATH)) {
            final Node hcmContentRoot = configurationServiceSession.getNode(HCM_CONTENT_NODE_PATH);
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
     * Add a content path to (the end of) the set of all content paths applied
     */
    void addAppliedContentPath(final String path) throws RepositoryException {
        final Set<String> appliedPaths = getAppliedContentPaths();
        if (!appliedPaths.contains(path)) {
            appliedPaths.add(path);
            final String[] newPaths = appliedPaths.toArray(new String[appliedPaths.size()]);
            getOrCreateContentNode().setProperty(HCM_CONTENT_PATHS_APPLIED, newPaths);
            configurationServiceSession.save();
        }
    }

    private Node getOrCreateContentNode() throws RepositoryException {
        final Node rootNode = configurationServiceSession.getRootNode();
        final Node hcmRootNode = rootNode.hasNode(HCM_ROOT)
                ? rootNode.getNode(HCM_ROOT)
                : rootNode.addNode(HCM_ROOT, NT_HCM_ROOT);
        return hcmRootNode.hasNode(NT_HCM_CONTENT)
                ? hcmRootNode.getNode(NT_HCM_CONTENT)
                : hcmRootNode.addNode(NT_HCM_CONTENT, NT_HCM_CONTENT);
    }

    /**
     * Update a module's stored sequence number to indicate that all content actions have been processed
     */
    void updateModuleSequenceNumber(final ModuleImpl module) throws RepositoryException {
        final Optional<Double> latestVersion = module.getActionsMap().keySet().stream().max(Double::compareTo);
        if (latestVersion.isPresent()) {
            // TODO: JCR encode this properly!
            final String moduleNodePath = HCM_BASELINE_PATH + "/" + module.getFullName();

            module.setSequenceNumber(latestVersion.get());

            configurationServiceSession.getNode(moduleNodePath).setProperty(HCM_MODULE_SEQUENCE, latestVersion.get());
            configurationServiceSession.save();
        }
    }
}
