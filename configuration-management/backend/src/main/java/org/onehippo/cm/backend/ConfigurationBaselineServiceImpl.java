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
package org.onehippo.cm.backend;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.util.Text;
import org.onehippo.cm.api.ConfigurationBaselineService;
import org.onehippo.cm.api.ConfigurationModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Group;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.engine.BaselineResourceInputProvider;
import org.onehippo.cm.engine.parser.ConfigSourceParser;
import org.onehippo.cm.engine.parser.ParserException;
import org.onehippo.cm.engine.parser.ModuleDescriptorParser;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ContentSourceImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.builder.MergedModelBuilder;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.onehippo.cm.engine.Constants.ACTIONS_NODE;
import static org.onehippo.cm.engine.Constants.ACTIONS_TYPE;
import static org.onehippo.cm.engine.Constants.ACTIONS_YAML;
import static org.onehippo.cm.engine.Constants.BASELINE_PATH;
import static org.onehippo.cm.engine.Constants.BASELINE_TYPE;
import static org.onehippo.cm.engine.Constants.BINARY_TYPE;
import static org.onehippo.cm.engine.Constants.CND_PROPERTY;
import static org.onehippo.cm.engine.Constants.CND_TYPE;
import static org.onehippo.cm.engine.Constants.CONFIG_FOLDER_TYPE;
import static org.onehippo.cm.engine.Constants.CONTENT_FOLDER_TYPE;
import static org.onehippo.cm.engine.Constants.CONTENT_PATH_PROPERTY;
import static org.onehippo.cm.engine.Constants.CONTENT_TYPE;
import static org.onehippo.cm.engine.Constants.DEFAULT_DIGEST;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;
import static org.onehippo.cm.engine.Constants.DEFINITIONS_TYPE;
import static org.onehippo.cm.engine.Constants.DIGEST_PROPERTY;
import static org.onehippo.cm.engine.Constants.GROUP_TYPE;
import static org.onehippo.cm.engine.Constants.LAST_UPDATED_PROPERTY;
import static org.onehippo.cm.engine.Constants.MANIFEST_PROPERTY;
import static org.onehippo.cm.engine.Constants.MODULE_DESCRIPTOR_NODE;
import static org.onehippo.cm.engine.Constants.MODULE_DESCRIPTOR_TYPE;
import static org.onehippo.cm.engine.Constants.MODULE_TYPE;
import static org.onehippo.cm.engine.Constants.PROJECT_TYPE;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_FOLDER;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_YAML;
import static org.onehippo.cm.engine.Constants.REPO_CONTENT_FOLDER;
import static org.onehippo.cm.engine.Constants.YAML_PROPERTY;

public class ConfigurationBaselineServiceImpl implements ConfigurationBaselineService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationBaselineServiceImpl.class);

    private final Session session;

    public ConfigurationBaselineServiceImpl(final Session session) {
        this.session = session;
    }

    /**
     * Store a merged configuration model as a baseline configuration in the JCR.
     * The provided ConfigurationModel is assumed to be fully formed and validated.
     * @param model the configuration model to store as the new baseline
     */
    public void storeBaseline(final ConfigurationModel model) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            // TODO determine what possible race conditions exist here, perhaps during clean bootstrap

            // find baseline root node, or create if necessary
            final Node rootNode = session.getRootNode();
            boolean baselineNodeExisted = rootNode.hasNode(BASELINE_PATH);
            Node baseline = createNodeIfNecessary(rootNode, BASELINE_PATH, BASELINE_TYPE, false);

            // if the baseline node didn't exist before, save it before attempting to lock it
            if (!baselineNodeExisted) {
                session.save();
            }

            // lock baseline root
            session.getWorkspace().getLockManager()
                    .lock(baseline.getPath(), false, true, Long.MAX_VALUE, "HCM baseline");
            session.save();

            try {
                // TODO: implement a smarter partial-update process instead of brute-force removal
                // clear existing group nodes before creating new ones
                for (NodeIterator nodes = rootNode.getNode(BASELINE_PATH).getNodes(); nodes.hasNext();) {
                    Node groupNode = nodes.nextNode();
                    groupNode.remove();
                }

                // set lastupdated date to now
                baseline.setProperty(LAST_UPDATED_PROPERTY, Calendar.getInstance());

                // compute model manifest
                // Note: we've decided not to worry about processing data twice, since we don't expect large files
                //       in the config portion, and content is already optimized to use content path instead of digest
                final String modelManifest = model.compileManifest();
                log.debug("model manifest:\n"+modelManifest);
                if (log.isDebugEnabled()) {
                    baseline.setProperty(MANIFEST_PROPERTY, modelManifest);
                }

                // compute and store digest from model manifest
                String modelDigestString = computeManifestDigest(modelManifest);
                baseline.setProperty(DIGEST_PROPERTY, modelDigestString);

                // create group, project, and module nodes, if necessary
                // foreach group
                for (Group cfg : model.getSortedGroups()) {
                    Node cfgNode = createNodeIfNecessary(baseline, cfg.getName(), GROUP_TYPE, true);

                    // foreach project
                    for (Project project : cfg.getProjects()) {
                        Node projectNode = createNodeIfNecessary(cfgNode, project.getName(), PROJECT_TYPE, true);

                        // foreach module
                        for (Module module : project.getModules()) {
                            Node moduleNode = createNodeIfNecessary(projectNode, module.getName(), MODULE_TYPE, true);

                            // process each module in detail
                            storeBaselineModule(module, moduleNode, model);
                        }
                    }
                }

                // Save the session within the try block, unlock in a finally
                session.save();
            }
            finally {
                // unlock baseline root
                session.refresh(false);
                session.getWorkspace().getLockManager().unlock(baseline.getPath());
                session.save();

                stopWatch.stop();
                log.info("ConfigurationModel stored as baseline configuration in {}", stopWatch.toString());
            }
        }
        catch (Exception e) {
            log.error("Failed to store baseline configuration", e);
            throw e;
        }
    }

    /**
     * Store a single Module into the configuration baseline. This method assumes the locking and session context
     * managed in storeBaseline().
     * @param module the module to store
     * @param moduleNode the JCR node destination for the module
     * @param model the full ConfigurationModel of which the Module is a part
     * @see #storeBaseline(ConfigurationModel)
     */
    protected void storeBaselineModule(Module module, Node moduleNode, ConfigurationModel model) throws RepositoryException, IOException {

        // get the resource input provider, which provides access to raw data for module content
        ResourceInputProvider rip = model.getResourceInputProviders().get(module);
        if (rip == null) {
            log.warn("Cannot find ResourceInputProvider for module {}", module.getName());
        }

        // create descriptor node, if necessary
        Node descriptorNode = createNodeIfNecessary(moduleNode, MODULE_DESCRIPTOR_NODE, MODULE_DESCRIPTOR_TYPE, false);

        // AFAIK, a module MUST have a descriptor, but check here for a malformed package or special case
        // TODO the "/../" is an ugly hack because RIP actually treats absolute paths as relative to config base, not module base
        if (rip.hasResource(null, "/../"+REPO_CONFIG_YAML)) {
            // open descriptor InputStream
            InputStream is = rip.getResourceInputStream(null, "/../"+REPO_CONFIG_YAML);

            // store yaml and digest (this call will close the input stream)
            storeStringAndDigest(is, descriptorNode, YAML_PROPERTY);
        }
        else {
            // if descriptor doesn't exist,
            // TODO: throw an appropriate exception if this is to be forbidden, once demo config is reorganized
            String dummyDescriptor = module.compileDummyDescriptor();

            // write that back to the YAML property and digest it
            storeStringAndDigest(IOUtils.toInputStream(dummyDescriptor, StandardCharsets.UTF_8), descriptorNode, YAML_PROPERTY);
        }

        // if this Module has an actions file...
        // TODO the "/../" is an ugly hack because RIP actually treats absolute paths as relative to config base, not module base
        if (rip.hasResource(null, "/../"+ACTIONS_YAML)) {
            // create actions node, if necessary
            Node actionsNode = createNodeIfNecessary(moduleNode, ACTIONS_NODE, ACTIONS_TYPE, false);

            // open actions InputStream
            InputStream is = rip.getResourceInputStream(null, "/../"+ACTIONS_YAML);

            // store yaml and digest (this call will close the input stream)
            storeStringAndDigest(is, actionsNode, YAML_PROPERTY);
        }

        // foreach content source
        for (Source source : module.getContentSources()) {
            // TODO this is an ugly hack because source.getPath() is actually relative to content root, not module root
            // create the content root node, if necessary
            Node contentRootNode = createNodeIfNecessary(moduleNode, REPO_CONTENT_FOLDER, CONTENT_FOLDER_TYPE, false);

            // create folder nodes, if necessary
            Node sourceNode = createNodeAndParentsIfNecessary(source.getPath(), contentRootNode,
                    CONTENT_FOLDER_TYPE, CONTENT_TYPE);

            // assume that there is exactly one content definition here, as required
            ContentDefinition firstDef = (ContentDefinition) source.getDefinitions().get(0);

            // set content path property
            sourceNode.setProperty(CONTENT_PATH_PROPERTY, firstDef.getNode().getPath());
        }

        // foreach config source
        for (Source source : module.getConfigSources()) {
            // TODO this is an ugly hack because source.getPath() is actually relative to config root, not module root
            // create the config root node, if necessary
            Node configRootNode = createNodeIfNecessary(moduleNode, REPO_CONFIG_FOLDER, CONFIG_FOLDER_TYPE, false);

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
                CONFIG_FOLDER_TYPE, DEFINITIONS_TYPE);

        // open source yaml InputStream
        InputStream is = rip.getResourceInputStream(null, "/"+sourcePath);

        // store yaml and digest (this call will close the input stream)
        storeStringAndDigest(is, sourceNode, YAML_PROPERTY);

        // foreach definition
        for (Definition def : source.getDefinitions()) {
            switch (def.getType()) {
                case NAMESPACE:
                case WEBFILEBUNDLE:
                    // no special processing required
                    break;
                case CONTENT:
                    // this shouldn't exist here anymore, but we'll let the verifier handle it
                    break;
                case CND:
                    NodeTypeDefinition ntd = (NodeTypeDefinition) def;

                    // if this is not a resource reference, the YAML source is all we need here
                    // if this IS a resource reference, we want to handle this resource differently
                    if (ntd.isResource()) {
                        String cndPath = ntd.getValue();

                        // create folder nodes, if necessary
                        Node cndNode = createNodeAndParentsIfNecessary(cndPath, baseForPath(cndPath, sourceNode, configRootNode),
                                CONFIG_FOLDER_TYPE, CND_TYPE);

                        // open cnd resource InputStream
                        InputStream cndIS = rip.getResourceInputStream(source, cndPath);

                        // store cnd and digest (this call will close the input stream)
                        storeStringAndDigest(cndIS, cndNode, CND_PROPERTY);
                    }
                    break;
                case CONFIG:
                    ConfigDefinition configDef = (ConfigDefinition) def;

                    // recursively find all resources, make nodes, store binary and digest
                    storeResourcesForNode(configDef.getNode(), source, sourceNode, configRootNode, rip);
                    break;
            }
        }
    }

    /**
     * Recursively find resource references in properties of a DefinitionNode and its children nodes and store the
     * referenced resource content in the configuration baseline.
     * @param defNode a DefinitionNode to search for resource references
     * @param source the Source to which the DefinitionNode belongs
     * @param sourceNode the JCR Node where source is stored in the baseline
     * @param configRootNode the JCR node destination for the config Sources and resources
     * @param rip provides access to raw data streams
     */
    protected void storeResourcesForNode(DefinitionNode defNode, Source source, Node sourceNode, Node configRootNode, ResourceInputProvider rip)
            throws RepositoryException, IOException {

        // find resource values
        for (DefinitionProperty dp : defNode.getProperties().values()) {
            switch (dp.getType()) {
                case SINGLE:
                    storeBinaryResourceIfNecessary(dp.getValue(), source, sourceNode, configRootNode, rip);
                    break;
                case SET: case LIST:
                    for (Value value : dp.getValues()) {
                        storeBinaryResourceIfNecessary(value, source, sourceNode, configRootNode, rip);
                    }
                    break;
            }
        }

        // recursively visit child definition nodes
        for (DefinitionNode dn : defNode.getNodes().values()) {
            storeResourcesForNode(dn, source, sourceNode, configRootNode, rip);
        }
    }

    /**
     * For the given Value, check if it is a resource reference, and if so, store the referenced resource content
     * as an appropriate binary resource node in the baseline.
     * @param value the Value to check for a resource reference
     * @param source the Source to which this Value belongs
     * @param sourceNode the JCR Node where source is stored in the baseline
     * @param configRootNode the JCR node destination for the config Sources and resources
     * @param rip provides access to raw data streams
     */
    protected void storeBinaryResourceIfNecessary(Value value, Source source, Node sourceNode, Node configRootNode, ResourceInputProvider rip)
            throws RepositoryException, IOException {
        if (value.isResource()) {
            // create nodes, if necessary
            String path = value.getString();
            Node resourceNode = createNodeAndParentsIfNecessary(path, baseForPath(path, sourceNode, configRootNode),
                    CONFIG_FOLDER_TYPE, BINARY_TYPE);

            // open cnd resource InputStream
            InputStream is = rip.getResourceInputStream(source, value.getString());

            // store binary and digest
            storeBinaryAndDigest(is, resourceNode);
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
            name = Text.escapeIllegalJcrChars(name);
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
    protected void storeBinaryAndDigest(InputStream is, Node resourceNode) throws IOException, RepositoryException {
        storeAndDigest(is, resourceNode, JcrConstants.JCR_DATA, true);
    }

    /**
     * Stores the content of the given InputStream into a String property of the given Node, then closes
     * the InputStream.
     * @param is the InputStream whose contents will be stored
     * @param resourceNode the JCR Node where the content will be stored
     * @param propName the property name where the content will be stored
     */
    protected void storeStringAndDigest(InputStream is, Node resourceNode, String propName)
            throws IOException, RepositoryException {
        storeAndDigest(is, resourceNode, propName, false);
    }

    /**
     * Helper for storeXxxAndDigest() methods.
     */
    protected void storeAndDigest(InputStream is, Node resourceNode, String propName, boolean binary)
            throws IOException, RepositoryException {
        // decorate InputStream with MessageDigest and buffer
        MessageDigest md = null;
        try {
            // use MD5 because it's fast and guaranteed to be supported, and crypto attacks are not a concern here
            md = MessageDigest.getInstance(DEFAULT_DIGEST);
            DigestInputStream dis = new DigestInputStream(is, md);

            if (binary) {
                // store content as Binary
                BufferedInputStream bis = new BufferedInputStream(dis);
                Binary bin = session.getValueFactory().createBinary(bis);
                resourceNode.setProperty(propName, bin);
            }
            else {
                // use try-with-resource to close the reader and therefore the input stream
                try (Reader isr = new InputStreamReader(dis, StandardCharsets.UTF_8)) {
                    // store content as String
                    String txt = IOUtils.toString(isr);
                    resourceNode.setProperty(propName, txt);
                }
            }

            // set digest property
            if (log.isDebugEnabled()) {
                setDigestProperty(md, resourceNode);
            }
        }
        catch (NoSuchAlgorithmException e) {
            // NOTE: this should never happen, since the Java spec requires MD5 to be supported
            log.error("{} algorithm not available for configuration baseline storage", DEFAULT_DIGEST, e);
        }
    }

    /**
     * Given a fully-updated MessageDigest, compute and write the appropriate digest property to resourceNode.
     * @param md the MessageDigest instance with fully updated content, ready to digest
     * @param resourceNode the JCR Node where the digest property will be written
     */
    private void setDigestProperty(final MessageDigest md, final Node resourceNode) throws RepositoryException {
        // compute MD5 from stream
        byte[] digest = md.digest();

        // store digest property
        resourceNode.setProperty(DIGEST_PROPERTY, ModuleImpl.toDigestHexString(digest));
    }

    /**
     * Load a (partial) ConfigurationModel from the stored configuration baseline in the JCR. This model will not contain
     * content definitions, which are not stored in the baseline.
     * @throws Exception
     */
    public ConfigurationModel loadBaseline() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final Node rootNode = session.getRootNode();
        ConfigurationModel result;

        // if the baseline node doesn't exist yet...
        if (!rootNode.hasNode(BASELINE_PATH)) {
            // ... there's nothing to load
            result = null;
        }
        else {
            // otherwise, if the baseline node DOES exist...
            final Node baselineNode = rootNode.getNode(BASELINE_PATH);
            final MergedModelBuilder builder = new MergedModelBuilder();
            final Map<Module, ResourceInputProvider> rips = new HashMap<>();
            final List<GroupImpl> groups = new ArrayList<>();

            // First phase: load and parse module descriptors
            parseDescriptors(baselineNode, rips, groups);

            // Second phase: load and parse config Sources, load and mockup content Sources
            parseSources(rips, groups);

            // build the final merged model
            for (GroupImpl group : groups) {
                builder.push(group);
            }
            builder.pushResourceInputProviders(rips);
            result = builder.build();
        }

        stopWatch.stop();
        log.info("ConfigurationModel loaded from baseline configuration in {}", stopWatch.toString());

        return result;
    }

    /**
     * First phase of loading a baseline: loading and parsing module descriptors. Accumulates results in rips and groups.
     * @param baselineNode the base node for the entire stored configuration baseline
     * @param rips accumulator object for ResourceInputProviders
     * @param groups accumulator object for Configuration groups
     * @throws RepositoryException
     * @throws ParserException
     */
    protected void parseDescriptors(final Node baselineNode, final Map<Module, ResourceInputProvider> rips,
                                    final List<GroupImpl> groups) throws RepositoryException, ParserException {
        // for each module node under this baseline
        for (Node moduleNode : findModuleNodes(baselineNode)) {
            Map<String, GroupImpl> moduleGroups;
            Module module;

            Node descriptorNode = moduleNode.getNode(MODULE_DESCRIPTOR_NODE);

            // if descriptor exists
            // TODO when demo project is restructured, we should assume this exists
            final String descriptor = descriptorNode.getProperty(YAML_PROPERTY).getString();
            if (StringUtils.isNotEmpty(descriptor)) {
                // parse descriptor with ModuleDescriptorParser
                InputStream is = IOUtils.toInputStream(descriptor, StandardCharsets.UTF_8);
                moduleGroups = new ModuleDescriptorParser(DEFAULT_EXPLICIT_SEQUENCING)
                        .parse(is, moduleNode.getPath());

                // This should always produce exactly one module!
                module = moduleGroups.values().stream()
                        .flatMap(g -> g.getProjects().stream())
                        .flatMap(p -> p.getModules().stream())
                        .findFirst().get();

                log.debug("Building module from descriptor: {}/{}/{}",
                        module.getProject().getGroup().getName(), module.getProject().getName(), module.getName());
            }
            else {
                // otherwise, create "raw" group/project/module as necessary
                // TODO remove this when demo project is restructured
                Node projectNode = moduleNode.getParent();
                Node groupNode = projectNode.getParent();
                final String groupName = Text.unescapeIllegalJcrChars(groupNode.getName());
                GroupImpl group =
                        new GroupImpl(groupName);
                ProjectImpl project =
                        group.addProject(Text.unescapeIllegalJcrChars(projectNode.getName()));
                module =
                        project.addModule(Text.unescapeIllegalJcrChars(moduleNode.getName()));

                log.warn("Building module from nodes without dependencies! {}/{}/{}",
                        group.getName(), project.getName(), module.getName());

                moduleGroups = new HashMap<>();
                moduleGroups.put(groupName, group);
            }

            // store RIP for later use
            // TODO this implies that it should be impossible to have a module with no config sources!!!!
            // TODO in fact, we will want to allow modules with only content
            ResourceInputProvider rip = new BaselineResourceInputProvider(moduleNode.getNode(REPO_CONFIG_FOLDER));
            rips.put(module, rip);

            // accumulate all groups
            groups.addAll(moduleGroups.values());
        }

        log.debug("After parsing descriptors, we have {} groups and {} modules", groups.size(),
                groups.stream().flatMap(g -> g.getProjects().stream()).flatMap(p -> p.getModules().stream()).count());
    }

    /**
     * Second phase of loading a baseline: loading and parsing config Sources and reconstructing minimal content
     * Source mockups (containing only the root definition path).
     * @param rips accumulator object from first phase
     * @param groups accumulator object from first phase
     * @throws RepositoryException
     * @throws IOException
     * @throws ParserException
     */
    protected void parseSources(final Map<Module, ResourceInputProvider> rips, final List<GroupImpl> groups)
            throws RepositoryException, IOException, ParserException {
        // for each group
        for (Group group : groups) {
            // for each project
            for (Project project : group.getProjects()) {
                // for each module
                for (Module module : project.getModules()) {
                    log.debug("Parsing sources from baseline for {}/{}/{}",
                            group.getName(), project.getName(), module.getName());

                    BaselineResourceInputProvider rip = (BaselineResourceInputProvider) rips.get(module);
                    ConfigSourceParser parser = new ConfigSourceParser(rip, true, DEFAULT_EXPLICIT_SEQUENCING);
                    Node moduleNode = rip.getBaseNode();

                    // for each config source
                    final List<Node> configSourceNodes = rip.getConfigSourceNodes();
                    log.debug("Found {} config sources in {}/{}/{}", configSourceNodes.size(),
                            group.getName(), project.getName(), module.getName());

                    for (Node configNode : configSourceNodes) {
                        // compute config-root-relative path
                        String sourcePath = StringUtils.removeStart(configNode.getPath(), moduleNode.getPath()+"/");

                        // unescape JCR-illegal chars here, since resource paths are intended to be filesystem style paths
                        sourcePath = Text.unescapeIllegalJcrChars(sourcePath);

                        log.debug("Loading config from {} in {}/{}/{}", sourcePath,
                                group.getName(), project.getName(), module.getName());

                        // get InputStream
                        InputStream is = rip.getResourceInputStream(null, "/"+sourcePath);

                        // parse config source
                        parser.parse(is, sourcePath, configNode.getPath(),
                                // TODO why is this cast necessary?
                                (ModuleImpl) module);
                    }

                    // for each content source
                    final List<Node> contentSourceNodes = rip.getContentSourceNodes();
                    log.debug("Found {} content sources in {}/{}/{}", contentSourceNodes.size(),
                            group.getName(), project.getName(), module.getName());

                    for (Node contentNode : contentSourceNodes) {
                        // compute config-root-relative path
                        String sourcePath = StringUtils.removeStart(contentNode.getPath(), moduleNode.getPath()+"/");

                        // unescape JCR-illegal chars here, since resource paths are intended to be filesystem style paths
                        sourcePath = Text.unescapeIllegalJcrChars(sourcePath);

                        log.debug("Building content def from {} in {}/{}/{}", sourcePath,
                                group.getName(), project.getName(), module.getName());

                        // create Source
                        ContentSourceImpl source = new ContentSourceImpl(sourcePath, (ModuleImpl) module);

                        // get content path from JCR Node
                        String contentPath = contentNode.getProperty(CONTENT_PATH_PROPERTY).getString();

                        // create ContentDefinition with a single definition node and just the node path
                        ContentDefinitionImpl cd = source.addContentDefinition();
                        final String name = StringUtils.substringAfterLast(contentPath, "/");
                        DefinitionNodeImpl defNode = new DefinitionNodeImpl(contentPath, name, cd);
                        cd.setNode(defNode);
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

            if (possibleGroup.getPrimaryNodeType().isNodeType(GROUP_TYPE)) {
                // for each project node
                for (NodeIterator pni = possibleGroup.getNodes(); pni.hasNext(); ) {
                    Node possibleProject = pni.nextNode();
                    if (possibleProject.getPrimaryNodeType().isNodeType(PROJECT_TYPE)) {
                        // for each module node
                        for (NodeIterator mni = possibleProject.getNodes(); mni.hasNext(); ) {
                            Node possibleModule = mni.nextNode();
                            if (possibleModule.getPrimaryNodeType().isNodeType(MODULE_TYPE)) {
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
     * Compare a ConfigurationModel against the baseline by comparing manifests produced by model.compileManifest()
     */
    public boolean matchesBaselineManifest(ConfigurationModel model) throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final Node rootNode = session.getRootNode();
        boolean result;

        // if the baseline node doesn't exist yet...
        if (!rootNode.hasNode(BASELINE_PATH)) {
            // ... there's a trivial mismatch, regardless of the model
            result = false;
        }
        else {
            // otherwise, if the baseline node DOES exist...
            // ... load the digest directly from the baseline JCR node
            String baselineDigestString = rootNode.getNode(BASELINE_PATH).getProperty(DIGEST_PROPERTY).getString();
            log.debug("baseline digest:\n"+baselineDigestString);

            // compute a digest from the model manifest
            String modelManifest = model.compileManifest();
            log.debug("model manifest:\n"+modelManifest);
            String modelDigestString = computeManifestDigest(modelManifest);

            // compare the baseline digest with the model manifest digest
            result = modelDigestString.equals(baselineDigestString);
        }

        stopWatch.stop();
        log.info("ConfigurationModel compared against baseline configuration in {}", stopWatch.toString());

        return result;
    }

    /**
     * Helper method to compute a digest string from a ConfigurationModel manifest.
     * @param modelManifest the manifest whose digest we want to compute
     * @return a digest string comparable to the baseline digest string, or "" if none can be computed
     */
    private String computeManifestDigest(final String modelManifest) {
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_DIGEST);
            byte[] digest = md.digest(StandardCharsets.UTF_8.encode(modelManifest).array());
            String modelDigestString = ModuleImpl.toDigestHexString(digest);
            log.debug("model digest:\n"+modelDigestString);

            return modelDigestString;
        }
        catch (NoSuchAlgorithmException e) {
            // NOTE: this should never happen, since the Java spec requires MD5 to be supported
            log.error("{} algorithm not available for configuration baseline diff", DEFAULT_DIGEST, e);
            return "";
        }
    }
}
