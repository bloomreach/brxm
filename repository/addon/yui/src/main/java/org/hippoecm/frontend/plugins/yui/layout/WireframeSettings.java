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

import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.IYuiListener;
import org.hippoecm.frontend.plugins.yui.javascript.SettingsArraySetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringArraySetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiId;
import org.hippoecm.frontend.plugins.yui.javascript.YuiIdSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

/**
 * Contains all settings of a wireframe and it's units.
 * 
 * <ul>
 *   <li>
 *   ROOT_ID: a {@link YuiId} value representing the wireframe's root element.
 *   </li>
 *   <li>
 *   PARENT_ID: a {@link YuiId} value representing the parent wireframe (value will be auto-set by the {@link 
 *   WireframeBehavior} when LINKED_WITH_PARENT is true. 
 *   </li>
 *   <li>
 *   LINKED_WITH_PARENT: when set to true, the wireframe will register itself with it's parent wireframe and sync render 
 *   and resize events. 
 *   </li>
 *   <li>
 *   UNITS: Array of {@UnitSettings} representing the units for this wireframe. These can either be set by the same 
 *   IPluginConfig object as the {@link WireframeSettings} or by merging the {@UnitSettings} of dynamically found 
 *   {@link UnitBehavior}s.<br/>
 *   The easiest way to configure units is storing the html and configuration all in the same place as the wireframe's 
 *   html and configuration. If we use an {@link IPluginConfig} for configuration, we can add a StringArray property
 *   and add the positional name of the units we'd like to add: for example "top" and "center". This will cause the 
 *   {@link WireframeSettings} to lookup two String properties on the IPluginConfig named "top" and "center" which 
 *   should contain a comma-separated string of UnitSettings, like "id=center,scroll=true,width=120". 
 *   </li>
 *   <li>
 *   WRAPPERS: TODO
 *   </li>
 * </ul>
 * 
 * Warning: this class currently extends the YuiObject which is deprecated and will soon be replaced by a POJO settings
 * file. Not sure how to handle default values and things like dont-escape-string yet.
 */
public class WireframeSettings extends YuiObject {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final YuiIdSetting ROOT_ID = new YuiIdSetting("rootId", new YuiId(""));
    private static final YuiIdSetting PARENT_ID = new YuiIdSetting("parentId", new YuiId(""));
    private static final BooleanSetting LINKED_WITH_PARENT = new BooleanSetting("linkedWithParent", false);

    private static final UnitSettingsArraySetting UNITS = new UnitSettingsArraySetting("units");
    private static final WrappersArraySetting WRAPPERS = new WrappersArraySetting("wrappers", new String[5]);

    protected static final YuiType TYPE = new YuiType(ROOT_ID, PARENT_ID, LINKED_WITH_PARENT, UNITS, WRAPPERS);

    private IYuiListener unitListener = new IYuiListener() {
        private static final long serialVersionUID = 1L;

        public void onEvent(IYuiListener.Event event) {
            if (!active) {
                notifyListeners();
            }
        };
    };

    private String clientClassName;
    private String markupId;
    private transient boolean active = false;

    public WireframeSettings(IPluginConfig config) {
        super(TYPE, config);
        clientClassName = config.getString("client.class.name", "YAHOO.hippo.Wireframe");
    }

    public String getClientClassName() {
        return clientClassName;
    }

    public void setMarkupId(String markupId) {
        this.markupId = markupId;

        // don't send any notifications
        active = true;
        try {
            YuiId root = getRootElementId();
            root.setParentId(markupId);
            for (UnitSettings us : UNITS.get(this)) {
                us.setParentMarkupId(markupId);
            }
        } finally {
            active = false;
        }
        notifyListeners();
    }

    public YuiId getRootElementId() {
        return ROOT_ID.get(this);
    }

    public void setParentId(YuiId id) {
        PARENT_ID.set(id, this);
    }

    public boolean isLinkedWithParent() {
        return LINKED_WITH_PARENT.get(this);
    }

    public UnitSettings getUnitSettingsByPosition(String position) {
        return UNITS.getByPosition(position, this);
    }

    public void register(UnitSettings newSettings) {
        UnitSettings current = UNITS.getByPosition(newSettings.getPosition(), this);
        if (current != null) {
            current.removeListener(unitListener);
        }

        UNITS.setByPosition(newSettings, this);
        newSettings.setWrapperId(current.getWrapperId());
        newSettings.setParentMarkupId(markupId);

        if (newSettings != null) {
            newSettings.addListener(unitListener);
        }
        notifyListeners();
    }

    static class UnitSettingsArraySetting extends SettingsArraySetting<UnitSettings> {
        private static final long serialVersionUID = 1L;

        public UnitSettingsArraySetting(String javascriptKey) {
            super(javascriptKey, null);
        }

        @Override
        public UnitSettings[] newValue() {
            return new UnitSettings[] { new UnitSettings(UnitSettings.TOP), new UnitSettings(UnitSettings.BOTTOM),
                    new UnitSettings(UnitSettings.LEFT), new UnitSettings(UnitSettings.RIGHT),
                    new UnitSettings(UnitSettings.CENTER) };
        }

        @Override
        protected UnitSettings[] getValueFromConfig(IPluginConfig config, YuiObject settings) {
            String[] ids = config.getStringArray(getKey());
            UnitSettings[] unitSettings = get(settings);
            for (String id : ids) {
                UnitSettings us = getByPosition(id, settings);
                us.updateValues(new ValueMap(config.getString(id)));
            }
            return unitSettings;
        }

        protected UnitSettings getByPosition(String position, YuiObject settings) {
            UnitSettings[] unitSettings = get(settings);
            for (UnitSettings setting : unitSettings) {
                if (setting.getPosition().equals(position)) {
                    return setting;
                }
            }
            throw new IllegalArgumentException("No UnitSettings found for position " + position);
        }

        protected void setByPosition(UnitSettings us, WireframeSettings ws) {
            UnitSettings[] usAr = get(ws);
            for (int i = 0; i < usAr.length; i++) {
                if (usAr[i].getPosition().equals(us.getPosition())) {
                    usAr[i] = us;
                    break;
                }
            }
        }

        public void setFromString(String value, YuiObject settings) {
            // TODO:implement
            System.out.println("WWWAAARRRNNNIIINNNGGG!!!: SHOULDN'T SEE THIS!");
        }

    }

    static class WrappersArraySetting extends StringArraySetting {
        private static final long serialVersionUID = 1L;

        public WrappersArraySetting(String javascriptKey, String[] defaultValue) {
            super(javascriptKey, defaultValue);
        }

        @Override
        public boolean isValid(String[] value) {
            return false;
        }

        @Override
        protected String[] getValueFromConfig(IPluginConfig config, YuiObject settings) {
            String[] ids = config.getStringArray(getKey());
            WireframeSettings ws = (WireframeSettings) settings;
            for (String id : ids) {
                UnitSettings setting = ws.getUnitSettingsByPosition(id);
                setting.setWrapperId(config.getString(id));
            }
            return null;
        }
    }
}
