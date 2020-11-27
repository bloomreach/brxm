/*
 * Copyright 2020 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.onehippo.cms7.services.hst.Channel;

public class ChannelContext {

    private Channel channel;
    private String channelId;
    private boolean channelAdmin;
    private boolean channelWebmaste;
    private boolean deletable;
    private boolean configurationLocked;
    private boolean hasCustomProperties;
    private boolean crossChannelPageCopySupported;
    private boolean workspaceExists;
    private boolean hasPrototypes;
    private Set<String> changeBySet;
    private Set<XPageLayout> xPageLayouts;
    private Map<String, String> xPageTemplateQueries;

    public String getChannelId() {
        return channelId;
    }

    public boolean isChannelAdmin() {
        return channelAdmin;
    }

    public boolean isChannelWebmaster() {
        return channelWebmaste;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public boolean isConfigurationLocked() {
        return configurationLocked;
    }

    public boolean hasCustomProperties() {
        return hasCustomProperties;
    }

    public Set<String> getChangedBySet() {
        return changeBySet;
    }

    public boolean isCrossChannelPageCopySupported() {
        return crossChannelPageCopySupported;
    }

    public boolean hasWorkspace() {
        return workspaceExists;
    }

    public boolean hasPrototypes() {
        return hasPrototypes;
    }

    public Set<XPageLayout> getXPageLayouts() {
        return xPageLayouts;
    }

    public Map<String, String> getXPageTemplateQueries() {
        return xPageTemplateQueries;
    }

    ChannelContext setChannelAdmin(final boolean channelAdmin) {
        this.channelAdmin = channelAdmin;
        return this;
    }

    public ChannelContext setChannelWebmaster(final boolean channelWebmaste) {
        this.channelWebmaste = channelWebmaste;
        return this;
    }

    ChannelContext setChannel(final Channel channel) {
        this.channel = channel;
        this.channelId = channel.getId();
        if (channelId.endsWith("-preview")) {
            channelId = channelId.substring(0, channelId.lastIndexOf("-preview"));
        }
        this.hasCustomProperties = channel.getHasCustomProperties();
        this.changeBySet = channel.getChangedBySet();
        this.workspaceExists = channel.isWorkspaceExists();
        return this;
    }

    ChannelContext setCrossChannelPageCopySupported(final boolean crossChannelPageCopySupported) {
        this.crossChannelPageCopySupported = crossChannelPageCopySupported;
        return this;
    }

    ChannelContext setHasPrototypes(final boolean hasPrototypes) {
        this.hasPrototypes = hasPrototypes;
        return this;
    }

    ChannelContext setDeletable(final boolean deletable) {
        this.deletable = deletable;
        return this;
    }

    ChannelContext setConfigurationLocked(final boolean configurationLocked) {
        this.configurationLocked = configurationLocked;
        return this;
    }

    public ChannelContext setXPageLayouts(final Set<XPageLayout> layouts) {
        this.xPageLayouts = layouts;
        return this;
    }

    public ChannelContext setXPageTemplateQueries(final Map<String, String> xPageTemplateQueries) {
        this.xPageTemplateQueries = xPageTemplateQueries;
        return this;
    }


}
