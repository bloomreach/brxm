/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.backend;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;
import java.util.EnumSet;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.onehippo.cm.api.ConfigurationService;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.ACTIONS_PROPERTY;
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
import static org.onehippo.cm.engine.Constants.DEFINITIONS_TYPE;
import static org.onehippo.cm.engine.Constants.MANIFEST_PROPERTY;
import static org.onehippo.cm.engine.Constants.MANIFEST_TYPE;
import static org.onehippo.cm.engine.Constants.DIGEST_PROPERTY;
import static org.onehippo.cm.engine.Constants.GROUP_TYPE;
import static org.onehippo.cm.engine.Constants.LAST_UPDATED_PROPERTY;
import static org.onehippo.cm.engine.Constants.MODULE_TYPE;
import static org.onehippo.cm.engine.Constants.PROJECT_TYPE;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_YAML;
import static org.onehippo.cm.engine.Constants.YAML_PROPERTY;

public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private final Session session;

    public ConfigurationServiceImpl(final Session session) {
        this.session = session;
    }

    @Override
    public void apply(final MergedModel mergedModel, final EnumSet<DefinitionType> includeDefinitionTypes)
            throws Exception {
        try {
            final ConfigurationPersistenceService service =
                    new ConfigurationPersistenceService(session, mergedModel.getResourceInputProviders());
            service.apply(mergedModel, includeDefinitionTypes);
            session.save();
        }
        catch (Exception e) {
            log.warn("Failed to apply configuration", e);
            throw e;
        }
    }

    /**
     * Store a merged configuration model as a baseline configuration in the JCR.
     * The provided MergedModel is assumed to be fully formed and validated.
     * @param mergedModel the configuration model to store as the new baseline
     */
    @Override
    public void storeBaseline(final MergedModel model) throws Exception {
        try {
            // find baseline root node, or create if necessary
            final Node rootNode = session.getRootNode();
            Node baseline = createNodeIfNecessary(rootNode, BASELINE_PATH, BASELINE_TYPE, false);

            // save the baseline before attempting to lock it
            session.save();

            // lock baseline root
            session.getWorkspace().getLockManager()
                    .lock(baseline.getPath(), true, true, Long.MAX_VALUE, "HCM baseline");
            session.save();

            try {
                // set lastupdated date to now
                baseline.setProperty(LAST_UPDATED_PROPERTY, Calendar.getInstance());

                // create group, project, and module nodes, if necessary
                // foreach group
                for (Configuration cfg : model.getSortedConfigurations()) {
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
     * @param model the full MergedModel of which the Module is a part
     * @see #storeBaseline(MergedModel)
     */
    protected void storeBaselineModule(Module module, Node moduleNode, MergedModel model) throws RepositoryException, IOException {

        // get the resource input provider, which provides access to raw data for module content
        ResourceInputProvider rip = model.getResourceInputProviders().get(module);
        if (rip == null) {
            log.warn("Cannot find ResourceInputProvider for module {}", module.getName());
        }

        // create manifest node, if necessary
        Node depNode = createNodeIfNecessary(moduleNode, MANIFEST_PROPERTY, MANIFEST_TYPE, false);

        // AFAIK, a module MUST have a manifest, but check here for a malformed package or special case
        if (rip.hasResource(null, "/"+REPO_CONFIG_YAML)) {
            // open manifest InputStream
            InputStream is = rip.getResourceInputStream(null, "/"+REPO_CONFIG_YAML);

            // store yaml and digest (this call will close the input stream)
            storeStringAndDigest(is, depNode, YAML_PROPERTY);
        }
        else {
            // if manifest doesn't exist, set the manifest property and digest to empty strings
            // TODO: throw an appropriate exception if this is to be forbidden, once demo config is reorganized
            depNode.setProperty(YAML_PROPERTY, "");
            depNode.setProperty(DIGEST_PROPERTY, "");
        }

        // if this Module has an actions file...
        if (rip.hasResource(null, "/"+ACTIONS_YAML)) {
            // create actions node, if necessary
            Node actionsNode = createNodeIfNecessary(moduleNode, ACTIONS_PROPERTY, ACTIONS_TYPE, false);

            // open actions InputStream
            InputStream is = rip.getResourceInputStream(null, "/"+ACTIONS_YAML);

            // store yaml and digest (this call will close the input stream)
            storeStringAndDigest(is, actionsNode, YAML_PROPERTY);
        }

        // foreach content source
        for (Source source : module.getContentSources()) {
            // create folder nodes, if necessary
            Node sourceNode = createNodeAndParentsIfNecessary(source.getPath(), moduleNode,
                    CONTENT_FOLDER_TYPE, CONTENT_TYPE);

            // assume that there is exactly one content definition here, as required
            ContentDefinition firstDef = (ContentDefinition) source.getDefinitions().get(0);

            // set content path property
            sourceNode.setProperty(CONTENT_PATH_PROPERTY, firstDef.getNode().getPath());
        }

        // foreach config source
        for (Source source : module.getConfigSources()) {
            // process in detail ...
            storeBaselineConfigSource(source, moduleNode, rip);
        }
    }

    /**
     * Store a single config definition Source into the baseline. This method assumes the locking and session context
     * managed in storeBaseline().
     * @param source the Source to store
     * @param moduleNode the JCR node destination for the module as a whole
     * @param rip provides access to raw data streams
     * @see #storeBaseline(MergedModel)
     */
    protected void storeBaselineConfigSource(final Source source, final Node moduleNode, final ResourceInputProvider rip)
            throws RepositoryException, IOException {
        // create folder nodes, if necessary
        String sourcePath = source.getPath();
        Node sourceNode = createNodeAndParentsIfNecessary(sourcePath, moduleNode,
                CONFIG_FOLDER_TYPE, DEFINITIONS_TYPE);

        // open source yaml InputStream
        InputStream is = rip.getResourceInputStream(null, "/"+sourcePath);

        // store yaml and digest (this call will close the input stream)
        storeStringAndDigest(is, sourceNode, YAML_PROPERTY);

        // foreach definition
        for (Definition def : source.getDefinitions()) {
            switch (def.getType()) {
                case NAMESPACE: case WEBFILEBUNDLE:
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
                        Node cndNode = createNodeAndParentsIfNecessary(cndPath, baseForPath(cndPath, sourceNode, moduleNode),
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
                    storeResourcesForNode(configDef.getNode(), source, sourceNode, moduleNode, rip);
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
     * @param rip provides access to raw data streams
     */
    protected void storeResourcesForNode(DefinitionNode defNode, Source source, Node sourceNode, Node moduleNode, ResourceInputProvider rip)
            throws RepositoryException, IOException {

        // find resource values
        for (DefinitionProperty dp : defNode.getProperties().values()) {
            switch (dp.getType()) {
                case SINGLE:
                    storeBinaryResourceIfNecessary(dp.getValue(), source, sourceNode, moduleNode, rip);
                    break;
                case SET: case LIST:
                    for (Value value : dp.getValues()) {
                        storeBinaryResourceIfNecessary(value, source, sourceNode, moduleNode, rip);
                    }
                    break;
            }
        }

        // recursively visit child definition nodes
        for (DefinitionNode dn : defNode.getNodes().values()) {
            storeResourcesForNode(dn, source, sourceNode, moduleNode, rip);
        }
    }

    /**
     * For the given Value, check if it is a resource reference, and if so, store the referenced resource content
     * as an appropriate binary resource node in the baseline.
     * @param value the Value to check for a resource reference
     * @param source the Source to which this Value belongs
     * @param sourceNode the JCR Node where source is stored in the baseline
     * @param rip provides access to raw data streams
     */
    protected void storeBinaryResourceIfNecessary(Value value, Source source, Node sourceNode, Node moduleNode, ResourceInputProvider rip)
            throws RepositoryException, IOException {
        if (value.isResource()) {
            // create nodes, if necessary
            String path = value.getString();
            Node resourceNode = createNodeAndParentsIfNecessary(path, baseForPath(path, sourceNode, moduleNode),
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
     * @param moduleNode base node for module
     * @return either moduleNode iff path has a leading /, else sourceNode.getParent()
     */
    private Node baseForPath(String path, Node sourceNode, Node moduleNode) throws RepositoryException {
        if (path.startsWith("/")) {
            return moduleNode;
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
    private void storeAndDigest(final InputStream is, final Node resourceNode, String propName, boolean binary)
            throws RepositoryException, IOException {
        // decorate InputStream with MessageDigest and buffer
        MessageDigest md = null;
        try {
            // use MD5 because it's fast and guaranteed to be supported, and crypto attacks are not a concern here
            md = MessageDigest.getInstance(DEFAULT_DIGEST);
            DigestInputStream dis = new DigestInputStream(is, md);

            if (binary) {
                setBinaryProperty(dis, resourceNode);
            }
            else {
                setStringProperty(dis, resourceNode, propName);
            }

            // set digest property
            setDigestProperty(md, resourceNode);
        }
        catch (NoSuchAlgorithmException e) {
            // NOTE: this should never happen, since the Java spec requires MD5 to be supported
            log.error("{} algorithm not available for configuration baseline storage", DEFAULT_DIGEST, e);
        }
    }

    /**
     * Helper for storeAndDigest(); handles binary resources.
     */
    private void setBinaryProperty(final DigestInputStream dis, final Node resourceNode) throws RepositoryException {
        // store content as Binary
        BufferedInputStream bis = new BufferedInputStream(dis);
        Binary bin = session.getValueFactory().createBinary(bis);
        resourceNode.setProperty(JcrConstants.JCR_DATA, bin);
    }

    /**
     * Helper for storeAndDigest(); handles text resources.
     */
    private void setStringProperty(final DigestInputStream dis, final Node resourceNode, final String propName)
            throws IOException, RepositoryException {
        // use try-with-resource to close the reader and therefore the input stream
        try (Reader isr = new InputStreamReader(dis, StandardCharsets.UTF_8)) {
            // store content as String
            String txt = IOUtils.toString(isr);
            resourceNode.setProperty(propName, txt);
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

        // prepend algorithm using same style as used in Hippo CMS password hashing
        String digestString = "$"+DEFAULT_DIGEST+"$"+ Base64.getEncoder().encodeToString(digest);

        // store digest property
        resourceNode.setProperty(DIGEST_PROPERTY, digestString);
    }
}
