/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.template;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class ItemDescriptor implements IClusterable {
    private static final long serialVersionUID = 1L;

    private PluginDescriptor plugin;

    private int id;
    private String type;
    private String field;

    private LinkedList<ItemDescriptor> items;

    public ItemDescriptor(int id, PluginDescriptor plugin) {
        this.id = id;
        this.plugin = plugin;
        this.type = this.field = null;
        this.items = new LinkedList<ItemDescriptor>();
    }

    public ItemDescriptor(Map<String, Object> map) {
        this.id = ((Integer) map.get("id")).intValue();
        this.type = (String) map.get("type");
        this.field = (String) map.get("field");

        this.items = new LinkedList<ItemDescriptor>();
        LinkedList<Map<String, Object>> itemList = (LinkedList<Map<String, Object>>) map.get("items");
        for (Map<String, Object> itemMap : itemList) {
            items.addLast(new ItemDescriptor(itemMap));
        }

        Map<String, Object> pluginMap = (Map<String, Object>) map.get("plugin");
        if (pluginMap != null) {
            this.plugin = new PluginDescriptor(pluginMap);
        }
    }

    public Map<String, Object> getMapRepresentation() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", new Integer(getId()));
        map.put("type", getType());
        map.put("field", getField());

        LinkedList<Map<String, Object>> itemList = new LinkedList<Map<String, Object>>();
        for (ItemDescriptor item : getItems()) {
            itemList.addLast(item.getMapRepresentation());
        }
        map.put("items", itemList);

        PluginDescriptor plugin = getPlugin();
        if (plugin != null) {
            map.put("plugin", plugin.getMapRepresentation());
        }
        return map;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("type", getType()).append("field",
                getField()).toString();
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
        return new EqualsBuilder().append(getType(), itemDescriptor.getType()).append(getField(),
                itemDescriptor.getField()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(421, 23).append(getType()).append(getField()).toHashCode();
    }
}
