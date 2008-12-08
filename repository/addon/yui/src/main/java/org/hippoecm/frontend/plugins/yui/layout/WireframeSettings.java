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
import org.hippoecm.frontend.plugins.yui.javascript.Settings;
import org.hippoecm.frontend.plugins.yui.javascript.SettingsArraySetting;
import org.hippoecm.frontend.plugins.yui.javascript.SettingsArrayValue;
import org.hippoecm.frontend.plugins.yui.javascript.StringArraySetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.Value;

public class WireframeSettings extends Settings {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final StringSetting ROOT_ID = new StringSetting("rootId", "");
    private static final StringSetting PARENT_ID = new StringSetting("parentId", "");
    private static final BooleanSetting LINKED_WITH_PARENT = new BooleanSetting("linkedWithParent", false);

    private static final StringSetting CLIENT_CLASS_NAME = new StringSetting("clientClassName",
            "YAHOO.hippo.Wireframe", false);

    private static final UnitSettingsArraySetting UNITS = new UnitSettingsArraySetting("units");
    private static final WrappersArraySetting WRAPPERS = new WrappersArraySetting("wrappers", new String[5]);

    private String markupId;

    public WireframeSettings(IPluginConfig config) {
        super(config);
    }

    protected void initValues() {
        add(ROOT_ID, PARENT_ID, LINKED_WITH_PARENT, CLIENT_CLASS_NAME, UNITS, WRAPPERS);
        skip(WRAPPERS);
    }

    public void setMarkupId(String markupId) {
        this.markupId = markupId;
    }

    public String getRootElementId() {
        return ROOT_ID.get(this);
    }

    public String getClientClassName() {
        return CLIENT_CLASS_NAME.get(this);
    }

    public void setClientClassName(String value) {
        CLIENT_CLASS_NAME.set(value, this);
    }

    public void setParentId(String id) {
        PARENT_ID.set(id, this);
    }

    public boolean isLinkedWithParent() {
        return LINKED_WITH_PARENT.get(this);
    }

    public UnitSettings getUnitSettingsByPosition(String position) {
        return UNITS.getByPosition(position, this);
    }

    public void register(UnitSettings newSettings) {
        UNITS.setByPosition(newSettings, this);
    }

    protected void enhanceIds() {
        String rootId = getRootElementId();
        if (rootId == null || rootId.equals("")) {
            rootId = markupId;
        } else {
            rootId = markupId + ':' + rootId;
        }
        ROOT_ID.set(rootId, this);

        for (UnitSettings us : UNITS.get(this)) {
            us.enhanceIds(markupId);
        }
    }

    static class UnitSettingsArraySetting extends SettingsArraySetting<UnitSettings> {
        private static final long serialVersionUID = 1L;

        public UnitSettingsArraySetting(String javascriptKey) {
            super(javascriptKey, null);
        }

        @Override
        public Value<UnitSettings[]> newValue() {
            return new SettingsArrayValue<UnitSettings>( new UnitSettings[] { 
                new UnitSettings(UnitSettings.TOP),
                new UnitSettings(UnitSettings.BOTTOM), 
                new UnitSettings(UnitSettings.LEFT),
                new UnitSettings(UnitSettings.RIGHT), 
                new UnitSettings(UnitSettings.CENTER) 
            });
        }

        @Override
        protected UnitSettings[] getValueFromConfig(IPluginConfig config, Settings settings) {
            String[] ids = config.getStringArray(getKey());
            UnitSettings[] unitSettings = get(settings);
            for (String id : ids) {
                UnitSettings us = getByPosition(id, settings);
                us.updateValues(new ValueMap(config.getString(id)));
            }
            return unitSettings;
        }

        protected UnitSettings getByPosition(String position, Settings settings) {
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
                    us.setWrapperId(usAr[i].getWrapperId()); //only value that can be set before registering
                    usAr[i] = us;
                    break;
                }
            }
        }

        public void setFromString(String value, Settings settings) {
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
        protected String[] getValueFromConfig(IPluginConfig config, Settings settings) {
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
