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

import javax.jcr.Node;
import javax.jcr.Property;
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

import org.hippoecm.repository.jackrabbit.HippoNodeTypeRegistry;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_MODULE_CONFIG;
import static org.hippoecm.repository.api.HippoNodeType.NT_AUTHROLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAIN;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOMAINRULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_FACETRULE;
import static org.hippoecm.repository.api.HippoNodeType.NT_ROLE;

class MigrateNodeTypesToV12 {

    static final Logger log = LoggerFactory.getLogger(MigrateNodeTypesToV12.class);

    private static String DEPRECATED_NT_HIPPOSYS_AUTOEXPORT = "hipposys:autoexport";

    private final Session session;
    private final HippoNodeTypeRegistry ntr;
    private final NodeTypeManager ntm;
    private final QueryManager qm;

    public MigrateNodeTypesToV12(final Session rootSession, final HippoNodeTypeRegistry ntr) throws RepositoryException {
        this.session = rootSession;
        this.ntr = ntr;
        this.ntm = rootSession.getWorkspace().getNodeTypeManager();
        this.qm = rootSession.getWorkspace().getQueryManager();
    }

    public void migrateIfNeeded() throws RepositoryException {
        if (!ntm.hasNodeType(DEPRECATED_NT_HIPPOSYS_AUTOEXPORT)) {
            log.debug("Already migrated");
            return;
        }

        checkDeprecatedTypeNotInUse(DEPRECATED_NT_HIPPOSYS_AUTOEXPORT);
        migrateDomains();
        migrateModuleConfig();

        ntr.ignoreNextCheckReferencesInContent();
        ntm.unregisterNodeType(DEPRECATED_NT_HIPPOSYS_AUTOEXPORT);
        log.info("Migrated");
        if (!Boolean.getBoolean("repo.migrateToV12immediately")) {
            throw new RuntimeException("Migrated to V12.0.0, please restart again.");
        }
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
        if (saveNeeded) {
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
        if (saveNeeded) {
            session.save();
        }
    }

    private void migrateEformsConfig() throws RepositoryException {
        if (session.nodeExists("/hippo:configuration/hippo:modules/eforms/hippo:moduleconfig")) {
            log.info("Migrating eforms");
            Node node = session.getNode("/hippo:configuration/hippo:modules/eforms/hippo:moduleconfig");
            if (fixEformsModuleConfigPrimaryType(node)) {
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
        log.info("Removing SNS from "+nodeTypeName);
        NodeTypeTemplate ntt = ntm.createNodeTypeTemplate(ntm.getNodeType(nodeTypeName));
        for (Object nd: ntt.getNodeDefinitionTemplates()) {
            NodeDefinitionTemplate ndt = (NodeDefinitionTemplate)nd;
            ndt.setSameNameSiblings(false);
        }
        ntr.ignoreNextConflictingContent();
        ntm.registerNodeType(ntt,true);
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
