/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.AjaxSettings;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains all settings of a wireframe and it's units.
 * <p/>
 * <ul>
 * <li>
 * ROOT_ID: a {@link YuiId} value representing the wireframe's root element.
 * </li>
 * <li>
 * PARENT_ID: a {@link YuiId} value representing the parent wireframe (value will be auto-set by the {@link
 * WireframeBehavior} when LINKED_WITH_PARENT is true.
 * </li>
 * <li>
 * LINKED_WITH_PARENT: when set to true, the wireframe will register itself with it's parent wireframe and sync render
 * and resize events.
 * </li>
 * <li>
 * UNITS: Array of {@UnitSettings} representing the units for this wireframe. These should be set by the same
 * IPluginConfig object as the {@link WireframeSettings}.<br/>
 * The easiest way to configure units is storing the html and configuration all in the same place as the wireframe's
 * html and configuration. If we use an {@link IPluginConfig} for configuration, we can add a StringArray property
 * and add the positional name of the units we'd like to add: for example "top" and "center". This will cause the
 * {@link WireframeSettings} to lookup two String properties on the IPluginConfig named "top" and "center" which
 * should contain a comma-separated string of UnitSettings, like "id=center,scroll=true,width=120".
 * </li>
 * </ul>
 * <p/>
 * Warning: this class currently extends the YuiObject which is deprecated and will soon be replaced by a POJO settings
 * file. Not sure how to handle default values and things like dont-escape-string yet.
 */
public class WireframeSettings extends AjaxSettings {

    private static final long serialVersionUID = 1L;

    private YuiId rootId = new YuiId("");
    private YuiId parentId = new YuiId("");
    private boolean linkedWithParent = true;
    private List<UnitSettings> units;

    private String clientClassName = "YAHOO.hippo.Wireframe";

    private String defaultExpandedUnit;

    public WireframeSettings(IPluginConfig config) {
        units = new ArrayList<UnitSettings>(5);
        for (String position : new String[]{"top", "left", "center", "right", "bottom"}) {
            String unitConfig = config.getString(position);
            if (unitConfig != null) {
                units.add(new UnitSettings(position, new ValueMap(unitConfig)));
            }
        }
        try {
            PluginConfigMapper.populate(this, config);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }
        rootId.setId(config.getString("root.id"));
    }

    public String getClientClassName() {
        return clientClassName;
    }

    public void setClientClassName(String clientClassName) {
        this.clientClassName = clientClassName;
    }

    public void setMarkupId(String markupId) {
        rootId.setParentId(markupId);
        for (UnitSettings unit : units) {
            unit.getId().setParentId(markupId);
            unit.getBody().setParentId(markupId);
        }
    }

    public YuiId getRootId() {
        return rootId;
    }

    public void setParentId(YuiId parentId) {
        this.parentId = parentId;
    }

    public YuiId getParentId() {
        return parentId;
    }

    public void setLinkedWithParent(boolean linkedWithParent) {
        this.linkedWithParent = linkedWithParent;
    }

    public boolean isLinkedWithParent() {
        return linkedWithParent;
    }

    public List<UnitSettings> getUnits() {
        return Collections.unmodifiableList(units);
    }

    public UnitSettings getUnit(String position) {
        for (UnitSettings unit : units) {
            if (unit.getPosition().equals(position)) {
                return unit;
            }
        }
        return null;
    }

    public String getDefaultExpandedUnit() {
        return defaultExpandedUnit;
    }

    public void setDefaultExpandedUnit(String defaultExpandedUnit) {
        this.defaultExpandedUnit = defaultExpandedUnit;
    }

    public boolean hasExpandedUnit() {
        for (UnitSettings unit : units) {
            if (unit.isExpandCollapseEnabled() && unit.isExpanded()) {
                return true;
            }
        }
        return false;
    }
}
