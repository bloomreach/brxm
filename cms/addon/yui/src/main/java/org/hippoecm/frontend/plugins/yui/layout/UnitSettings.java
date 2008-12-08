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

import java.util.Map;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.Settings;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;

public class UnitSettings extends Settings {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    private static final StringSetting POSITION = new StringSetting("position");
    private static final StringSetting ID = new StringSetting("id");
    private static final StringSetting BODY = new StringSetting("body");    
    
    private static final StringSetting WIDTH = new StringSetting("width");
    private static final StringSetting HEIGHT = new StringSetting("height");

    private static final StringSetting GUTTER = new StringSetting("gutter");
    private static final BooleanSetting SCROLL = new BooleanSetting("scroll");
    
    public static final String TOP = "top";
    public static final String RIGHT = "right";
    public static final String BOTTOM = "bottom";
    public static final String LEFT = "left";
    public static final String CENTER = "center";
    
    
    private String wrapperId;
    private String markupId;
    
    public UnitSettings(String position) {
        POSITION.set(position, this);
    }

    public UnitSettings(String position, Map<String, String> options) {
        POSITION.set(position, this);
        updateValues(options);
    }

    public UnitSettings(IPluginConfig config) {
        super(config);
    }
    
    @Override
    protected void initValues() {
        add(POSITION, ID, BODY, WIDTH, HEIGHT, GUTTER, SCROLL);
    }
    
    public void setId(String value) {
        ID.set(value, this);
    }
    
    public String getId() {
        return ID.get(this);
    }

    public void setBody(String value) {
        BODY.set(value, this);
    }

    public String getBody() {
        return BODY.get(this);
    }

    public void setPosition(String position, Settings settings) {
        POSITION.set(position, settings);
    }
    
    public String getPosition() {
        return POSITION.get(this);
    }
    
    public void setWidth(String width) {
        WIDTH.set(width, this);
    }
    
    public void setHeight(String height) {
        HEIGHT.set(height, this);
    }
    
    public void setWrapperId(String id) {
        wrapperId = id;
    }

    public String getWrapperId() {
        return wrapperId;
    }
    
    public void setMarkupId(String markupId) {
        this.markupId = markupId;
    }

    public void enhanceIds(String parentMarkupId) {
        if(wrapperId != null) {
            setId(parentMarkupId + ":" + wrapperId);
            setBody(markupId);
        } else if(getId() != null) {
            setId(parentMarkupId + ":" + getId());
            if(getBody() != null) {
                setBody(parentMarkupId + ":" + getBody());
            }
        }
    }
    
    @Override
    public boolean isValid() {
        return ID.get(this) != null;
    }

}
