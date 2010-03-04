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
package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.IntSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiId;
import org.hippoecm.frontend.plugins.yui.javascript.YuiIdSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;
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
 *   BODY: a {@link YuiId} value representing the unit's body element.
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
 *   Z_INDEX: z-index for this unit. TODO: check if this works. 
 *   </li>
 * </ul>
 */
public class UnitSettings extends YuiObject {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UnitSettings.class);

    private static final StringSetting POSITION = new StringSetting("position");
    private static final YuiIdSetting ID = new YuiIdSetting("id");
    private static final YuiIdSetting BODY = new YuiIdSetting("body");

    private static final StringSetting WIDTH = new StringSetting("width");
    private static final StringSetting HEIGHT = new StringSetting("height");

    private static final StringSetting MIN_WIDTH = new StringSetting("minWidth");
    private static final StringSetting MIN_HEIGHT = new StringSetting("minHeight");

    private static final StringSetting GUTTER = new StringSetting("gutter");
    private static final BooleanSetting SCROLL = new BooleanSetting("scroll");
    private static final BooleanSetting RESIZE = new BooleanSetting("resize");
    private static final BooleanSetting USE_SHIM = new BooleanSetting("useShim");
    
    //TODO: does this still work?
    private static final IntSetting Z_INDEX = new IntSetting("zindex");

    protected final static YuiType TYPE = new YuiType(POSITION, ID, BODY, WIDTH, HEIGHT, MIN_WIDTH, MIN_HEIGHT, GUTTER,
            SCROLL, RESIZE, Z_INDEX, USE_SHIM);

    public static final String TOP = "top";
    public static final String RIGHT = "right";
    public static final String BOTTOM = "bottom";
    public static final String LEFT = "left";
    public static final String CENTER = "center";

    private String wrapperId;
    private String markupId;

    public UnitSettings(String position) {
        super(TYPE);
        POSITION.set(position, this);
    }

    public UnitSettings(String position, IValueMap options) {
        super(TYPE);
        POSITION.set(position, this);
        updateValues(options);
    }

    public UnitSettings(IPluginConfig config) {
        super(TYPE, config);
    }

    public void setPosition(String position, YuiObject settings) {
        POSITION.set(position, settings);
    }

    public String getPosition() {
        return POSITION.get(this);
    }

    public String getWidth() {
        return WIDTH.get(this);
    }

    public void setWidth(String width) {
        WIDTH.set(width, this);
    }

    public String getHeight() {
        return HEIGHT.get(this);
    }

    public void setHeight(String height) {
        HEIGHT.set(height, this);
    }

    public String getMinWidth() {
        return MIN_WIDTH.get(this);
    }

    public void setMinWidth(String minWidth) {
        MIN_WIDTH.set(minWidth, this);
    }

    public String getMinHeight() {
        return MIN_HEIGHT.get(this);
    }

    public void setMinHeight(String minHeight) {
        MIN_HEIGHT.set(minHeight, this);
    }

    public String getElementId() {
        return ID.get(this).getElementId();
    }

    public String getWrapperId() {
        return wrapperId;
    }

    public void setWrapperId(String id) {
        wrapperId = id;
        notifyListeners();
    }

    public String getMarkupId() {
        return markupId;
    }

    public String getBody() {
        return BODY.getScriptValue(BODY.get(this));
    }

    public void setMarkupId(String markupId) {
        this.markupId = markupId;
        if (wrapperId != null) {
            if (BODY.get(this) != null) {
                BODY.get(this).setId(markupId);
            }
        }
        notifyListeners();
    }

    @Override
    public boolean isValid() {
        return ID.get(this) != null;
    }

    public void setParentMarkupId(String parentMarkupId) {
        if (wrapperId != null) {
            YuiId id = ID.get(this);
            if (id == null) {
                ID.set(new YuiId(wrapperId), this);
                id = ID.get(this);
            } else {
                id.setId(wrapperId);
            }
            id.setParentId(parentMarkupId);

            YuiId body = BODY.get(this);
            if (body == null) {
                BODY.set(new YuiId(markupId), this);
            } else {
                body.setId(markupId);
                body.setParentId(null);
            }
        } else if (ID.get(this) != null) {
            YuiId id = ID.get(this);
            id.setParentId(parentMarkupId);

            YuiId body = BODY.get(this);
            if (body != null) {
                body.setParentId(parentMarkupId);
            }
        }
        notifyListeners();
    }

}
