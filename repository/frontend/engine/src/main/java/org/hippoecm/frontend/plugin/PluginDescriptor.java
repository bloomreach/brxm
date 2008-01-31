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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.channel.Channel;

public class PluginDescriptor implements IClusterable, Cloneable {
    private static final long serialVersionUID = 1L;	

    private String pluginId;
    private String wicketId;
    private String className;
    private Map<String, List<String>> parameters;

    private Channel incoming;
    private Channel outgoing;

    public PluginDescriptor(String pluginId, String className, Channel outgoing) {
        this.pluginId = pluginId;
        this.wicketId = pluginId;
        this.className = className;
        this.outgoing = outgoing;
        parameters = new HashMap<String, List<String>>();
    }

    public PluginDescriptor(Map<String, Object> map, Channel bottom) {
        this.pluginId = (String) map.get("pluginId");
        this.wicketId = (String) map.get("wicketId");
        this.className = (String) map.get("className");
        this.parameters = (Map<String, List<String>>) map.get("parameters");
        this.outgoing = bottom;
    }

    public PluginDescriptor clone() {
        try {
            return (PluginDescriptor) super.clone();
        } catch (CloneNotSupportedException ex) {
            // cannot occur
        }
        return null;
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("pluginId", pluginId);
        map.put("wicketId", wicketId);
        map.put("className", className);
        map.put("parameters", parameters);
        return map;
    }

    // setters

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public void setWicketId(String wicketId) {
        this.wicketId = wicketId;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void addParameter(String key, List<String> value) {
        parameters.put(key, value);
    }

    public List<String> getParameter(String key) {
        return parameters.get(key);
    }

    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    public void connect(Channel incoming) {
        this.incoming = incoming;
    }

    public void disconnect() {
        incoming = null;
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

    public Channel getIncoming() {
        return incoming;
    }

    public Channel getOutgoing() {
        return outgoing;
    }

    public void setOutgoing(Channel channel) {
        outgoing = channel;
    }

    public List<PluginDescriptor> getChildren() {
        return new ArrayList<PluginDescriptor>();
    }

    // override Object methods

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
