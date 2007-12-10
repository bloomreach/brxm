/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin;

import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;

public class PluginDescriptor implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String pluginId;
    private String wicketId;
    private String className;

    private Set<EventChannel> incoming;
    private Set<EventChannel> outgoing;

    public PluginDescriptor(String pluginId, String className, Set<EventChannel> incoming, Set<EventChannel> outgoing) {
        this.pluginId = pluginId;
        this.wicketId = pluginId;
        this.className = className;
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    // setters

    public void setWicketId(String wicketId) {
        this.wicketId = wicketId;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    // getters

    public String getPluginId() {
        return pluginId;
    }

    public String getWicketId() {
        return wicketId;
    }

    public String getClassName() {
        return className;
    }

    public Set<EventChannel> getIncoming() {
        return incoming;
    }

    public Set<EventChannel> getOutgoing() {
        return outgoing;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("pluginId", pluginId).append(
                "wicketId", wicketId).append("className", className).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof PluginDescriptor == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        PluginDescriptor pluginDescriptor = (PluginDescriptor) object;
        return new EqualsBuilder().append(pluginId, pluginDescriptor.pluginId).append(wicketId,
                pluginDescriptor.wicketId).append(className, pluginDescriptor.className).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 313).append(pluginId).append(wicketId).append(className).toHashCode();
    }

}
