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
import java.util.HashSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
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

class MigrateToV12 {

    static final Logger log = LoggerFactory.getLogger(MigrateToV12.class);

    private static String DEPRECATED_NT_HIPPOSYS_AUTOEXPORT = "hipposys:autoexport";
    private static final String HTMLCLEANER_ID = "htmlcleaner.id";
    private static final String HTMLPROCESSOR_ID = "htmlprocessor.id";
    private static final String HIPPO_NAMESPACES = "/hippo:namespaces";
    private static final String CLUSTER_OPTIONS = "cluster.options";

    private final Session session;
    private final HippoNodeTypeRegistry ntr;
    private final boolean dryRun;
    private final NodeTypeManager ntm;
    private final QueryManager qm;

    public MigrateToV12(final Session rootSession, final HippoNodeTypeRegistry ntr, final boolean dryRun)
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
            if (!Boolean.getBoolean("repo.migrateToV12immediately")) {
                throw new RuntimeException("Migrated to V12.0.0, please restart again.");
            }
        } else {
            log.info("MigrateToV12 dry-run completed.");
        }
    }

    private void migrateHtmlProcessor() throws RepositoryException {
        final String sourceNodePath = "/hippo:configuration/hippo:frontend/cms/cms-services/filteringHtmlCleanerService";
        final String destinationNodePath = "/hippo:configuration/hippo:modules/htmlprocessor/hippo:moduleconfig/richtext";

        final boolean sourceNodeExists = session.nodeExists(sourceNodePath);
        final boolean destinationNodeExists = session.nodeExists(destinationNodePath);

        if (sourceNodeExists) {
            final boolean saveNeeded = migrateHtmlCleanerConfiguration(sourceNodePath, destinationNodePath, destinationNodeExists);
            if (!dryRun && saveNeeded) {
                session.save();
            }
        } else {
            log.info("Source node {} does not exist, skipping migrating html cleaner", sourceNodePath);
        }
    }

    private boolean migrateHtmlCleanerConfiguration(final String sourceNodePath, final String destinationNodePath, final boolean destinationNodeExists) throws RepositoryException {
        log.info("Migrating html cleaner");
        final Node sourceNode = session.getNode(sourceNodePath);
        final Node destinationNode = destinationNodeExists ? session.getNode(destinationNodePath) : createHtmlCleanerDestinationNode();
        boolean saveNeeded = false;

        final Collection<String> copiedProperties = getCopiedProperties();

        final PropertyIterator propertyIterator = sourceNode.getProperties();
        while(propertyIterator.hasNext()) {
            final Property property = propertyIterator.nextProperty();

            if (copiedProperties.contains(property.getName())) {
                log.info("Migrating property '{}'", property.getName());
                movePropertyToNode(property, destinationNode);
                saveNeeded = true;
            }
        }
        final NodeIterator nodeIterator = sourceNode.getNode("whitelist").getNodes();
        while(nodeIterator.hasNext()) {
            final Node node = nodeIterator.nextNode();
            log.info("Migrating whitelisted node '{}'", node.getName());
            final String newPath = destinationNodePath + "/" + node.getName();
            if(!session.nodeExists(newPath)) {
                session.move(node.getPath(), newPath);
                saveNeeded = true;
            }
        }
        return saveNeeded;
    }

    private void migrateHtmlProcessorUsage() throws RepositoryException {
        migrateSingleHtmlProcessorUsage("/hippo:namespaces/system/Html/editor:templates/_default_", "formatted");
        migrateSingleHtmlProcessorUsage("/hippo:namespaces/hippostd/html/editor:templates/_default_", "richtext");

        // migrate all custom usages
        final Node node = session.getNode(HIPPO_NAMESPACES);
        migrateAllCustomHtmlProcessorUsages(node);
    }

    private void migrateAllCustomHtmlProcessorUsages(final Node node) throws RepositoryException {
        final NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            final Node nextNode = nodeIterator.nextNode();
            if(nextNode.hasProperty("plugin.class") && nextNode.hasNodes()) {
                final NodeIterator nextNodeIterator = nextNode.getNodes();
                while (nextNodeIterator.hasNext()) {
                    final Node child = nextNodeIterator.nextNode();
                    if(child.getName().equalsIgnoreCase(CLUSTER_OPTIONS) && child.hasProperty(HTMLCLEANER_ID)) {
                        final Property htmlcleanerId = child.getProperty("htmlcleaner.id");
                        final Property pluginClass = nextNode.getProperty("plugin.class");
                        final String htmlProcessorId = getHtmlProcessorId(htmlcleanerId, pluginClass);
                        updateCleanerToProcessor(child, htmlProcessorId);
                    }
                }
            } else {
                migrateAllCustomHtmlProcessorUsages(nextNode);
            }
        }
    }

    private void migrateSingleHtmlProcessorUsage(final String sourceNodePath, final String defaultConfigName) throws RepositoryException {
        if (session.nodeExists(sourceNodePath)) {
            final Node sourceNode = session.getNode(sourceNodePath);
            final String processorType = isHtmlCleanerNotDefined(sourceNode) ? "no-filter" : defaultConfigName;
            updateCleanerToProcessor(sourceNode, processorType);
        }
    }

    private String getHtmlProcessorId(final Property htmlcleanerId, final Property pluginClass) throws RepositoryException {
        if (htmlcleanerId == null || StringUtils.isBlank(htmlcleanerId.getString())) {
            return "no-filter";
        } else if (pluginClass.getString().equalsIgnoreCase("org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin")) {
            return "formatted";
        }
        return "richtext";
    }

    private static void updateCleanerToProcessor(final Node node, final String processorType) throws RepositoryException {
        node.getProperty(HTMLCLEANER_ID).remove();
        node.setProperty(HTMLPROCESSOR_ID, processorType);
    }

    private static boolean isHtmlCleanerNotDefined(final Node sourceNode) throws RepositoryException {
        final Property htmlcleanerId = sourceNode.getProperty(HTMLCLEANER_ID);
        return htmlcleanerId == null || StringUtils.isBlank(htmlcleanerId.getString());
    }

    private static Collection<String> getCopiedProperties() {
        final Collection<String> copiedProperties = new HashSet<>();
        copiedProperties.add("charset");
        copiedProperties.add("filter");
        copiedProperties.add("omitComments");
        copiedProperties.add("serializer");
        return copiedProperties;
    }

    private Node createHtmlCleanerDestinationNode() throws RepositoryException {
        final Node modulesNode = session.getNode("/hippo:configuration/hippo:modules");
        final String htmlProcessor = "htmlprocessor";
        final Node htmlprocessorNode = modulesNode.hasNode(htmlProcessor) ?
                modulesNode.getNode(htmlProcessor) :
                modulesNode.addNode(htmlProcessor, NT_MODULE);

        final Node moduleConfigNode = htmlprocessorNode.hasNode(HIPPO_MODULECONFIG) ?
                htmlprocessorNode.getNode(HIPPO_MODULECONFIG) : htmlprocessorNode.addNode(HIPPO_MODULECONFIG, HIPPOSYS_MODULE_CONFIG);

        final String richtext = "richtext";
        return moduleConfigNode.hasNode(richtext) ?
                moduleConfigNode.getNode(richtext) : moduleConfigNode.addNode(richtext, HIPPOSYS_MODULE_CONFIG);
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
            for(PropertyIterator iterator = sourceNode.getProperties(); iterator.hasNext();) {
                if (destinationNode == null) {
                    destinationNode = destinationNodeExists ? session.getNode(destinationNodePath) : createUrlRewriterDestinationNode();
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
        String urlrewriter = "urlrewriter";
        final Node urlrewriterNode = modulesNode.hasNode(urlrewriter) ?
                modulesNode.getNode(urlrewriter) :
                modulesNode.addNode(urlrewriter, NT_MODULE);

        return urlrewriterNode.hasNode(HIPPO_MODULECONFIG) ?
                urlrewriterNode.getNode(HIPPO_MODULECONFIG) : urlrewriterNode.addNode(HIPPO_MODULECONFIG, HIPPOSYS_MODULE_CONFIG);
    }

    private void movePropertyToNode(final Property sourceProperty, final Node destinationNode) throws RepositoryException {

        final String propertyName = sourceProperty.getName();
        if (destinationNode.hasProperty(propertyName)) {
            destinationNode.getProperty(propertyName).remove();
        }

        log.info(String.format("Setting property '%s' to destination node '%s'", propertyName, destinationNode.getPath()));
        if (sourceProperty.isMultiple()) {
            destinationNode.setProperty(propertyName, sourceProperty.getValues());
        } else {
            destinationNode.setProperty(propertyName, sourceProperty.getValue());
        }

        log.info(String.format("Removing property '%s' from source node", propertyName));
        sourceProperty.remove();
    }

    private void checkDeprecatedTypeNotInUse(final String nodeType) throws RepositoryException {
        Query query = qm.createQuery("//element(*, " + nodeType + ")", Query.XPATH);
        QueryResult queryResult = query.execute();
        if (queryResult.getNodes().hasNext()) {
            Node node = queryResult.getNodes().nextNode();
            throw new RepositoryException("Deprecated node type "+nodeType+" still in use at '"+node.getPath()+"'. " +
                    "Remove all usage of this node type before upgrading to v12");
        }
    }

    private void migrateDomains() throws RepositoryException {
        if (allowsSNS(NT_DOMAINRULE)) {
            log.info("Migrating "+NT_DOMAINRULE);
            checkOrFixSNS(NT_FACETRULE, true);
            removeAllSNSFromNTD(NT_DOMAINRULE);
        }
        if (allowsSNS(NT_DOMAIN)) {
            log.info("Migrating "+NT_DOMAIN);
            migrateDomainAuthRoles();
            checkOrFixSNS(NT_DOMAINRULE, true);
            removeAllSNSFromNTD(NT_DOMAIN);
        }
    }

    private void migrateModuleConfig() throws RepositoryException {
        if (allowsSNS(HIPPOSYS_MODULE_CONFIG)) {
            log.info("Migrating "+HIPPOSYS_MODULE_CONFIG);
            migrateEformsConfig();
            checkOrFixSNS(HIPPOSYS_MODULE_CONFIG, false);
            removeAllSNSFromNTD(HIPPOSYS_MODULE_CONFIG);
        }
    }

    private void migrateDomainAuthRoles() throws RepositoryException {
        boolean saveNeeded = false;
        log.info("Migrating "+NT_AUTHROLE);
        Query query = qm.createQuery("//element(*, hipposys:authrole)", Query.XPATH);
        QueryResult queryResult = query.execute();
        for (Node node : new NodeIterable(queryResult.getNodes())) {
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
        Query query = qm.createQuery("//element(*, "+nodeTypeName+")", Query.XPATH);
        QueryResult queryResult = query.execute();
        for (Node node : new NodeIterable(queryResult.getNodes())) {
            if (node.getIndex() > 1) {
                if (fixSNS) {
                    moveToUniqueName(node, node.getName());
                    saveNeeded = true;
                }
                else {
                    throw new RepositoryException("Encountered "+nodeTypeName+" SNS at: "+ node.getPath());
                }
            }
        }
        if (!dryRun && saveNeeded) {
            session.save();
        }
    }

    private void migrateEformsConfig() throws RepositoryException {
        if (session.nodeExists("/hippo:configuration/hippo:modules/eforms/hippo:moduleconfig")) {
            log.info("Migrating eforms");
            Node node = session.getNode("/hippo:configuration/hippo:modules/eforms/hippo:moduleconfig");
            if (!dryRun && fixEformsModuleConfigPrimaryType(node)) {
                session.save();
            }
        }
    }

    private boolean fixEformsModuleConfigPrimaryType(Node node) throws RepositoryException {
        boolean saveNeeded = false;
        for (Node child : new NodeIterable(node.getNodes())) {
            if (fixEformsModuleConfigPrimaryType(child)) {
                saveNeeded = true;
            }
        }
        if (!node.isNodeType(HIPPOSYS_MODULE_CONFIG)) {
            saveNeeded = true;
            node.setPrimaryType(HIPPOSYS_MODULE_CONFIG);
            if ("eforms:validationrule".equals(node.getName())) {
                final Property ruleIdProperty = node.getProperty("eforms:validationruleid");
                String ruleId = ruleIdProperty.getString();
                final String newPath = node.getParent().getPath() + "/" + ruleId;
                log.info("checking new path ["+newPath+"]");
                if (session.nodeExists(newPath)) {
                    throw new RepositoryException("Cannot fix eforms:validationrule SNS for node with eforms:validationruleid '"+ruleId+"'. Another SNS found: "+ newPath);
                }
                ruleIdProperty.remove();
                log.info("Renaming "+node.getPath() + " to "+ ruleId);
                session.move(node.getPath(), newPath);
            }
            if ("eforms:daterule".equals(node.getName())) {
                final Property ruleIdProperty = node.getProperty("eforms:dateruleid");
                final String ruleId = ruleIdProperty.getString();
                final String newPath = node.getParent().getPath() + "/" + ruleId;
                if (session.nodeExists(newPath)) {
                    throw new RepositoryException("Cannot fix eforms:daterule SNS for node with eforms:dateruleid '"+ruleId+"'. Another SNS found: "+ newPath);
                }
                ruleIdProperty.remove();
                log.info("Renaming "+node.getPath() + " to "+ ruleId);
                session.move(node.getPath(), newPath);
            }
        }
        return saveNeeded;
    }

    private boolean allowsSNS(final String nodeType) throws RepositoryException {
        NodeType nt = ntm.getNodeType(nodeType);
        for (NodeDefinition nd : nt.getDeclaredChildNodeDefinitions()) {
            if (nd.allowsSameNameSiblings()) {
                return true;
            }
        }
        return false;
    }

    private void removeAllSNSFromNTD(final String nodeTypeName) throws RepositoryException {
        if (!dryRun) {
            log.info("Removing SNS from "+nodeTypeName);
            NodeTypeTemplate ntt = ntm.createNodeTypeTemplate(ntm.getNodeType(nodeTypeName));
            for (Object nd: ntt.getNodeDefinitionTemplates()) {
                NodeDefinitionTemplate ndt = (NodeDefinitionTemplate)nd;
                ndt.setSameNameSiblings(false);
            }
            ntr.ignoreNextConflictingContent();
            ntm.registerNodeType(ntt,true);
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
        log.info("Renaming "+node.getPath() + " to " + nameCandidate + (postFix==0 ? "" : postFix));
        session.move(node.getPath(), newPath);
    }
}
