/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.NodeImpl;
import org.hippoecm.repository.impl.NodeDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.engine.impl.DigestBundleResolver;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.definition.NamespaceDefinition;
import org.onehippo.cm.model.definition.WebFileBundleDefinition;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.source.FileResourceInputProvider;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ModuleReader;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.ConfigurationNode;
import org.onehippo.cm.model.tree.ConfigurationProperty;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.cm.model.tree.ValueType;
import org.onehippo.cm.model.util.SnsUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.cms7.services.webfiles.watch.WebFilesWatcherService;
import org.onehippo.repository.util.NodeTypeUtils;
import org.onehippo.repository.util.PartialZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.onehippo.cm.engine.ConfigurationServiceImpl.USE_HCM_SITES_MODE;
import static org.onehippo.cm.engine.Constants.HST_DEFAULT_ROOT_PATH;
import static org.onehippo.cm.engine.ValueProcessor.determineVerifiedValues;
import static org.onehippo.cm.engine.ValueProcessor.isKnownDerivedPropertyName;
import static org.onehippo.cm.engine.ValueProcessor.isReferenceTypeProperty;
import static org.onehippo.cm.engine.ValueProcessor.isUuidInUse;
import static org.onehippo.cm.engine.ValueProcessor.propertyIsIdentical;
import static org.onehippo.cm.engine.ValueProcessor.valueFrom;
import static org.onehippo.cm.engine.ValueProcessor.valuesFrom;
import static org.onehippo.cm.model.util.FilePathUtils.getParentOrFsRoot;

/**
 * ConfigurationConfigService is responsible for reading and writing Configuration from/to the repository. Access to the
 * repository is provided to this service through the API ({@link javax.jcr.Node} or {@link Session}), this service is
 * stateless.
 */
public class ConfigurationConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationConfigService.class);

    private static class UnprocessedReference {
        final ConfigurationProperty updateProperty;
        final ConfigurationProperty baselineProperty;
        final Node targetNode;

        UnprocessedReference(ConfigurationProperty updateProperty, ConfigurationProperty baselineProperty, Node targetNode) {
            this.updateProperty = updateProperty;
            this.baselineProperty = baselineProperty;
            this.targetNode = targetNode;
        }
    }

    void writeWebfiles(final List<? extends WebFileBundleDefinition> webfileBundles, final ConfigurationBaselineService baselineService, final Session session)
            throws IOException, RepositoryException {

        if (!webfileBundles.isEmpty()) {

            final WebFilesWatcherService webFilesWatcherService = HippoServiceRegistry.getService(WebFilesWatcherService.class);
            final List<String> watchedModules = collectWatchedWebfileModules(webFilesWatcherService);

            final WebFilesService webFilesService = HippoServiceRegistry.getService(WebFilesService.class);
            if (webFilesService == null) {
                log.warn(String.format("Skipping import web file bundles: '%s' not available.",
                        WebFilesService.class.getName()));
                return;
            }
            for (WebFileBundleDefinition webFileBundleDefinition : webfileBundles) {
                final String bundleName = webFileBundleDefinition.getName();
                log.debug(String.format("processing web file bundle '%s' defined in %s.", bundleName,
                        webFileBundleDefinition.getOrigin()));

                final Module module = webFileBundleDefinition.getSource().getModule();

                //check if webfile service already loaded this module
                if (watchedModules.contains(module.getFullName())) {
                    //Module was already loaded by WebFileService
                    continue;
                }

                if (module.isArchive()) {

                    final PartialZipFile bundleZipFile = new PartialZipFile(module.getArchiveFile(), bundleName);
                    final String fsBundleDigest = DigestBundleResolver.calculateFsBundleDigest(bundleZipFile, webFilesService);
                    boolean reload = shouldReloadBundle(fsBundleDigest, bundleName, webFilesService.getReloadMode(), baselineService, session);
                    if (reload) {
                        webFilesService.importJcrWebFileBundle(session, bundleZipFile, false);
                        final Map<String, String> bundlesDigests = baselineService.getBundlesDigests(session);
                        final String baselineBundleDigest = bundlesDigests.get(bundleName);
                        if ((baselineBundleDigest != null && !baselineBundleDigest.equals(fsBundleDigest)) || baselineBundleDigest == null) {
                            baselineService.addOrUpdateBundleDigest(bundleName, fsBundleDigest, session);
                        }
                    }
                } else {
                    log.debug(String.format("Module '%s' is not an archive, perform bundle reload", module));
                    final FileResourceInputProvider resourceInputProvider =
                            (FileResourceInputProvider) module.getConfigResourceInputProvider();
                    final Path modulePath = getParentOrFsRoot(resourceInputProvider.getBasePath());
                    webFilesService.importJcrWebFileBundle(session, modulePath.resolve(bundleName).toFile(), true);
                }
            }
        }
    }

    /**
     * Collect all webfilebundle modules watched by WebFileWatcherService
     */
    private static List<String> collectWatchedWebfileModules(final WebFilesWatcherService webFilesWatcherService) {

        final List<String> webfileModules = new ArrayList<>();
        if (webFilesWatcherService != null) {
            final List<Path> webFilesDirectories = webFilesWatcherService.getWebFilesDirectories();
            for (final Path webFilesDirectory : webFilesDirectories) {
                final Path moduleDescriptorPath = webFilesDirectory.resolveSibling(Constants.HCM_MODULE_YAML);
                try {
                    // TODO: this is somewhat excessive -- we could load just the module descriptor instead of all the sources
                    final ModuleImpl moduleImpl = new ModuleReader().read(moduleDescriptorPath, false).getModule();
                    webfileModules.add(moduleImpl.getFullName());
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Failed to read webfile bundle module: %s", moduleDescriptorPath), e);
                }
            }
        }
        return webfileModules;
    }

    /**
     * Check if bundle reload is required
     * @param fsBundleDigest jar's bundle digest
     * @param bundleName bundle name
     * @param reloadMode reload mode,<pre/>
     * RELOAD_NEVER - dont reload bundle even if it is modified at filesystem level <pre/>
     * RELOAD_IF_RUNTIME_UNCHANGED - only reload bundle if bundle's runtime digest is consistent with the one from baseline<pre/>
     * RELOAD_DISCARD_RUNTIME_CHANGES - reload bundle event if runtime digest is modified<pre/>
     * @param baselineService baseline service
     * @param session current session
     */
    boolean shouldReloadBundle(final String fsBundleDigest,
                                       final String bundleName,
                                       final String reloadMode,
                                       final ConfigurationBaselineService baselineService,
                                       final Session session) throws IOException, RepositoryException {

        final Optional<String> baselineBundleDigest = baselineService.getBaselineBundleDigest(bundleName, session);
        if (!baselineBundleDigest.isPresent()) {
            log.info("baseline bundle does not exist, first bootstrap, (re)load");
            return true;
        } else if (fsBundleDigest.equals(baselineBundleDigest.get())) {
            log.info("classpath & baseline bundle's digests are same, skip reload");
            return false;
        } else {
            log.info("classpath & baseline bundles digests are different");
            final JcrPath bundlePath = JcrPaths.getPath(WebFilesService.JCR_ROOT_PATH, bundleName);
            final boolean bundleNodeExists = session.nodeExists(bundlePath.toString());
            if (bundleNodeExists) {
                switch (reloadMode) {
                    case WebFilesService.RELOAD_NEVER:
                        log.info("reload mode is set to NEVER, skip reload");
                        return false;
                    case WebFilesService.RELOAD_IF_RUNTIME_UNCHANGED:
                        final Node bundleNode = session.getNode(bundlePath.toString());
                        final String runtimeDigest = DigestBundleResolver.calculateRuntimeBundleDigest(bundleNode);
                        boolean shouldReload = runtimeDigest.equals(baselineBundleDigest.get());
                        log.info("reload mode is set to RELOAD_IF_RUNTIME_UNCHANGED, should reload: {}", shouldReload);
                        return shouldReload;
                    case WebFilesService.RELOAD_DISCARD_RUNTIME_CHANGES:
                        log.warn(String.format("Web bundle '%s' will be force reloaded and runtime changes will be lost", bundleName));
                        return true;
                    default:
                        throw new RuntimeException(String.format("Unsupported reload mode '%s'", reloadMode));
                }
            } else {
                log.warn(String.format("Bundle name '%s' does not exist in repository, skip processing bundle", bundlePath));
            }
        }
        return false;
    }

    /**
     * Compute the difference between the baseline configuration and the update configuration, and apply it to the
     * repository.
     *
     * @param baseline baseline configuration for computing the delta
     * @param update   updated configuration, potentially different from baseline
     * @param session  JCR session to write to the repository. The caller has to take care of #save-ing any changes.
     * @param forceApply flag indicating that runtime changes to configuration data should be reverted
     * @throws RepositoryException in case of failure reading from / writing to the repository
     * @throws IOException in case of failure reading external resources
     */
    void computeAndWriteDelta(final ConfigurationModel baseline,
                              final ConfigurationModel update,
                              final Session session,
                              final boolean forceApply) throws RepositoryException, IOException {

        // Note: Namespaces, once they are in the repository, cannot be changed or removed.
        //       Therefore, both the baseline configuration and the forceApply flag are immaterial
        //       to the handling of namespaces. The same applies to node types, at least, as far as
        //       BootstrapUtils#initializeNodetypes offers support.

        final Collection<? extends NamespaceDefinition> updateNsDefs =
                removeDuplicateNamespaceDefinitions(update.getNamespaceDefinitions());

        final Collection<? extends NamespaceDefinition> baselineNsDefs =
                removeDuplicateNamespaceDefinitions(baseline.getNamespaceDefinitions());

        applyNamespaces(updateNsDefs, session);
        applyNodeTypes(baselineNsDefs, updateNsDefs, session);

        final ConfigurationNode baselineRoot = baseline.getConfigurationRootNode();
        final Node targetNode = session.getNode(baselineRoot.getPath());
        final List<UnprocessedReference> unprocessedReferences = new ArrayList<>();

        computeAndWriteNodeDelta(baselineRoot, update.getConfigurationRootNode(), targetNode,
                false, forceApply, unprocessedReferences);
        postProcessReferences(unprocessedReferences);
    }

    /**
     * Return a collection of namespace definitions without duplicates. Only last namespace
     * in a list with the same prefix is preserved
     * @param definitions Definitions to clean
     */
    private Collection<? extends NamespaceDefinition> removeDuplicateNamespaceDefinitions(final List<? extends NamespaceDefinition> definitions) {
        return definitions.stream().collect(
                Collectors.toMap(NamespaceDefinition::getPrefix, identity(), (d1, d2) -> {
                    log.warn(String.format("Duplicate namespace definitions %s, %s", d1, d2));
                    return d2;
                }, LinkedHashMap::new))
                .values();
    }

    private void applyNamespaces(final Collection<? extends NamespaceDefinition> namespaceDefinitions, final Session session)
            throws RepositoryException {

        final Optional<? extends NamespaceDefinition> cndExtension =
                namespaceDefinitions.stream().filter(ns -> (ns.getSource().getModule().isHcmSite())).findFirst();
        cndExtension.ifPresent(def -> {
            final String msg = String.format("Failed to process namespace definition defined in %s: " +
                    "namespace with prefix '%s'. Namespace definition can not be a part of extension module: '%s'",
                    def.getOrigin(), def.getPrefix(), def.getSource().getModule());
            throw new ConfigurationRuntimeException(msg);
        });

        final Set<String> prefixes = new HashSet<>(Arrays.asList(session.getNamespacePrefixes()));

        for (NamespaceDefinition namespaceDefinition : namespaceDefinitions) {
            final String prefix = namespaceDefinition.getPrefix();
            final String uriString = namespaceDefinition.getURI().toString();

            log.debug(String.format("processing namespace prefix='%s' uri='%s' defined in %s.",
                    prefix, uriString, namespaceDefinition.getOrigin()));

            if (prefixes.contains(prefix)) {
                final String repositoryURI = session.getNamespaceURI(prefix);
                if (!uriString.equals(repositoryURI)) {
                    final String msg = String.format("Failed to process namespace definition defined in %s: " +
                                    "namespace with prefix '%s' already exists in repository with different URI. " +
                                    "Existing: '%s', target: '%s'. Changing existing namespaces is not supported. Aborting.",
                            namespaceDefinition.getOrigin(), prefix, repositoryURI, uriString);
                    throw new ConfigurationRuntimeException(msg);
                }
            } else {
                session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uriString);
            }
        }
    }

    private void applyNodeTypes(final Collection<? extends NamespaceDefinition> baselineDefs,
                                final Collection<? extends NamespaceDefinition> nsDefinitions,
                                final Session session) throws RepositoryException, IOException {
        // index baseline defs by prefix for faster lookups later
        final ImmutableMap<String, ? extends NamespaceDefinition> baselineDefsByPrefix =
                Maps.uniqueIndex(baselineDefs, NamespaceDefinition::getPrefix);

        for (NamespaceDefinition nsDefinition : nsDefinitions) {
            // skip namespace defs with no CND
            if (nsDefinition.getCndPath() == null) {
                continue;
            }

            final String cndPath = nsDefinition.getCndPath().getString();

            // find baseline version of this namespace def, if one exists
            final NamespaceDefinition baselineDef = baselineDefsByPrefix.get(nsDefinition.getPrefix());

            final boolean reloadCND;
            if (baselineDef != null && baselineDef.getCndPath() != null) {

                // check if the baseline version of the CND is exactly bytewise equal to the new CND
                // CNDs are small enough to just do the full bytewise compare instead of hashing
                try (final InputStream baselineCND = baselineDef.getCndPath().getResourceInputStream();
                    final InputStream newCND = nsDefinition.getCndPath().getResourceInputStream()) {

                    // don't reload if the new CND exactly matches the old one
                    reloadCND = !IOUtils.contentEquals(baselineCND, newCND);
                }
                if (!reloadCND && log.isDebugEnabled()) {
                    log.debug(String.format("skipping CND already loaded in baseline: '%s' defined in %s.", cndPath, nsDefinition.getOrigin()));
                }
            }
            else {
                // no matching baseline also means we should reload the CND
                reloadCND = true;
            }

            if (reloadCND) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("processing CND '%s' defined in %s.", cndPath, nsDefinition.getOrigin()));
                }
                final String cndPathOrigin = String.format("'%s' (%s)", cndPath, nsDefinition.getOrigin());
                try (final InputStream nodeTypeStream = nsDefinition.getCndPath().getResourceInputStream()) {
                    NodeTypeUtils.initializeNodeTypes(session, nodeTypeStream, cndPathOrigin);
                }
            }
        }
    }

    private void computeAndWriteNodeDelta(final ConfigurationNode baselineNode,
                                          final ConfigurationNode updateNode,
                                          final Node targetNode,
                                          final boolean isNew,
                                          final boolean forceApply,
                                          final List<UnprocessedReference> unprocessedReferences)
            throws RepositoryException, IOException {

        if (targetNode.isLocked()) {
            log.warn("Target node {} is locked, skipping its processing", targetNode.getPath());
            final LockManager lockManager = targetNode.getSession().getWorkspace().getLockManager();
            try {
                final Lock lock = lockManager.getLock(targetNode.getPath());
                if (lock.isDeep()) {
                    return;
                }
            } catch (LockException ignored) {
            }
        } else {
            computeAndWritePrimaryTypeDelta(baselineNode, updateNode, targetNode, forceApply);
            computeAndWriteMixinTypesDelta(baselineNode, updateNode, targetNode, forceApply);
            computeAndWritePropertiesDelta(baselineNode, updateNode, targetNode, isNew, forceApply, unprocessedReferences);
        }

        computeAndWriteChildNodesDelta(baselineNode, updateNode, targetNode, forceApply, unprocessedReferences);
    }

    private void computeAndWritePrimaryTypeDelta(final ConfigurationNode baselineNode,
                                                 final ConfigurationNode updateNode,
                                                 final Node targetNode,
                                                 final boolean forceApply)
            throws RepositoryException {

        final ConfigurationProperty baselinePrimaryTypeProperty = baselineNode.getProperty(JCR_PRIMARYTYPE);
        final ConfigurationProperty updatePrimaryTypeProperty = updateNode.getProperty(JCR_PRIMARYTYPE);

        final String updatePrimaryType = updatePrimaryTypeProperty.getValue().getString();
        final String baselinePrimaryType = baselinePrimaryTypeProperty.getValue().getString();

        if (forceApply || !updatePrimaryType.equals(baselinePrimaryType)) {
            final String jcrPrimaryType = targetNode.getPrimaryNodeType().getName();
            if (jcrPrimaryType.equals(updatePrimaryType)) {
                return;
            }

            if (!jcrPrimaryType.equals(baselinePrimaryType)) {
                final String msg = forceApply
                        ? String.format("[OVERRIDE] Primary type '%s' of node '%s' is adjusted to '%s' as defined in %s.",
                                jcrPrimaryType, updateNode.getPath(), updatePrimaryType,
                        updatePrimaryTypeProperty.getOrigin())
                        : String.format("[OVERRIDE] Primary type '%s' of node '%s' has been changed from '%s'."
                                + "Overriding to type '%s', defined in %s.",
                                jcrPrimaryType, updateNode.getPath(), baselinePrimaryType, updatePrimaryType,
                        updatePrimaryTypeProperty.getOrigin());
                log.info(msg);
            }
            targetNode.setPrimaryType(updatePrimaryType);
        }
    }

    private void computeAndWriteMixinTypesDelta(final ConfigurationNode baselineNode,
                                                final ConfigurationNode updateNode,
                                                final Node targetNode,
                                                final boolean forceApply) throws RepositoryException {

        final ConfigurationProperty baselineMixinProperty = baselineNode.getProperty(JCR_MIXINTYPES);
        final ConfigurationProperty updateMixinProperty = updateNode.getProperty(JCR_MIXINTYPES);

        // Update the mixin types, if needed
        final List<? extends Value> updateMixinValues = updateMixinProperty != null ? updateMixinProperty.getValues() : emptyList();
        final List<? extends Value> baselineMixinValues = baselineMixinProperty != null ? baselineMixinProperty.getValues() : emptyList();

        // Add / restore mixin types
        for (Value mixinValue : updateMixinValues) {
            final String mixin = mixinValue.getString();
            if (!hasMixin(targetNode, mixin)) {
                if (forceApply) {
                    if (hasMixin(baselineMixinValues, mixin)) {
                        final String msg = String.format("[OVERRIDE] Mixin '%s' has been removed from node '%s', " +
                                        "but is re-added because it is defined at %s.",
                                mixin, updateNode.getPath(), updateMixinProperty.getOrigin());
                        log.info(msg);
                    }
                    targetNode.addMixin(mixin);
                } else {
                    // only add the mixin in case of a delta with the baseline
                    if (!hasMixin(baselineMixinValues, mixin)) {
                        targetNode.addMixin(mixin);
                    }
                }
            }
        }

        // Remove / clean up mixin types
        if (forceApply) {
            for (NodeType mixinType : targetNode.getMixinNodeTypes()) {
                final String jcrMixin = mixinType.getName();
                if (!hasMixin(updateMixinValues, jcrMixin)) {
                    if (!hasMixin(baselineMixinValues, jcrMixin)) {
                        final String msg = String.format("[OVERRIDE] Mixin '%s' has been added to node '%s'," +
                                        " but is removed because it is not present in definition %s.",
                                jcrMixin, updateNode.getPath(), updateNode.getOrigin());
                        log.info(msg);
                    }

                    removeMixin(targetNode, jcrMixin);
                }
            }
        } else {
            for (Value baselineMixinValue : baselineMixinValues) {
                final String baselineMixin = baselineMixinValue.getString();
                if (!hasMixin(updateMixinValues, baselineMixin)) {
                    removeMixin(targetNode, baselineMixin);
                }
            }
        }
    }

    private boolean hasMixin(final Node node, final String mixin) throws RepositoryException {
        return Arrays.stream(node.getMixinNodeTypes()).anyMatch(mixinType -> mixinType.getName().equals(mixin));
    }

    private boolean hasMixin(final List<? extends Value> mixinValues, final String mixin) {
        return mixinValues.stream().anyMatch(mixinValue -> mixinValue.getString().equals(mixin));
    }

    private void removeMixin(final Node node, final String mixin) throws RepositoryException {
        for (NodeType mixinType : node.getMixinNodeTypes()) {
            if (mixinType.getName().equals(mixin)) {
                node.removeMixin(mixin);
                return;
            }
        }
    }

    private void computeAndWritePropertiesDelta(final ConfigurationNode baselineNode,
                                                final ConfigurationNode updateNode,
                                                final Node targetNode,
                                                final boolean isNew,
                                                final boolean forceApply,
                                                final List<UnprocessedReference> unprocessedReferences)
            throws RepositoryException, IOException {

        for (final ConfigurationProperty updateProperty : updateNode.getProperties()) {
            final String propertyName = updateProperty.getName();
            if (propertyName.equals(JCR_PRIMARYTYPE) || propertyName.equals(JCR_MIXINTYPES)) {
                continue; // we have already addressed these properties
            }
            if (propertyName.equals(JCR_UUID)) {
                continue; // nor need to look at immutable jcr:uuid
            }

            // skip initial-value system property, if this node is not new (and forceApply is false)
            // todo: should this perhaps only apply initial value on forceApply if property is missing?
            final ConfigurationItemCategory category = updateNode.getChildPropertyCategory(propertyName);
            if (category == ConfigurationItemCategory.SYSTEM && !isNew) {
                continue;
            }

            final ConfigurationProperty baselineProperty = baselineNode.getProperty(propertyName);

            if (forceApply || baselineProperty == null || !propertyIsIdentical(updateProperty, baselineProperty)) {
                if (isReferenceTypeProperty(updateProperty)) {
                    unprocessedReferences.add(new UnprocessedReference(updateProperty, baselineProperty, targetNode));
                } else {
                    updateProperty(updateProperty, baselineProperty, targetNode);
                }
            }
        }

        // Remove deleted properties
        if (forceApply) {
            for (String propertyName : getDeletablePropertyNames(targetNode)) {
                if (updateNode.getProperty(propertyName) == null
                        // make sure not to remove properties (now) of category SYSTEM, not even with forceApply==true
                        && updateNode.getChildPropertyCategory(propertyName) == ConfigurationItemCategory.CONFIG) {
                    removeProperty(propertyName, baselineNode.getProperty(propertyName), targetNode, updateNode);
                }
            }
        } else {
            for (final ConfigurationProperty property : baselineNode.getProperties()) {
                final String propertyName = property.getName();
                if (!propertyName.equals(JCR_PRIMARYTYPE)
                        && !propertyName.equals(JCR_MIXINTYPES)
                        && updateNode.getProperty(propertyName) == null
                        // make sure not to remove properties (now) of category SYSTEM
                        && updateNode.getChildPropertyCategory(propertyName) == ConfigurationItemCategory.CONFIG) {
                    removeProperty(propertyName, property, targetNode, updateNode);
                }
            }
        }
    }

    private List<String> getDeletablePropertyNames(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Property property : new PropertyIterable(node.getProperties())) {
            final ItemDefinition definition = property.getDefinition();
            final String name = property.getName();
            if (!definition.isProtected() && !definition.isMandatory() && !isKnownDerivedPropertyName(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private void computeAndWriteChildNodesDelta(final ConfigurationNode baselineNode,
                                                final ConfigurationNode updateNode,
                                                final Node targetNode,
                                                final boolean forceApply,
                                                final List<UnprocessedReference> unprocessedReferences)
            throws RepositoryException, IOException {

        // Add or update child nodes

        for (final ConfigurationNode updateChild : updateNode.getNodes()) {
            final JcrPathSegment nameAndIndex = updateChild.getJcrName().forceIndex();
            ConfigurationNode baselineChild = baselineNode.getNode(nameAndIndex);
            final Node existingChildNode = getChildWithIndex(targetNode, nameAndIndex.getName(), nameAndIndex.getIndex());
            final Node childNode;

            final boolean isNew = (existingChildNode == null);
            if (isNew) {
                // need to add node
                final String childPrimaryType = updateChild.getProperty(JCR_PRIMARYTYPE).getValue().getString();
                final String childName = nameAndIndex.getName();

                if (baselineChild == null) {
                    baselineChild = newChildOfType(childPrimaryType);
                } else {
                    if (forceApply) {
                        final String msg = String.format("[OVERRIDE] Node '%s' has been removed, " +
                                        "but will be re-added due to definition %s.",
                                updateChild.getPath(), updateChild.getOrigin());
                        log.info(msg);
                    } else {
                        // In the baseline, the child exists. Some of its properties or (nested) children may
                        // have changed, but the current implementation considers the manual removal of the
                        // child node more important, so we don't compute if there is a difference between
                        // baselineChild and updateChild, and refrain from re-adding the deleted child node.
                        continue;
                    }
                }
                childNode = addNode(targetNode, childName, childPrimaryType, updateChild);
            } else {
                if (baselineChild == null) {
                    final String childPrimaryType = existingChildNode.getPrimaryNodeType().getName();
                    baselineChild = newChildOfType(childPrimaryType);
                }
                childNode = existingChildNode;
            }

            // recurse
            computeAndWriteNodeDelta(baselineChild, updateChild, childNode, isNew, forceApply, unprocessedReferences);
        }

        // Remove child nodes that are not / no longer in the model.

        final List<JcrPathSegment> indexedNamesOfToBeRemovedChildren = new ArrayList<>();
        if (forceApply) {
            for (Node childNode : new NodeIterable(targetNode.getNodes())) {
                final JcrPathSegment indexedChildName = JcrPaths.getSegment(childNode).forceIndex();
                if (updateNode.getNode(indexedChildName) == null) {
                    if (updateNode.getChildNodeCategory(indexedChildName) == ConfigurationItemCategory.CONFIG) {
                        indexedNamesOfToBeRemovedChildren.add(indexedChildName);
                    }
                }
            }
        } else {
            // Note: SNS is supported because the node's name includes the index.
            //       But it's brittle: basically, we can only correctly remove SNS children if we
            //       remove the children with the highest index from the baseline configuration only,
            //       while additional runtime/repository SNS nodes have not been added, or at the end only.
            //       This is why we process the child nodes of the baseline model in *reverse* order.

            final List<JcrPathSegment> reversedIndexedBaselineChildNames =
                    baselineNode.getNodes().stream().map(ConfigurationNode::getJcrName).collect(Collectors.toList());
            Collections.reverse(reversedIndexedBaselineChildNames);

            for (JcrPathSegment indexedChildName : reversedIndexedBaselineChildNames) {
                if (updateNode.getNode(indexedChildName) == null) {
                    indexedNamesOfToBeRemovedChildren.add(indexedChildName);
                }
            }
        }

        for (JcrPathSegment nameAndIndex : indexedNamesOfToBeRemovedChildren) {
            final Node childNode = getChildWithIndex(targetNode, nameAndIndex.getName(), nameAndIndex.getIndex());
            if (childNode != null) {
                if (baselineNode.getNode(nameAndIndex) == null) {
                    final String msg = String.format("[OVERRIDE] Child node '%s' exists, " +
                                    "but will be deleted while processing the children of node '%s' defined in %s.",
                            nameAndIndex, updateNode.getPath(), updateNode.getOrigin());
                    log.info(msg);
                }
//                else {
//                    // [OVERRIDE] We don't currently check if the removed node has changes compared to the baseline.
//                    //            Such a check would be rather invasive (potentially full sub-tree compare)
//                }
                removeNode(childNode);
            }
        }

        // Care for node ordering?
        final boolean orderingIsRelevant = targetNode.getPrimaryNodeType().hasOrderableChildNodes()
                && (updateNode.getIgnoreReorderedChildren() == null || !updateNode.getIgnoreReorderedChildren());
        if (orderingIsRelevant && updateNode.getNodes().size() > 0) {
            reorderChildren(targetNode, updateNode.getNodes().stream().map(ConfigurationNode::getName).collect(Collectors.toList()));
        }
    }

    protected void removeNode(final Node childNode) throws RepositoryException {

        final JcrPath excludedPath = JcrPaths.getPath(HST_DEFAULT_ROOT_PATH);
        if (USE_HCM_SITES_MODE) {
            final JcrPath nodePath = JcrPaths.getPath(childNode.getPath());
            if (nodePath.startsWith(excludedPath)) {
                log.info("Skipping '/hst:hst' node removal");
                return;
            }
        }
        childNode.remove();
    }

    private Node addNode(final Node parentNode, final String childName, final String childPrimaryType,
                         final ConfigurationNode childModelNode) throws RepositoryException {
        final ConfigurationProperty uuidProperty = childModelNode.getProperty(JCR_UUID);
        if (uuidProperty != null) {
            final String uuid = uuidProperty.getValue().getString();
            if (!isUuidInUse(uuid, parentNode.getSession())) {
                // uuid not in use: create node with the requested uuid
                final NodeImpl parentNodeImpl = (NodeImpl) NodeDecorator.unwrap(parentNode);
                return parentNodeImpl.addNodeWithUuid(childName, childPrimaryType, uuid);
            } else {
                log.warn(String.format("Specified jcr:uuid %s for node '%s' defined in %s already in use: "
                                + "a new jcr:uuid will be generated instead.",
                        uuid, childModelNode.getPath(), childModelNode.getOrigin()));
            }
        }
        return parentNode.addNode(childName, childPrimaryType);
    }

    private ConfigurationNode newChildOfType(final String primaryType) {
        final ConfigurationNodeImpl child = new ConfigurationNodeImpl();
        final ConfigurationPropertyImpl property = new ConfigurationPropertyImpl();
        property.setName(JCR_PRIMARYTYPE);
        property.setKind(PropertyKind.SINGLE);
        property.setValueType(ValueType.NAME);
        property.setValue(new ValueImpl(primaryType));
        child.addProperty(property.getName(), property);
        return child;
    }

    /**
     * Put nodes into the appropriate order:
     *
     * We bring the children of parent into the desired order specified by indexedModelChildNames
     * by ignoring/skipping non-model child nodes of parent, such that these children
     * stay "in place", rather than trickle down to the end of parent's list of children.
     */
    private void reorderChildren(final Node parent, final List<String> indexedModelChildNames) throws RepositoryException {
        final List<String> indexedTargetNodeChildNames = new ArrayList<>();
        final NodeIterator targetChildNodes = parent.getNodes();
        while (targetChildNodes.hasNext()) {
            final Node targetChildNode = targetChildNodes.nextNode();
            indexedTargetNodeChildNames.add(SnsUtils.createIndexedName(targetChildNode));
        }

        for (int modelIndex = 0, targetIndex = 0; modelIndex < indexedModelChildNames.size(); modelIndex++) {
            final String indexedModelName = indexedModelChildNames.get(modelIndex);
            if (indexedTargetNodeChildNames.contains(indexedModelName)) {
                String indexedTargetName = indexedTargetNodeChildNames.get(targetIndex);
                while (indexedModelChildNames.indexOf(indexedTargetName) < modelIndex) {
                    targetIndex++; // skip target node, it isn't part of the model.
                    indexedTargetName = indexedTargetNodeChildNames.get(targetIndex);
                }

                if (indexedTargetName.equals(indexedModelName)) {
                    // node is at appropriate position, do nothing
                    targetIndex++;
                } else {
                    // node is at a later position, reorder it
                    parent.orderBefore(indexedModelName, indexedTargetName);
                }
            }

        }
    }

    private Node getChildWithIndex(final Node parent, final String name, final int index) throws RepositoryException {
        final NodeIterator existingSnsNodes = parent.getNodes(name);
        Node sibling = null;
        for (int i = 0; i < index; i++) {
            if (existingSnsNodes.hasNext()) {
                sibling = existingSnsNodes.nextNode();
            } else {
                return null;
            }
        }
        return sibling;
    }

    private void updateProperty(final ConfigurationProperty updateProperty,
                                final ConfigurationProperty baselineProperty,
                                final Node jcrNode)
            throws RepositoryException, IOException {

        if (isKnownDerivedPropertyName(updateProperty.getName())) {
            return; // TODO: should derived properties even be an allowed part of the configuration model?
        }

        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, updateProperty.getName());

        if (jcrProperty != null && jcrProperty.getDefinition().isProtected()) {
            return; // protected properties cannot be update, no need to even check or try
        }

        // pre-process the values of the property to address reference type values
        final Session session = jcrNode.getSession();
        final List<Value> verifiedUpdateValues = determineVerifiedValues(updateProperty, session);

        if (jcrProperty != null) {
            if (propertyIsIdentical(jcrProperty, updateProperty, verifiedUpdateValues)) {
                return; // no update needed
            }

            if (baselineProperty != null) {
                // property should already exist, and so it does. But has it been changed?
                if (!propertyIsIdentical(jcrProperty, baselineProperty)) {
                    final String msg = String.format("[OVERRIDE] Property '%s' has been changed in the repository, " +
                                    "and will be overridden due to definition %s.",
                            updateProperty.getPath(), updateProperty.getOrigin());
                    log.info(msg);
                }
            } else {
                // property should not yet exist, but does, with a different value.
                // This should not trigger an [OVERRIDE] message if the property is autoCreated and its parent
                // has just been created.
                if (!(jcrProperty.getDefinition().isAutoCreated() && jcrNode.isNew())) {
                    final String msg = String.format("[OVERRIDE] Property '%s' has been created in the repository, " +
                                    "and will be overridden due to definition %s.",
                            updateProperty.getPath(), updateProperty.getOrigin());
                    log.info(msg);
                }
            }

            // TODO: is this check adding sufficient value, or can/should we always remove the old property?
            if (updateProperty.getValueType().ordinal() != jcrProperty.getType()
                    || updateProperty.isMultiple() != jcrProperty.isMultiple()) {
                jcrProperty.remove();
            }
        } else {
            if (baselineProperty != null) {
                // property should already exist, doesn't.
                final String msg = String.format("[OVERRIDE] Property '%s' has been deleted from the repository, " +
                                "and will be re-added due to definition %s.",
                        updateProperty.getPath(), updateProperty.getOrigin());
                log.info(msg);
            }
        }

        try {
            if (updateProperty.isMultiple()) {
                jcrNode.setProperty(updateProperty.getName(), valuesFrom(updateProperty, verifiedUpdateValues, session),
                        updateProperty.getValueType().ordinal());
            } else {
                if (verifiedUpdateValues.size() > 0) {
                    jcrNode.setProperty(updateProperty.getName(), valueFrom(updateProperty, verifiedUpdateValues.get(0), session));
                }
            }
        } catch (RepositoryException e) {
            String msg = String.format(
                    "Failed to process property '%s' defined in %s: %s",
                    updateProperty.getPath(), updateProperty.getOrigin(), e.getMessage());
            throw new ConfigurationRuntimeException(msg, e);
        }
    }

    private void removeProperty(final String propertyName,
                                final ConfigurationProperty baselineProperty,
                                final Node jcrNode,
                                final ConfigurationNode modelNode) throws RepositoryException, IOException {
        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, propertyName);
        if (jcrProperty == null) {
            return; // Successful merge, no action needed.
        }

        if (jcrProperty.getDefinition().isProtected()) {
            return; // protected properties cannot be removed, no need to even check or try
        }

        if (baselineProperty != null) {
            if (!propertyIsIdentical(jcrProperty, baselineProperty)) {
                final String msg = String.format("[OVERRIDE] Property '%s' originally defined in %s has been changed, " +
                                "but will be deleted because it no longer is part of the configuration model.",
                        baselineProperty.getPath(), baselineProperty.getOrigin());
                log.info(msg);
            }
        } else {
            final String msg = String.format("[OVERRIDE] Property '%s' of node '%s' has been added to the repository, " +
                            "but will be deleted because it is not defined in %s.",
                    propertyName, jcrNode.getPath(), modelNode.getOrigin());
            log.info(msg);
        }

        jcrProperty.remove();
    }

    private void postProcessReferences(final List<UnprocessedReference> references)
            throws RepositoryException, IOException {
        for (UnprocessedReference reference : references) {
            updateProperty(reference.updateProperty, reference.baselineProperty, reference.targetNode);
        }
    }
}
