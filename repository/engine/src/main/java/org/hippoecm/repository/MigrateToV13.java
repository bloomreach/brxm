/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.jackrabbit.HippoNodeTypeRegistry;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.jackrabbit.JcrConstants.MIX_VERSIONABLE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_VERSIONABLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_CONFIGURATION;
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;

public class MigrateToV13 {

    static final Logger log = LoggerFactory.getLogger(MigrateToV13.class);

    public static final String NT_HST_HST = "hst:hst";
    public static final String NT_HST_BLUEPRINT = "hst:blueprint";
    public static final String NT_HST_BLUEPRINTS = "hst:blueprints";
    public static final String NT_HST_CHANNEL = "hst:channel";
    public static final String NT_HST_CHANNELS = "hst:channels";
    public static final String NT_HST_CONFIGURATION = "hst:configuration";
    public static final String NT_HST_CONFIGURATIONS = "hst:configurations";
    public static final String NT_HST_SITE = "hst:site";
    public static final String NT_HST_SITES = "hst:sites";
    public static final String NT_HST_VIRTUALHOSTS = "hst:virtualhosts";

    public static final String OBSOLETE_NT_HIPPO_INITIALIZEITEM = "hippo:initializeitem";
    public static final String OBSOLETE_NT_HIPPO_INITIALIZEFOLDER = "hippo:initializefolder";
    public static final String OBSOLETE_NT_HIPPO_LOCKABLE = "hippo:lockable";
    public static final String OBSOLETE_NT_HIPPO_LOCK = "hippo:lock";
    public static final String OBSOLETE_NT_HIPPOSYS_INITIALIZEITEM = "hipposys:initializeitem";

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
        this.ntm = session.getWorkspace().getNodeTypeManager();
        this.qm = session.getWorkspace().getQueryManager();
    }

    public void migrateIfNeeded() throws RepositoryException {
        if (!ntm.hasNodeType(OBSOLETE_NT_HIPPO_INITIALIZEITEM)) {
            log.debug("No migration needed");
            return;
        }
        ensureNodeTypeNotInUse(OBSOLETE_NT_HIPPO_LOCKABLE, true, new String[]{OBSOLETE_NT_HIPPO_LOCK});
        ensureNodeTypeNotInUse(OBSOLETE_NT_HIPPO_LOCK, false, null);
        ensureNodeTypeNotInUse(OBSOLETE_NT_HIPPO_INITIALIZEFOLDER, false, null);
        ensureNodeTypeNotInUse(OBSOLETE_NT_HIPPOSYS_INITIALIZEITEM, false, null);
        ensureNodeTypeNotInUse(OBSOLETE_NT_HIPPO_INITIALIZEITEM, false, null);

        removeVersionableMixinFromNodeTypes();

        if (dryRun) {
            log.info("MigrateToV13 dry-run completed.");
        } else {
            removeChildNodeFromType(OBSOLETE_NT_HIPPO_INITIALIZEFOLDER, OBSOLETE_NT_HIPPO_INITIALIZEITEM);
            removeChildNodeFromType(NT_CONFIGURATION, OBSOLETE_NT_HIPPO_INITIALIZEFOLDER);
            removeChildNodeFromType(NT_HCM_ROOT, OBSOLETE_NT_HIPPO_LOCK);

            removeNodeType(OBSOLETE_NT_HIPPO_INITIALIZEFOLDER, false);
            removeNodeType(OBSOLETE_NT_HIPPOSYS_INITIALIZEITEM, false);
            removeNodeType(OBSOLETE_NT_HIPPO_LOCK, false);
            removeNodeType(OBSOLETE_NT_HIPPO_LOCKABLE, true);

            removeNodeType(OBSOLETE_NT_HIPPO_INITIALIZEITEM, false);
            log.info("MigrateToV13 completed.");
        }
    }

    private void ensureHippoSysVersionableMixinType() throws RepositoryException {
        if (!ntm.hasNodeType(HIPPOSYS_VERSIONABLE)) {
            log.info("Registering new Mixin {}", HIPPOSYS_VERSIONABLE);
            final NodeTypeTemplate ntt = ntm.createNodeTypeTemplate();
            ntt.setName(HIPPOSYS_VERSIONABLE);
            ntt.setDeclaredSuperTypeNames(new String[]{NodeType.MIX_VERSIONABLE});
            ntt.setMixin(true);
            ntm.registerNodeType(ntt, false);
        } else {
            log.info("Mixin {} already registered", HIPPOSYS_VERSIONABLE);
        }
    }

    private void removeVersionableMixinFromNodeTypes() throws RepositoryException {
        final String[] candidateVersionableNodeTypes = {
                NT_HST_HST, NT_HST_BLUEPRINT, NT_HST_BLUEPRINTS, NT_HST_CHANNEL, NT_HST_CHANNELS, NT_HST_CONFIGURATION,
                NT_HST_CONFIGURATIONS, NT_HST_SITE, NT_HST_SITES, NT_HST_VIRTUALHOSTS
        };
        final Set<NodeTypeDefinition> versionableNodeTypes = new LinkedHashSet<>();
        final Set<String> versionableNodeTypesUsed = new LinkedHashSet<>();
        boolean hasHippoSysVersionableMixinType = false;
        for (final String ntName : candidateVersionableNodeTypes) {
            if (ntm.hasNodeType(ntName)) {
                final NodeTypeDefinition ntd = ntm.getNodeType(ntName);
                if (Arrays.stream(ntd.getDeclaredSupertypeNames()).anyMatch(MIX_VERSIONABLE::equals)) {
                    versionableNodeTypes.add(ntd);
                    if (dryRun) {
                        log.info("Mixin {} will be removed from NodeType {} during the actual migration to v13", MIX_VERSIONABLE, ntName);
                    } else {
                        log.info("Removing Mixin {} from NodeType {}", MIX_VERSIONABLE, ntName);
                    }
                    if (!dryRun && !hasHippoSysVersionableMixinType) {
                        ensureHippoSysVersionableMixinType();
                        hasHippoSysVersionableMixinType = true;
                    }
                    final Query query = qm.createQuery("//element(*, " + ntName + ")", Query.XPATH);
                    QueryResult queryResult = query.execute();

                    for (final Node node : new NodeIterable(queryResult.getNodes())) {
                        if (!dryRun) {
                            log.info("Adding temporarily Mixin {} to {} node at {}",
                                    HIPPOSYS_VERSIONABLE, ntName, node.getPath());
                            JcrUtils.ensureIsCheckedOut(node);
                            node.addMixin(HIPPOSYS_VERSIONABLE);
                            versionableNodeTypesUsed.add(ntName);
                        }
                    }
                }
            }
        }
        if (!dryRun) {
            if (!versionableNodeTypesUsed.isEmpty()) {
                session.save();
            }
            for (final NodeTypeDefinition ntd : versionableNodeTypes) {
                final NodeTypeTemplate ntt = ntm.createNodeTypeTemplate(ntd);
                ntt.setDeclaredSuperTypeNames(
                        Arrays.stream(ntd.getDeclaredSupertypeNames())
                                .filter(n -> !n.equals(MIX_VERSIONABLE))
                                .toArray(String[]::new)
                );
                ntr.ignoreNextConflictingContent();
                ntm.registerNodeType(ntt, true);
                log.info("Mixin {} removed from NodeType {}", MIX_VERSIONABLE, ntd.getName());
            }
            if (!versionableNodeTypesUsed.isEmpty()) {
                for (final String ntName : versionableNodeTypesUsed) {
                    final Query query = qm.createQuery("//element(*, " + ntName + ")", Query.XPATH);
                    QueryResult queryResult = query.execute();
                    for (final Node node : new NodeIterable(queryResult.getNodes())) {
                        if (node.isNodeType(HIPPOSYS_VERSIONABLE)) {
                            log.info("Removing temporarily added Mixin {} from {} node at {}",
                                    HIPPOSYS_VERSIONABLE, ntName, node.getPath());
                            node.removeMixin(HIPPOSYS_VERSIONABLE);
                        }
                    }
                }
                session.save();
            }
        }
    }

    private void removeChildNodeFromType(final String nodeType, final String childNodeType) throws RepositoryException {
        log.info("Removing ChildNodeType {} from NodeType {}", childNodeType, nodeType);
        if (!dryRun && ntm.hasNodeType(nodeType)) {
            final NodeTypeTemplate ntt = ntm.createNodeTypeTemplate(ntm.getNodeType(nodeType));
            boolean found = false;
            for (Iterator<Object> iter = ntt.getNodeDefinitionTemplates().iterator(); iter.hasNext(); ) {
                final NodeDefinitionTemplate ndt = (NodeDefinitionTemplate) iter.next();
                if (Arrays.stream(ndt.getRequiredPrimaryTypeNames()).anyMatch(childNodeType::equals)) {
                    iter.remove();
                    found = true;
                    log.info("Removing ChildNodeDefinition {} of type {} from NodeType {}",
                            ndt.getName(), childNodeType, nodeType);
                    ntr.ignoreNextConflictingContent();
                    ntm.registerNodeType(ntt, true);
                }
            }
            if (!found) {
                log.info("ChildNodeDefinition of type {} not found in NodeType {}. Ignored.", childNodeType, nodeType);
            }
        }
    }

    private void removeNodeType(final String nodeType, final boolean mixin) throws RepositoryException {
        if (ntm.hasNodeType(nodeType)) {
            log.info("Removing "+(mixin ? "Mixin" : "NodeType")+" {}", nodeType);
            ntr.ignoreNextCheckReferencesInContent();
            ntm.unregisterNodeType(nodeType);
        }
    }

    private void ensureNodeTypeNotInUse(final String nodeType, boolean mixin, final String[] mixinPrimaryTypes) throws RepositoryException {
        if (!ntm.hasNodeType(nodeType)) {
            return;
        }
        final Query query = qm.createQuery("//element(*, " + nodeType + ")", Query.XPATH);
        final QueryResult queryResult = query.execute();
        boolean save = false;
        for (final Node node : new NodeIterable(queryResult.getNodes())) {
            if (mixinPrimaryTypes != null && mixin && !Arrays.stream(node.getMixinNodeTypes()).map(nt -> nt.getName()).anyMatch(nodeType::equals)) {
                if (Arrays.stream(mixinPrimaryTypes).anyMatch(node.getPrimaryNodeType().getName()::equals)) {
                    log.info("Mixin {} is part of the primary type {} of node at '{}' which will be removed during the actual migration to v13.",
                            nodeType, node.getPrimaryNodeType().getName(), node.getPath());
                    continue;
                } else {
                    // Unexpected primary nodetype with mixin, this will FAIL at the following node.removeMixin(nodeType)
                    // with a NoSuchNodeTypeException!
                    // This would block migration to v13 but an extremely unlikely case (then requiring custom handling).
                }
            }
            final String type = mixin ? "Mixin" : "Node";
            if (dryRun) {
                log.warn("{} type {} to be dropped is still in use at '{}'.\n" +
                                "All usages of {} will be automatically removed during the actual migrating to v13",
                        type, nodeType, node.getPath(), nodeType);
            } else {
                log.info("Removing still in use {} type {} at '{}'.", type, nodeType, node.getPath());
            }
            boolean skip = false;
            if (!node.isCheckedOut()) {
                if (dryRun) {
                    log.warn("Node at {} is checked-in: skipping dryRun removal.", node.getPath());
                    skip = true;
                } else {
                    JcrUtils.ensureIsCheckedOut(node);
                }
            }
            if (!skip) {
                if (mixin) {
                    node.removeMixin(nodeType);
                } else {
                    node.remove();
                }
                save = true;
            }
        }
        if (save && !dryRun) {
            session.save();
        }
    }
}
