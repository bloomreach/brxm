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
package org.hippoecm.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.jackrabbit.HippoNodeTypeRegistry;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_MODULE_CONFIG;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MODULECONFIG;
import static org.hippoecm.repository.api.HippoNodeType.NT_AUTHROLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAIN;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAINRULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETRULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_MODULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_ROLE;

class MigrateToV13 {

    static final Logger log = LoggerFactory.getLogger(MigrateToV13.class);

    private static final String DEPRECATED_NT_HIPPOSYS_AUTOEXPORT = "hipposys:autoexport";
    private static final String HIPPO_NAMESPACES = "/hippo:namespaces";

    // properties
    private static final String HTMLCLEANER_ID = "htmlcleaner.id";
    private static final String HTMLPROCESSOR_ID = "htmlprocessor.id";
    private static final String PLUGIN_CLASS = "plugin.class";
    private static final String CLUSTER_OPTIONS = "cluster.options";

    private static final String HIPPO_MODULES_PATH = "/hippo:configuration/hippo:modules";
    private static final String HTML_PROCESSOR = "htmlprocessor";
    private static final String HTML_PROCESSOR_SERVICE_MODULE = "org.onehippo.cms7.services.htmlprocessor.HtmlProcessorServiceModule";

    // builtin htmlprocessor's
    private static final String NO_FILTER_PROCESSOR = "no-filter";
    private static final String FORMATTED_PROCESSOR = "formatted";
    private static final String RICHTEXT_PROCESSOR = "richtext";

    private static final Map<String, String[]> FORMATTED_HTML_PROCESSOR_WHITELIST = new HashMap<>();
    static {
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("address", new String[] {"class", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("br", new String[] {"class", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("em", new String[] {"class", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("h1", new String[] {"class", "dir", "id", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("h2", new String[] {"class", "dir", "id", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("h3", new String[] {"class", "dir", "id", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("h4", new String[] {"class", "dir", "id", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("h5", new String[] {"class", "dir", "id", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("h6", new String[] {"class", "dir", "id", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("p", new String[] {"align", "class", "dir", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("pre", new String[] {"class", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("strong", new String[] {"class", "style"});
        FORMATTED_HTML_PROCESSOR_WHITELIST.put("u", new String[] {"class", "style"});
    }

    private final Session session;
    private final HippoNodeTypeRegistry ntr;
    private final boolean dryRun;
    private final NodeTypeManager ntm;
    private final QueryManager qm;

    public MigrateToV13(final Session rootSession, final HippoNodeTypeRegistry ntr, final boolean dryRun)
            throws RepositoryException {
        this.session = rootSession;
        this.ntr = ntr;
        this.dryRun = dryRun;
        this.ntm = rootSession.getWorkspace().getNodeTypeManager();
        this.qm = rootSession.getWorkspace().getQueryManager();
    }

    public void migrateIfNeeded() throws RepositoryException {
        if (!ntm.hasNodeType(DEPRECATED_NT_HIPPOSYS_AUTOEXPORT)) {
            log.debug("No migration needed");
            return;
        }

        checkDeprecatedTypeNotInUse(DEPRECATED_NT_HIPPOSYS_AUTOEXPORT);
        migrateDomains();
        migrateModuleConfig();
        migrateUrlRewriter();
        migrateHtmlProcessor();
        migrateHtmlProcessorUsage();

        if (!dryRun) {
            ntr.ignoreNextCheckReferencesInContent();
            ntm.unregisterNodeType(DEPRECATED_NT_HIPPOSYS_AUTOEXPORT);
            log.info("Migrated");
        } else {
            log.info("MigrateToV12 dry-run completed.");
        }
    }

    private void migrateHtmlProcessor() throws RepositoryException {
        final Node moduleConfigNode = getHtmlProcessorModuleConfigNode();
        migrateNoFilterHtmlProcessor(moduleConfigNode);
        migrateFormattedHtmlProcessor(moduleConfigNode);
        migrateRichTextHtmlProcessor(moduleConfigNode);
    }

    private Node getHtmlProcessorModuleConfigNode() throws RepositoryException {
        boolean saveNeeded = false;

        final Node modulesNode = session.getNode(HIPPO_MODULES_PATH);
        final String htmlProcessor = HTML_PROCESSOR;

        final Node htmlProcessorNode;
        if (modulesNode.hasNode(htmlProcessor)) {
            htmlProcessorNode = modulesNode.getNode(htmlProcessor);
        } else {
            htmlProcessorNode = modulesNode.addNode(htmlProcessor, NT_MODULE);
            saveNeeded = true;
        }

        if (!htmlProcessorNode.hasProperty("hipposys:className")) {
            htmlProcessorNode.setProperty("hipposys:className", HTML_PROCESSOR_SERVICE_MODULE);
            saveNeeded = true;
        }

        final Node moduleConfigNode;
        if (htmlProcessorNode.hasNode(HIPPO_MODULECONFIG)) {
            moduleConfigNode = htmlProcessorNode.getNode(HIPPO_MODULECONFIG);
        } else {
            moduleConfigNode = htmlProcessorNode.addNode(HIPPO_MODULECONFIG, HIPPOSYS_MODULE_CONFIG);
            saveNeeded = true;
        }

        if (!dryRun && saveNeeded) {
            session.save();
        }

        return moduleConfigNode;
    }

    private void migrateNoFilterHtmlProcessor(final Node moduleConfigNode) throws RepositoryException {
        if (!moduleConfigNode.hasNode(NO_FILTER_PROCESSOR)) {
            log.info("Creating default no-filter HtmlProcessor");

            final Node noFilter = moduleConfigNode.addNode(NO_FILTER_PROCESSOR, HIPPOSYS_MODULE_CONFIG);
            noFilter.setProperty("charset", "UTF-8");
            noFilter.setProperty("filter", false);
            noFilter.setProperty("omitComments", false);

            if (!dryRun) {
                session.save();
            }
        }
    }

    private void migrateFormattedHtmlProcessor(final Node moduleConfigNode) throws RepositoryException {
        if (!moduleConfigNode.hasNode(FORMATTED_PROCESSOR)) {
            log.info("Creating default formatted HtmlProcessor");

            final Node formatted = moduleConfigNode.addNode(FORMATTED_PROCESSOR, HIPPOSYS_MODULE_CONFIG);
            formatted.setProperty("charset", "UTF-8");
            formatted.setProperty("filter", true);
            formatted.setProperty("omitComments", false);

            for (final Map.Entry<String, String[]> entry : FORMATTED_HTML_PROCESSOR_WHITELIST.entrySet()) {
                final Node whitelistNode = formatted.addNode(entry.getKey(), HIPPOSYS_MODULE_CONFIG);
                whitelistNode.setProperty("attributes", entry.getValue());
            }

            if (!dryRun) {
                session.save();
            }
        }
    }

    private void migrateRichTextHtmlProcessor(final Node htmlProcessorModuleConfigNode) throws RepositoryException {
        final String sourceNodePath = "/hippo:configuration/hippo:frontend/cms/cms-services/filteringHtmlCleanerService";
        final boolean sourceNodeExists = session.nodeExists(sourceNodePath);
        if (sourceNodeExists) {
            log.info("Migrating filteringHtmlCleanerService to richtext HtmlProcessor");

            final boolean saveNeeded = migrateHtmlCleanerConfiguration(sourceNodePath, htmlProcessorModuleConfigNode);
            if (!dryRun && saveNeeded) {
                session.save();
            }
        } else {
            log.info("Source node {} does not exist, skipping migrating richtext html cleaner", sourceNodePath);
        }
    }

    private boolean migrateHtmlCleanerConfiguration(final String sourceNodePath, final Node moduleConfig)
            throws RepositoryException {

        boolean saveNeeded = false;

        final Node sourceNode = session.getNode(sourceNodePath);
        final Node destinationNode;
        if (moduleConfig.hasNode(RICHTEXT_PROCESSOR)) {
            destinationNode = moduleConfig.getNode(RICHTEXT_PROCESSOR);
        } else {
            destinationNode = moduleConfig.addNode(RICHTEXT_PROCESSOR, HIPPOSYS_MODULE_CONFIG);
            saveNeeded = true;
        }

        final Collection<String> copiedProperties = getCopiedProperties();

        final PropertyIterator propertyIterator = sourceNode.getProperties();
        while (propertyIterator.hasNext()) {
            final Property property = propertyIterator.nextProperty();

            if (copiedProperties.contains(property.getName())) {
                log.info("Migrating property '{}'", property.getName());
                if (property.getName().equals("omitComments")) {
                    destinationNode.setProperty(property.getName(), Boolean.valueOf(property.getString()));
                } else {
                    movePropertyToNode(property, destinationNode);
                }
                saveNeeded = true;
            }
        }

        final NodeIterator nodeIterator = sourceNode.getNode("whitelist").getNodes();
        while (nodeIterator.hasNext()) {
            final Node node = nodeIterator.nextNode();
            final String name = node.getName();
            log.info("Migrating whitelisted node '{}'", name);
            if (!destinationNode.hasNode(name)) {
                final Node whitelistedNode = destinationNode.addNode(name, HIPPOSYS_MODULE_CONFIG);
                final String attributes = "attributes";
                if (node.hasProperty(attributes)) {
                    whitelistedNode.setProperty(attributes, node.getProperty(attributes).getValues());
                }
                saveNeeded = true;
            }
        }
        return saveNeeded;
    }

    private void migrateHtmlProcessorUsage() throws RepositoryException {
        boolean saveNeeded = migrateSingleHtmlProcessorUsage("/hippo:namespaces/system/Html/editor:templates/_default_",
                FORMATTED_PROCESSOR);
        saveNeeded = migrateSingleHtmlProcessorUsage("/hippo:namespaces/hippostd/html/editor:templates/_default_",
                RICHTEXT_PROCESSOR) || saveNeeded;

        // migrate all custom usages
        final Node node = session.getNode(HIPPO_NAMESPACES);
        saveNeeded = migrateAllCustomHtmlProcessorUsages(node) || saveNeeded;

        if (!dryRun && saveNeeded) {
            session.save();
        }
    }

    private boolean migrateAllCustomHtmlProcessorUsages(final Node node) throws RepositoryException {
        final NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            final Node nextNode = nodeIterator.nextNode();
            if (nextNode.hasProperty(PLUGIN_CLASS) && nextNode.hasNodes()) {
                final NodeIterator nextNodeIterator = nextNode.getNodes();
                while (nextNodeIterator.hasNext()) {
                    final Node child = nextNodeIterator.nextNode();
                    if (child.getName().equalsIgnoreCase(CLUSTER_OPTIONS) && child.hasProperty(HTMLCLEANER_ID)) {
                        final Property htmlcleanerId = child.getProperty(HTMLCLEANER_ID);
                        final Property pluginClass = nextNode.getProperty(PLUGIN_CLASS);
                        final String htmlProcessorId = getHtmlProcessorId(htmlcleanerId, pluginClass);
                        updateCleanerToProcessor(child, htmlProcessorId);
                        return true;
                    }
                }
            } else {
                return migrateAllCustomHtmlProcessorUsages(nextNode);
            }
        }
        return false;
    }

    private boolean migrateSingleHtmlProcessorUsage(final String sourceNodePath, final String defaultConfigName) throws RepositoryException {
        if (session.nodeExists(sourceNodePath)) {
            final Node sourceNode = session.getNode(sourceNodePath);
            final String processorType = isHtmlCleanerNotDefined(sourceNode) ? NO_FILTER_PROCESSOR : defaultConfigName;
            updateCleanerToProcessor(sourceNode, processorType);
            return true;
        }
        return false;
    }

    private String getHtmlProcessorId(final Property htmlcleanerId, final Property pluginClass) throws RepositoryException {
        if (htmlcleanerId == null || StringUtils.isBlank(htmlcleanerId.getString())) {
            return NO_FILTER_PROCESSOR;
        } else if (pluginClass.getString().equalsIgnoreCase("org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin")) {
            return FORMATTED_PROCESSOR;
        }
        return RICHTEXT_PROCESSOR;
    }

    private static void updateCleanerToProcessor(final Node node, final String processorType) throws RepositoryException {
        if (node.hasProperty(HTMLCLEANER_ID)) {
            node.getProperty(HTMLCLEANER_ID).remove();
        }
        node.setProperty(HTMLPROCESSOR_ID, processorType);
    }

    private static boolean isHtmlCleanerNotDefined(final Node sourceNode) throws RepositoryException {
        return !sourceNode.hasProperty(HTMLCLEANER_ID) ||
                StringUtils.isBlank(sourceNode.getProperty(HTMLCLEANER_ID).getString());
    }

    private static Collection<String> getCopiedProperties() {
        final Collection<String> copiedProperties = new HashSet<>();
        copiedProperties.add("charset");
        copiedProperties.add("filter");
        copiedProperties.add("omitComments");
        copiedProperties.add("serializer");
        return copiedProperties;
    }

    void migrateUrlRewriter() throws RepositoryException {

        final String sourceNodePath = "/content/urlrewriter";
        final String destinationNodePath = "/hippo:configuration/hippo:modules/urlrewriter/hippo:moduleconfig";

        final boolean sourceNodeExists = session.nodeExists(sourceNodePath);
        final boolean destinationNodeExists = session.nodeExists(destinationNodePath);

        boolean saveNeeded = false;
        if (sourceNodeExists) {
            log.info("Migrating urlrewriter");
            final Node sourceNode = session.getNode(sourceNodePath);
            Node destinationNode = null;
            for (final PropertyIterator iterator = sourceNode.getProperties(); iterator.hasNext(); ) {
                if (destinationNode == null) {
                    destinationNode = destinationNodeExists ? session.getNode(
                            destinationNodePath) : createUrlRewriterDestinationNode();
                }
                final Property property = iterator.nextProperty();
                if (property.getName().startsWith("urlrewriter:")) {
                    log.info("Migrating property '{}'", property.getName());
                    movePropertyToNode(property, destinationNode);
                    saveNeeded = true;
                }
            }
        } else {
            log.info("Source node {} does not exist, skipping migrating url rewriter",
                    sourceNodePath);
        }

        if (!dryRun && saveNeeded) {
            session.save();
        }
    }

    private Node createUrlRewriterDestinationNode() throws RepositoryException {
        final Node modulesNode = session.getNode("/hippo:configuration/hippo:modules");
        final String urlrewriter = "urlrewriter";
        final Node urlrewriterNode = modulesNode.hasNode(urlrewriter) ?
                modulesNode.getNode(urlrewriter) :
                modulesNode.addNode(urlrewriter, NT_MODULE);

        return urlrewriterNode.hasNode(HIPPO_MODULECONFIG) ?
                urlrewriterNode.getNode(HIPPO_MODULECONFIG) : urlrewriterNode.addNode(HIPPO_MODULECONFIG,
                HIPPOSYS_MODULE_CONFIG);
    }

    private void movePropertyToNode(final Property sourceProperty, final Node destinationNode) throws RepositoryException {

        final String propertyName = sourceProperty.getName();
        if (destinationNode.hasProperty(propertyName)) {
            destinationNode.getProperty(propertyName).remove();
        }

        log.info(String.format("Setting property '%s' to destination node '%s'", propertyName,
                destinationNode.getPath()));
        if (sourceProperty.isMultiple()) {
            destinationNode.setProperty(propertyName, sourceProperty.getValues());
        } else {
            destinationNode.setProperty(propertyName, sourceProperty.getValue());
        }

        log.info(String.format("Removing property '%s' from source node", propertyName));
        sourceProperty.remove();
    }

    private void checkDeprecatedTypeNotInUse(final String nodeType) throws RepositoryException {
        final Query query = qm.createQuery("//element(*, " + nodeType + ")", Query.XPATH);
        final QueryResult queryResult = query.execute();
        if (queryResult.getNodes().hasNext()) {
            final Node node = queryResult.getNodes().nextNode();
            throw new RepositoryException(String.format(
                    "Deprecated node type %s still in use at '%s'. Remove all usage of this node type before upgrading to v12",
                    nodeType, node.getPath()));
        }
    }

    private void migrateDomains() throws RepositoryException {
        if (allowsSNS(NT_DOMAINRULE)) {
            log.info("Migrating " + NT_DOMAINRULE);
            checkOrFixSNS(NT_FACETRULE, true);
            removeAllSNSFromNTD(NT_DOMAINRULE);
        }
        if (allowsSNS(NT_DOMAIN)) {
            log.info("Migrating " + NT_DOMAIN);
            migrateDomainAuthRoles();
            checkOrFixSNS(NT_DOMAINRULE, true);
            removeAllSNSFromNTD(NT_DOMAIN);
        }
    }

    private void migrateModuleConfig() throws RepositoryException {
        if (allowsSNS(HIPPOSYS_MODULE_CONFIG)) {
            log.info("Migrating " + HIPPOSYS_MODULE_CONFIG);
            migrateEformsConfig();
            checkOrFixSNS(HIPPOSYS_MODULE_CONFIG, false);
            removeAllSNSFromNTD(HIPPOSYS_MODULE_CONFIG);
        }
    }

    private void migrateDomainAuthRoles() throws RepositoryException {
        boolean saveNeeded = false;
        log.info("Migrating " + NT_AUTHROLE);
        final Query query = qm.createQuery("//element(*, hipposys:authrole)", Query.XPATH);
        final QueryResult queryResult = query.execute();
        for (final Node node : new NodeIterable(queryResult.getNodes())) {
            final String roleName = node.getProperty(NT_ROLE).getString();
            if (!roleName.equals(node.getName())) {
                moveToUniqueName(node, roleName);
                saveNeeded = true;
            } else if (node.getIndex() > 1) {
                moveToUniqueName(node, node.getName());
                saveNeeded = true;
            }
        }
        if (!dryRun && saveNeeded) {
            session.save();
        }
    }

    private void checkOrFixSNS(final String nodeTypeName, final boolean fixSNS) throws RepositoryException {
        boolean saveNeeded = false;
        final Query query = qm.createQuery("//element(*, " + nodeTypeName + ")", Query.XPATH);
        final QueryResult queryResult = query.execute();
        for (final Node node : new NodeIterable(queryResult.getNodes())) {
            if (node.getIndex() > 1) {
                if (fixSNS) {
                    moveToUniqueName(node, node.getName());
                    saveNeeded = true;
                } else {
                    throw new RepositoryException("Encountered " + nodeTypeName + " SNS at: " + node.getPath());
                }
            }
        }
        if (!dryRun && saveNeeded) {
            session.save();
        }
    }

    private void migrateEformsConfig() throws RepositoryException {
        final String eformsConfigPath = "/hippo:configuration/hippo:modules/eforms/hippo:moduleconfig";
        if (session.nodeExists(eformsConfigPath)) {
            log.info("Migrating eforms");
            final Node node = session.getNode(eformsConfigPath);
            if (fixEformsModuleConfigPrimaryType(node) && !dryRun) {
                session.save();
            }
        }
    }

    private boolean fixEformsModuleConfigPrimaryType(final Node node) throws RepositoryException {
        boolean saveNeeded = false;
        for (final Node child : new NodeIterable(node.getNodes())) {
            if (fixEformsModuleConfigPrimaryType(child)) {
                saveNeeded = true;
            }
        }
        if (!node.isNodeType(HIPPOSYS_MODULE_CONFIG)) {
            saveNeeded = true;
            node.setPrimaryType(HIPPOSYS_MODULE_CONFIG);
            if ("eforms:validationrule".equals(node.getName())) {
                final Property ruleIdProperty = node.getProperty("eforms:validationruleid");
                final String ruleId = ruleIdProperty.getString();
                final String newPath = node.getParent().getPath() + "/" + ruleId;
                log.info("checking new path [" + newPath + "]");
                if (session.nodeExists(newPath)) {
                    throw new RepositoryException(String.format(
                            "Cannot fix eforms:validationrule SNS for node with eforms:validationruleid '%s'. Another SNS found: %s",
                            ruleId, newPath));
                }
                ruleIdProperty.remove();
                log.info("Renaming " + node.getPath() + " to " + ruleId);
                session.move(node.getPath(), newPath);
            }
            if ("eforms:daterule".equals(node.getName())) {
                final Property ruleIdProperty = node.getProperty("eforms:dateruleid");
                final String ruleId = ruleIdProperty.getString();
                final String newPath = node.getParent().getPath() + "/" + ruleId;
                if (session.nodeExists(newPath)) {
                    throw new RepositoryException(String.format(
                            "Cannot fix eforms:daterule SNS for node with eforms:dateruleid '%s'. Another SNS found: %s",
                            ruleId, newPath));
                }
                ruleIdProperty.remove();
                log.info("Renaming " + node.getPath() + " to " + ruleId);
                session.move(node.getPath(), newPath);
            }
        }
        return saveNeeded;
    }

    private boolean allowsSNS(final String nodeType) throws RepositoryException {
        final NodeType nt = ntm.getNodeType(nodeType);
        for (final NodeDefinition nd : nt.getDeclaredChildNodeDefinitions()) {
            if (nd.allowsSameNameSiblings()) {
                return true;
            }
        }
        return false;
    }

    private void removeAllSNSFromNTD(final String nodeTypeName) throws RepositoryException {
        if (!dryRun) {
            log.info("Removing SNS from " + nodeTypeName);
            final NodeTypeTemplate ntt = ntm.createNodeTypeTemplate(ntm.getNodeType(nodeTypeName));
            for (final Object nd : ntt.getNodeDefinitionTemplates()) {
                final NodeDefinitionTemplate ndt = (NodeDefinitionTemplate) nd;
                ndt.setSameNameSiblings(false);
            }
            ntr.ignoreNextConflictingContent();
            ntm.registerNodeType(ntt, true);
        }
    }

    private void moveToUniqueName(final Node node, final String nameCandidate) throws RepositoryException {
        final String parentPath = node.getParent().getPath();
        int postFix = 0;
        String newPath = parentPath + "/" + nameCandidate;
        while (session.nodeExists(newPath)) {
            postFix++;
            newPath = parentPath + "/" + nameCandidate + postFix;
        }
        log.info("Renaming " + node.getPath() + " to " + nameCandidate + (postFix == 0 ? "" : postFix));
        session.move(node.getPath(), newPath);
    }
}
