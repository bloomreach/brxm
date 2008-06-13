/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.legacy.template;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;

@Deprecated
public class ItemDescriptor implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private PluginDescriptor plugin;

    private int id;
    private String field;

    private String mode;
    private TemplateDescriptor template;
    private LinkedList<ItemDescriptor> items;

    public ItemDescriptor(int id, PluginDescriptor plugin, String mode) {
        this.id = id;
        this.plugin = plugin;
        this.mode = mode;
        this.field = null;
        this.template = null;
        this.items = new LinkedList<ItemDescriptor>();
    }

    public ItemDescriptor(Map<String, Object> map) {
        this.id = ((Integer) map.get("id")).intValue();
        this.field = (String) map.get("field");
        this.mode = (String) map.get("mode");

        this.items = new LinkedList<ItemDescriptor>();
        LinkedList<Map<String, Object>> itemList = (LinkedList<Map<String, Object>>) map.get("items");
        for (Map<String, Object> itemMap : itemList) {
            items.addLast(new ItemDescriptor(itemMap));
        }

        Map<String, Object> pluginMap = (Map<String, Object>) map.get("plugin");
        if (pluginMap != null) {
            this.plugin = new PluginDescriptor(pluginMap);
        }

        Map<String, Object> templateMap = (Map<String, Object>) map.get("template");
        if (templateMap != null) {
            this.template = new TemplateDescriptor(templateMap);
        }
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", new Integer(getId()));
        map.put("field", getField());
        map.put("mode", getMode());

        LinkedList<Map<String, Object>> itemList = new LinkedList<Map<String, Object>>();
        for (ItemDescriptor item : getItems()) {
            itemList.addLast(item.getMapRepresentation());
        }
        map.put("items", itemList);

        PluginDescriptor plugin = getPlugin();
        if (plugin != null) {
            map.put("plugin", plugin.getMapRepresentation());
        }

        TemplateDescriptor template = getTemplate();
        if (template != null) {
            map.put("template", template.getMapRepresentation());
        }
        return map;
    }

    public int getId() {
        return id;
    }

    public String getMode() {
        return mode;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<ItemDescriptor> getItems() {
        return items;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public void setPlugin(PluginDescriptor plugin) {
        this.plugin = plugin;
    }

    public TemplateDescriptor getTemplate() {
        return template;
    }

    public void setTemplate(TemplateDescriptor descriptor) {
        template = descriptor;
        for (ItemDescriptor item : getItems()) {
            item.setTemplate(descriptor);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("template", getTemplate()).append(
                "field", getField()).toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ItemDescriptor == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        ItemDescriptor itemDescriptor = (ItemDescriptor) object;
        return new EqualsBuilder().append(id, itemDescriptor.id).append(getTemplate(), itemDescriptor.getTemplate())
                .append(getField(), itemDescriptor.getField()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(421, 23).append(getTemplate()).append(getField()).toHashCode();
    }
}
