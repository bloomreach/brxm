/*
 * Copyright 2016-2020 Bloomreach
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.HstConfigurationException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorBuilder;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.platform.api.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.platform.api.beans.FieldGroupInfo;
import org.hippoecm.hst.platform.api.beans.HstPropertyDefinitionInfo;
import org.hippoecm.hst.platform.api.beans.InformationObjectsBuilder;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils.getEditingPreviewVirtualHosts;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils.getPreviewHstModel;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils.getRequestContext;

public class ChannelServiceImpl implements ChannelService {
    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private ValidatorFactory validatorFactory;
    private HstConfigurationService hstConfigurationService;
    private org.hippoecm.hst.platform.api.ChannelService channelService;

    private void init() {
        channelService = HippoServiceRegistry.getService(PlatformServices.class).getChannelService();
    }

    @Override
    public ChannelInfoDescription getChannelInfoDescription(final String channelId, final String locale, final String hostGroup)
            throws ChannelException, RepositoryException {
        try {
            final VirtualHosts virtualHosts = getEditingPreviewVirtualHosts();
            final Class<? extends ChannelInfo> channelInfoClass = virtualHosts.getChannelInfoClass(hostGroup, channelId);

            if (channelInfoClass == null) {
                throw new ChannelException("Cannot find ChannelInfo class of the channel with id '" + channelId + "'");
            }

            final List<Class<? extends ChannelInfo>> channelInfoMixins = virtualHosts.getChannelInfoMixins(hostGroup, channelId);

            final List<HstPropertyDefinition> propertyDefinitions = getHstPropertyDefinitions(virtualHosts, channelId, hostGroup);
            final Set<String> annotatedFields = propertyDefinitions.stream()
                    .map(HstPropertyDefinition::getName)
                    .collect(Collectors.toSet());

            final Set<String> hiddenFields = propertyDefinitions.stream()
                    .filter(HstPropertyDefinition::isHiddenInChannelManager)
                    .map(HstPropertyDefinition::getName)
                    .collect(Collectors.toSet());


            final Map<String, HstPropertyDefinitionInfo> visiblePropertyDefinitions = createVisiblePropDefinitionInfos(propertyDefinitions);
            final List<FieldGroupInfo> validFieldGroups = getValidFieldGroups(channelInfoClass, channelInfoMixins,
                    annotatedFields, hiddenFields);
            final Map<String, String> localizedResources = getLocalizedResources(virtualHosts, channelId, locale, hostGroup);

            augmentDropDownAnnotatedPropertyDefinitionValues(visiblePropertyDefinitions, localizedResources);

            final String lockedBy = getChannelLockedBy(virtualHosts, channelId, hostGroup);
            final boolean editable = isChannelSettingsEditable(virtualHosts, channelId, hostGroup);
            return new ChannelInfoDescription(validFieldGroups, visiblePropertyDefinitions, localizedResources, lockedBy, editable);
        } catch (ChannelException | RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e);
            } else {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e.toString());
            }
            throw e;
        }
    }

    /**
     * Inspect {@code propertyDefInfosMap} and augment an annotation of a property definition, the type of which
     * is {@link DropDownList} with {@link DropDownList#valueListProvider() set to a custom class, by setting the
     * value to ones dynamically retrieved from the {@link ValueListProvider} implementation.
     * @param propertyDefInfosMap propertyDefinitionInfo map
     * @param localizedResources localized resources map
     */
    private void augmentDropDownAnnotatedPropertyDefinitionValues(Map<String, HstPropertyDefinitionInfo> propertyDefInfosMap,
            Map<String, String> localizedResources) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final Locale locale = (requestContext != null) ? requestContext.getPreferredLocale() : null;

        for (HstPropertyDefinitionInfo propDefInfo : propertyDefInfosMap.values()) {
            final List<? extends Annotation> annotations = propDefInfo.getAnnotations();
            final List<Annotation> augmentedAnnotations = new ArrayList<>();
            boolean anyItemAugmented = false;

            for (Annotation annotation : annotations) {
                boolean itemAugmented = false;
                DropDownList augmentedAnnotation = null;

                if (DropDownList.class.isAssignableFrom(annotation.getClass())) {
                    final DropDownList dropDownListAnnotation = (DropDownList) annotation;
                    String valueListProviderKey = dropDownListAnnotation.valueListProviderKey();
                    final Class<? extends ValueListProvider> valueListProviderClass =
                        dropDownListAnnotation.valueListProvider();

                    try {
                        final ValueListProvider valueListProvider = ParametersInfoProcessor.getValueListProvider(
                            valueListProviderKey, valueListProviderClass, null);

                        if (valueListProvider != null) {
                            final List<String> valueList = valueListProvider.getValues();

                            // NOTE: The following block adds i18n labels for the dynamic values from the custom ValueListProvider.
                            //       However, Channel Manager currently doesn't use the i18n labels in UI yet, unlike Component Parameters.
                            //       So, let's keep this, which does no harm, even if it's not effective yet.
                            {
                                for (String value : valueList) {
                                    final String displayValueKey = propDefInfo.getName() + "." + value;
                                    if (!localizedResources.containsKey(displayValueKey)) {
                                        final String displayValue = valueListProvider.getDisplayValue(value, locale);
                                        if (displayValue != null) {
                                            localizedResources.put(displayValueKey, displayValue);
                                        }
                                    }
                                }
                            }

                            // Augment the annotation instance by this custom instance to return dynamically resolved values.
                            augmentedAnnotation = new DropDownList() {
                                @Override
                                public Class<? extends Annotation> annotationType() {
                                    return DropDownList.class;
                                }

                                @Override
                                public String[] value() {
                                    return valueList.toArray(new String[valueList.size()]);
                                }

                                @Override
                                public Class<? extends ValueListProvider> valueListProvider() {
                                    return valueListProviderClass;
                                }

                                @Override
                                public String valueListProviderKey() {
                                    return valueListProviderKey;
                                }
                            };

                            itemAugmented = true;
                            anyItemAugmented = true;
                        }
                    } catch (Exception e) {
                        log.error("Failed to create or invoke the custom valueListProvider: '{}'.",
                                valueListProviderClass, e);
                    }
                }

                if (itemAugmented) {
                    augmentedAnnotations.add(augmentedAnnotation);
                } else {
                    augmentedAnnotations.add(annotation);
                }
            }

            if (anyItemAugmented) {
                propDefInfo.setAnnotations(augmentedAnnotations);
            }
        }
    }

    /**
     * Get field groups containing only visible, annotated fields and give warnings on duplicated or without annotation fields
     *
     * @param channelInfoClass
     * @param channelInfoMixins
     * @param annotatedFields a set containing annotated fields
     * @param hiddenFields a set containing fields that mark as hidden
     */
    @SuppressWarnings("unchecked")
    private List<FieldGroupInfo> getValidFieldGroups(final Class<? extends ChannelInfo> channelInfoClass,
                                                     final List<Class<? extends ChannelInfo>> channelInfoMixins,
                                                     final Set<String> annotatedFields, final Set<String> hiddenFields) {
        final ChannelInfoClassInfo channelInfoClassInfo = InformationObjectsBuilder
                .buildChannelInfoClassInfo(channelInfoClass, channelInfoMixins.toArray(new Class[channelInfoMixins.size()]));
        final List<FieldGroupInfo> fieldGroups = channelInfoClassInfo.getFieldGroups();

        final Set<String> allFields = new HashSet<>();
        return fieldGroups.stream().map(fgi -> {
            final Set<String> visibleFieldsInGroup = new LinkedHashSet<>();
            for (String fieldName : fgi.getValue()) {
                if (!allFields.add(fieldName)) {
                    log.warn("Channel property '{}' in group '{}' is encountered multiple times. " +
                            "Please check your ChannelInfo class", fieldName, fgi.getTitleKey());
                    continue;
                }
                if (!annotatedFields.contains(fieldName)) {
                    log.warn("Channel property '{}' in group '{}' is not annotated correctly. " +
                            "Please check your ChannelInfo class", fieldName, fgi.getTitleKey());
                    continue;
                }

                if (!hiddenFields.contains(fieldName)) {
                    visibleFieldsInGroup.add(fieldName);
                }
            }
            return new FieldGroupInfo(visibleFieldsInGroup.toArray(new String[visibleFieldsInGroup.size()]), fgi.getTitleKey());
        }).collect(Collectors.toList());
    }

    private Map<String, HstPropertyDefinitionInfo> createVisiblePropDefinitionInfos(List<HstPropertyDefinition> propertyDefinitions) {
        final List<HstPropertyDefinitionInfo> visiblePropertyDefinitionInfos = propertyDefinitions.stream()
                .filter((propDef) -> !propDef.isHiddenInChannelManager())
                .map(InformationObjectsBuilder::buildHstPropertyDefinitionInfo)
                .collect(Collectors.toList());

        return visiblePropertyDefinitionInfos.stream()
                .collect(Collectors.toMap(HstPropertyDefinitionInfo::getName, Function.identity()));
    }

    private List<HstPropertyDefinition> getHstPropertyDefinitions(final VirtualHosts virtualHosts,
                                                                  final String channelId, final String hostGroup) {
        return virtualHosts.getPropertyDefinitions(hostGroup, channelId);
    }

    private boolean isChannelSettingsEditable(final VirtualHosts virtualHosts, final String channelId, final String hostGroup) {
        Channel channel = virtualHosts.getChannelById(hostGroup, channelId);
        return channel.isChannelSettingsEditable();
    }

    private String getChannelLockedBy(final VirtualHosts virtualHosts, final String channelId, final String hostGroup) {
        final String channelPath = virtualHosts.getChannelById(hostGroup, channelId).getChannelPath();
        try {
            final Node channelNode = RequestContextProvider.get().getSession().getNode(channelPath);
            if (channelNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                return channelNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            }
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.info("Failed to retrieve locked-by information for channel '{}'", channelId, e);
            } else {
                log.info("Failed to retrieve locked-by information for channel '{}'", channelId, e.toString());
            }
        }
        return null;
    }

    private Map<String, String> getLocalizedResources(final VirtualHosts virtualHosts, final String channelId,
                                                      final String language, final String hostGroup) throws ChannelException, RepositoryException {
        final ResourceBundle resourceBundle = virtualHosts.getResourceBundle(getChannel(RequestContextProvider.get().getSession(), channelId, hostGroup), new Locale(language));
        if (resourceBundle == null) {
            // Do not return Collections.EMPTY_MAP since the localized resources should be possible to get appended
            return new HashMap<>();
        }

        return resourceBundle.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), resourceBundle::getString));
    }

    @Override
    public Channel getChannel(final Session session, final String channelId, final String hostGroup) throws ChannelException {

        final Channel channel = channelService.getChannel(channelId, hostGroup);
        if (channel == null) {
            throw new ChannelNotFoundException(channelId);
        }
        return channel;
    }

    @Override
    public void saveChannel(final Session session, final String channelId, final Channel channel, final String hostGroup) throws RepositoryException, IllegalStateException, ChannelException {
        if (!StringUtils.equals(channel.getId(), channelId)) {
            throw new ChannelException("Channel object does not contain the correct id, that should be " + channelId);
        }

        if (!channel.isChannelSettingsEditable()) {
            throw new ChannelException("Channel object is not editable because not part of preview configuration or " +
                    "not part of the HST workspace");
        }

        getPreviewHstModel().getChannelManager().save(session, hostGroup, channel);
    }

    @Override
    public List<Channel> getChannels(final boolean previewConfigRequired,
                                     final boolean workspaceRequired,
                                     final boolean skipBranches,
                                     final boolean skipConfigurationLocked,
                                     final String hostGroup,
                                     final String privilegeAllowed) {
        final VirtualHosts virtualHosts = getEditingPreviewVirtualHosts();
        try {
            final HstRequestContext requestContext = RequestContextProvider.get();
            if (requestContext == null) {
                throw new IllegalStateException("Method #getChannels is not allowed to be invoked without a requestContext");
            }
            final Session session = requestContext.getSession();
            return virtualHosts.getChannels(hostGroup)
                    .values()
                    .stream()
                    .filter(channel -> previewConfigRequiredFiltered(channel, previewConfigRequired))
                    .filter(channel -> workspaceFiltered(virtualHosts, channel, workspaceRequired))
                    .filter(channel -> !skipBranches || channel.getBranchOf() == null)
                    .filter(channel -> !channel.isConfigurationLocked())
                    .filter(channel -> {
                        try {
                            return session.getAccessControlManager()
                                    .hasPrivileges(channel.getHstConfigPath(),
                                            new Privilege[] {session.getAccessControlManager().privilegeFromName(privilegeAllowed)});
                        } catch (RepositoryException e) {
                            log.error("Exception while checking privilege for channel '{}'. Skip channel in result",
                                    channel.getId());
                            return false;
                        }
                    })
                    .sorted(Comparator.comparing(channel -> channel.getName() == null ? "" : channel.getName().toLowerCase()))
                    .collect(Collectors.toList());
        } catch (RepositoryException e) {
            throw new IllegalStateException("Method #getChannels is not allowed to be invoked without a JCR User Session " +
                    "on the request context");
        }
    }

    @Override
    public Optional<Channel> getChannelByMountId(final String mountId, final String hostGroup) {
        if (StringUtils.isBlank(mountId)) {
            throw new IllegalArgumentException("MountId argument must not be blank");
        }
        // below assumes the correct preview virtual hosts model is fetched...
        // requestContext.setAttribute(PREVIEW_EDITING_HST_MODEL_ATTR, previewHstModelSnapshot);
        final VirtualHosts virtualHosts = getEditingPreviewVirtualHosts();
        final List<Mount> mounts = virtualHosts.getMountsByHostGroup(hostGroup);
        return mounts.stream()
                .filter(mount -> StringUtils.equals(mount.getIdentifier(), mountId))
                .map(Mount::getChannel)
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    public boolean canChannelBeDeleted(final String channelId, final String hostGroup) throws ChannelException, RepositoryException {
        return canChannelBeDeleted(getChannel(RequestContextProvider.get().getSession(), channelId, hostGroup));
    }

    @Override
    public boolean canChannelBeDeleted(final Channel channel) {
        return channel.isDeletable();
    }

    @Override
    public boolean isMaster(final Channel channel) {
        return channel.getBranchOf() == null;
    }

    @Override
    public void preDeleteChannel(final Session session, final Channel channel, List<Mount> mountsOfChannel) throws ChannelException, RepositoryException {
        if (!channel.isDeletable() || !isMaster(channel) || channel.isConfigurationLocked()) {
            throw new ChannelException("Requested channel cannot be deleted");
        }

        final Validator hasNoChildMounts = validatorFactory.getHasNoChildMountNodeValidator(mountsOfChannel);
        ValidatorBuilder.builder().add(hasNoChildMounts).build().validate(getRequestContext());
    }

    @Override
    public void deleteChannel(Session session, Channel channel, List<Mount> mountsOfChannel) throws RepositoryException, ChannelException {
        removeConfigurationNodes(channel);
        removeSiteNode(session, channel);
        removeMountNodes(session, mountsOfChannel);
    }


    private void removeConfigurationNodes(final Channel channel) throws RepositoryException {
        final String hstConfigPath = channel.getHstConfigPath();
        try {
            hstConfigurationService.delete(getRequestContext(), channel);
        } catch (HstConfigurationException e) {
            log.info("configuration node '{}' won't be deleted because : {}", hstConfigPath, e.getMessage());
        }
    }

    private void removeSiteNode(final Session session, final Channel channel) throws RepositoryException {
        session.removeItem(channel.getHstMountPoint());
    }

    private void removeMountNodes(final Session session, final List<Mount> allMountsOfChannel) throws RepositoryException {
        final List<String> removingNodePaths = allMountsOfChannel.stream()
                .map(Mount::getIdentifier)
                .map((mountId) -> {
                    try {
                        return session.getNodeByIdentifier(mountId).getPath();
                    } catch (RepositoryException e) {
                        log.debug("Failed to get node of mount " + mountId, e);
                        return StringUtils.EMPTY;
                    }
                })
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        for (String path: removingNodePaths) {
            removeMountNodeAndVirtualHostNodes(session, path);
        }
    }

    @Override
    public List<Mount> findMounts(final Channel channel) {
        final List<Mount> allMounts = loadAllMounts(getEditingPreviewVirtualHosts());

        final String mountPoint = channel.getHstMountPoint();
        return allMounts.stream()
                .filter(mount -> StringUtils.equals(mountPoint, mount.getMountPoint()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<XPageLayout> getXPageLayouts(final Mount mount) {
        final Map<String, XPageLayout> xPageLayoutsMap = channelService.getXPageLayouts(mount);
        return new HashSet<>(xPageLayoutsMap.values());
    }

    private List<Mount> loadAllMounts(final VirtualHosts virtualHosts) {

        final List<Mount> allMounts = new ArrayList<>();
        for (String hostGroup : virtualHosts.getHostGroupNames()) {
            final List<Mount> mountsByHostGroup = virtualHosts.getMountsByHostGroup(hostGroup);
            if (mountsByHostGroup != null) {
                allMounts.addAll(mountsByHostGroup);
            }
        }
        return allMounts;
    }

    /**
     * Remove given node and its ancestor nodes if they become leaf nodes after removal
     */
    private void removeMountNodeAndVirtualHostNodes(final Session session, final String nodePath) throws RepositoryException {
        Node removeNode = session.getNode(nodePath);
        Node parentNode = removeNode.getParent();

        if (removeNode.isNodeType(HstNodeTypes.NODETYPE_HST_MOUNT)) {
            removeNode.remove();

            // Remove ancestor nodes of type 'hst:virtualhost' if they become leaf nodes
            while (parentNode != null && !parentNode.hasNodes() && parentNode.isNodeType(HstNodeTypes.NODETYPE_HST_VIRTUALHOST)) {
                removeNode = parentNode;
                parentNode = parentNode.getParent();
                removeNode.remove();
            }
        }
    }

    private boolean previewConfigRequiredFiltered(final Channel channel, final boolean previewConfigRequired) {
        return !previewConfigRequired || channel.isPreview();
    }

    private boolean workspaceFiltered(final VirtualHosts virtualHosts, final Channel channel, final boolean required) throws RuntimeRepositoryException {
        if (!required) {
            return true;
        }
        final Mount mount = virtualHosts.getMountByIdentifier(channel.getMountId());
        final String workspacePath = mount.getHstSite().getConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
        try {
            return RequestContextProvider.get().getSession().nodeExists(workspacePath);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    public void setValidatorFactory(final ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    public void setHstConfigurationService(final HstConfigurationService hstConfigurationService) {
        this.hstConfigurationService = hstConfigurationService;
    }


}
