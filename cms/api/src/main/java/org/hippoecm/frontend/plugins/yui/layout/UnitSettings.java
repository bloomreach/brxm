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

import java.io.Serializable;

import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains all settings of a YUI unit.
 * 
 * <ul>
 *   <li>
 *   POSITION: String representation of the position this unit has in the wireframe; top, right, bottom, left or center
 *   </li>
 *   <li>
 *   ID: a {@link YuiId} value representing the unit's root element.
 *   </li>
 *   <li>
 *   BODY: a {@link YuiId} value representing the unit's body element.  The body should not be configured when the unit
 *      wraps a {@link Component} with a {@link UnitBehavior}.  When neither body nor UnitBehavior is present, a body
 *      element will be generated.
 *   </li>
 *   <li>
 *   WIDTH: width (in pixels) that the unit will take up in the wireframe (only applies to left and right units and 
 *   is required)
 *   </li>
 *   <li>
 *   HEIGHT: height (in pixels) that the unit will take up in the wireframe (only applies to top and bottom units 
 *   and is required).
 *   </li>
 *   <li>
 *   MIN_WIDTH: minimum width (in pixels) the unit will take up in the wireframe (only applies to left and right units 
 *   and is required)
 *   </li>
 *   <li>
 *   MIN_HEIGHT: minimum height (in pixels) that the unit will take up in the wireframe (only applies to top and bottom 
 *   units and is required).
 *   </li>
 *   <li>
 *   GUTTER: the gutter applied to the unit's wrapper, before the content, for example "5px".
 *   </li>
 *   <li>
 *   SCROLL: Boolean indicating whether the unit's body should have scroll bars if the body content is larger than the 
 *   display area.
 *   </li>
 *   <li>
 *   RESIZE: Boolean indicating whether this unit is resizeable.
 *   </li>
 *   <li>
 *   USE_SHIM: Sometimes resizing a unit over an iframe (or other elements) will lead to unexpected behavior, especially
 *   in everyone's favorite browser IE. Set this property to true to fix it. 
 *   </li>
 *   <li>
 *   Z_INDEX: z-index for this unit. 
 *   </li>
 *   <li>
 *   EXPANDED: whether or not this unit is expanded, meaning it will take up all available space inside the wireframe, hiding
 *   all other active units.
 *   </li>
 * </ul>
 */
public class UnitSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UnitSettings.class);

    private String position = "center";
    private YuiId id = new YuiId("");
    private YuiId body = new YuiId("");

    private String width;
    private String height;

    private String minWidth;
    private String minHeight;

    private String gutter = "0px 0px 0px 0px";
    private boolean scroll = false;
    private boolean resize = false;
    private boolean useShim = false;

    private int zIndex = 0;

    private boolean expandCollapseEnabled = false;
    private boolean expanded = false;
    
    public static final String TOP = "top";
    public static final String RIGHT = "right";
    public static final String BOTTOM = "bottom";
    public static final String LEFT = "left";
    public static final String CENTER = "center";

    public UnitSettings(String position, IValueMap config) {
        this.position = position;
        if (config != null) {
            try {
                PluginConfigMapper.populate(this, config);
            } catch (MappingException e) {
                throw new RuntimeException("invalid configuration");
            }
            id.setId(config.getString("id"));
            if (config.containsKey("body")) {
                body.setId(config.getString("body"));
            }
        }
    }

    public String getPosition() {
        return position;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getWidth() {
        return width;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height;
    }

    public void setMinWidth(String minWidth) {
        this.minWidth = minWidth;
    }

    public String getMinWidth() {
        return minWidth;
    }

    public void setMinHeight(String minHeight) {
        this.minHeight = minHeight;
    }

    public String getMinHeight() {
        return minHeight;
    }

    public void setGutter(String gutter) {
        this.gutter = gutter;
    }

    public String getGutter() {
        return gutter;
    }

    public void setScroll(boolean scroll) {
        this.scroll = scroll;
    }

    public boolean isScroll() {
        return scroll;
    }

    public void setResize(boolean resize) {
        this.resize = resize;
    }

    public boolean isResize() {
        return resize;
    }

    public void setUseShim(boolean useShim) {
        this.useShim = useShim;
    }

    public boolean isUseShim() {
        return useShim;
    }

    public YuiId getBody() {
        return body;
    }

    public YuiId getId() {
        return id;
    }

    public void setZindex(int zIndex) {
        this.zIndex = zIndex;
    }

    public int getZindex() {
        return zIndex;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpandCollapseEnabled() {
        return expandCollapseEnabled;
    }

    public void setExpandCollapseEnabled(boolean expandCollapseEnabled) {
        this.expandCollapseEnabled = expandCollapseEnabled;
    }
}
