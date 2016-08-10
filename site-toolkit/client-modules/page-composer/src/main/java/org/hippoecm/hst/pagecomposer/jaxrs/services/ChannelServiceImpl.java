/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.hippoecm.hst.rest.beans.FieldGroupInfo;
import org.hippoecm.hst.rest.beans.HstPropertyDefinitionInfo;
import org.hippoecm.hst.rest.beans.InformationObjectsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelServiceImpl implements ChannelService {
    private static final Logger log = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private ChannelManager channelManager;

    @Override
    public ChannelInfoDescription getChannelInfoDescription(final String channelId, final String locale) throws ChannelException {
        try {
            Class<? extends ChannelInfo> channelInfoClass = getAllVirtualHosts().getChannelInfoClass(getCurrentVirtualHost().getHostGroupName(), channelId);

            if (channelInfoClass == null) {
                throw new ChannelException("Cannot find ChannelInfo class of the channel with id '" + channelId + "'");
            }

            final List<HstPropertyDefinition> propertyDefinitions = getHstPropertyDefinitions(channelId);
            final Set<String> annotatedFields = propertyDefinitions.stream()
                    .map(HstPropertyDefinition::getName)
                    .collect(Collectors.toSet());

            final Set<String> hiddenFields = propertyDefinitions.stream()
                    .filter(HstPropertyDefinition::isHiddenInChannelManager)
                    .map(HstPropertyDefinition::getName)
                    .collect(Collectors.toSet());


            final Map<String, HstPropertyDefinitionInfo> visiblePropertyDefinitions = createVisiblePropDefinitionInfos(propertyDefinitions);

            final List<FieldGroupInfo> validFieldGroups = getValidFieldGroups(channelInfoClass, annotatedFields, hiddenFields);
            final Map<String, String> localizedResources = getLocalizedResources(channelId, locale);

            final String lockedBy = getChannelLockedBy(channelId);
            return new ChannelInfoDescription(validFieldGroups, visiblePropertyDefinitions, localizedResources, lockedBy);
        } catch (ChannelException e) {
            if (log.isDebugEnabled()) {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e);
            } else {
                log.info("Failed to retrieve channel info class for channel with id '{}'", channelId, e.toString());
            }
            throw e;
        }
    }

    /**
     * Get field groups containing only visible, annotated fields and give warnings on duplicated or without annotation fields
     *
     *  @param channelInfoClass
     * @param annotatedFields a set containing annotated fields
     * @param hiddenFields a set containing fields that mark as hidden
     */
    private List<FieldGroupInfo> getValidFieldGroups(final Class<? extends ChannelInfo> channelInfoClass,
                                                     final Set<String> annotatedFields, final Set<String> hiddenFields) {
        final ChannelInfoClassInfo channelInfoClassInfo = InformationObjectsBuilder.buildChannelInfoClassInfo(channelInfoClass);
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

    private List<HstPropertyDefinition> getHstPropertyDefinitions(final String channelId) {
        final String currentHostGroupName = getCurrentVirtualHost().getHostGroupName();
        return getAllVirtualHosts().getPropertyDefinitions(currentHostGroupName, channelId);
    }

    private String getChannelLockedBy(final String channelId) {
        final String hostGroupName = getCurrentVirtualHost().getHostGroupName();
        final String channelPath = getAllVirtualHosts().getChannelById(hostGroupName, channelId).getChannelPath();
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

    private Map<String, String> getLocalizedResources(final String channelId, final String language) throws ChannelException {
        final ResourceBundle resourceBundle = getAllVirtualHosts().getResourceBundle(getChannel(channelId), new Locale(language));
        if (resourceBundle == null) {
            return Collections.EMPTY_MAP;
        }

        return resourceBundle.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), resourceBundle::getString));
    }

    @Override
    public Channel getChannel(final String channelId) throws ChannelException {
        final VirtualHost virtualHost = getCurrentVirtualHost();
        final Channel channel = getAllVirtualHosts().getChannelById(virtualHost.getHostGroupName(), channelId);
        if (channel == null) {
            throw new ChannelNotFoundException(channelId);
        }
        return channel;
    }

    @Override
    public void saveChannel(Session session, final String channelId, Channel channel) throws RepositoryException, IllegalStateException, ChannelException {
        if (!StringUtils.equals(channel.getId(), channelId)) {
            throw new ChannelException("Channel object does not contain the correct id, that should be " + channelId);
        }
        final String currentHostGroupName = getCurrentVirtualHost().getHostGroupName();

        this.channelManager.save(currentHostGroupName, channel);
    }

    @Override
    public List<Channel> getChannels(final boolean previewConfigRequired, final boolean workspaceRequired) {
        final VirtualHost virtualHost = getCurrentVirtualHost();
        return virtualHost.getVirtualHosts().getChannels(virtualHost.getHostGroupName())
                .values()
                .stream()
                .filter(channel -> previewConfigRequiredFiltered(channel, previewConfigRequired))
                .filter(channel -> workspaceFiltered(channel, workspaceRequired))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteChannel(final Session session, final String channelId) throws RepositoryException, ChannelException {
        // TODO Implement validation prior deleting a channel (HSTTWO-3731)

        // TODO Implement logic to delete a channel (HSTTWO-3733)
        throw new ChannelException("Unimplemented Operation");
    }

    private Node getOrAddChannelPropsNode(final Node channelNode) throws RepositoryException {
        if (!channelNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
            return channelNode.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        } else {
            return channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
        }
    }

    private VirtualHost getCurrentVirtualHost() {
        return RequestContextProvider.get().getResolvedMount().getMount().getVirtualHost();
    }

    private VirtualHosts getAllVirtualHosts() {
        return RequestContextProvider.get().getVirtualHost().getVirtualHosts();
    }

    private boolean previewConfigRequiredFiltered(final Channel channel, final boolean previewConfigRequired) {
        return !previewConfigRequired || channel.isPreview();
    }

    private boolean workspaceFiltered(final Channel channel, final boolean required) throws RuntimeRepositoryException {
        if (!required) {
            return true;
        }
        final Mount mount = getAllVirtualHosts().getMountByIdentifier(channel.getMountId());
        final String workspacePath = mount.getHstSite().getConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
        try {
            return RequestContextProvider.get().getSession().nodeExists(workspacePath);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    public void setChannelManager(final ChannelManager channelManager) {
        this.channelManager = channelManager;
    }
}
