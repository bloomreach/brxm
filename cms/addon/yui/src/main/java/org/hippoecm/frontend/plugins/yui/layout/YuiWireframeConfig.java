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

package org.hippoecm.frontend.plugins.yui.layout;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.plugins.yui.util.JavascriptUtil;
import org.hippoecm.frontend.plugins.yui.util.OptionsUtil;

public class YuiWireframeConfig implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String rootElementId;
    private String parentElementId;
    private boolean linkedWithParent = false;

    private Map<String, Unit> units;
    private Map<String, String> unitElements;

    public YuiWireframeConfig(boolean linkedWithParent) {
        this();
        this.linkedWithParent = linkedWithParent;
    }

    public YuiWireframeConfig() {
        units = new MiniMap(5);
        units.put(Unit.TOP, null);
        units.put(Unit.BOTTOM, null);
        units.put(Unit.LEFT, null);
        units.put(Unit.RIGHT, null);
        units.put(Unit.CENTER, null);

        unitElements = new MiniMap(5);
    }

    public void addUnit(String position, String... pairs) {
        addUnit(position, OptionsUtil.keyValuePairsToMap(pairs));
    }

    public void addUnit(String position, Map<String, String> options) {
        if (!units.containsKey(position)) {
            throw new IllegalArgumentException(
                    "Position value '"
                            + position
                            + "' is not a valid position, use constants TOP, BOTTOM, LEFT, RIGHT or CENTER in the YuiLayoutConfiguration.Unit class instead.");
        } else {
            units.put(position, new Unit(options));
        }
    }

    //pass none ajax updatable element as root
    public void registerUnitElement(String position, String elId) {
        unitElements.put(position, elId);
    }

    public String getUnitElement(String position) {
        if (unitElements.containsKey(position))
            return unitElements.get(position);
        return null;
    }

    public void setRootElementId(String id) {
        rootElementId = id;
    }

    public String getRootElementId() {
        return rootElementId;
    }

    public class Unit implements Serializable {
        private static final long serialVersionUID = 1L;

        public final static String TOP = "top";
        public final static String BOTTOM = "bottom";
        public final static String LEFT = "left";
        public final static String RIGHT = "right";
        public final static String CENTER = "center";

        private Map<String, String> options;

        public Unit(Map<String, String> options) {
            if (options == null)
                options = new HashMap<String, String>();
            else
                this.options = options;
        }

        public void addOption(String key, String value) {
            options.put(key, value);
        }

    }

    public Map<String, Object> getMap() {
        Map<String, Object> newMap = new HashMap<String, Object>();
        newMap.put("rootElementId", JavascriptUtil.serialize2JS(rootElementId));
        newMap.put("parentElementId", JavascriptUtil.serialize2JS(parentElementId));
        newMap.put("linkedWithParent", linkedWithParent);

        StringBuilder config = new StringBuilder();
        config.append("{");
        if (units.size() > 0) {
            config.append("units: [");
            for (String unitKey : units.keySet()) {
                Unit unit = units.get(unitKey);
                if (unit != null) {
                    config.append("    { position: '").append(unitKey).append("'");
                    if (unit.options != null) {
                        Map<String, String> options = units.get(unitKey).options;
                        for (String optionsKey : options.keySet()) {
                            config.append(", ").append(optionsKey).append(": ").append(
                                    JavascriptUtil.serialize2JS(options.get(optionsKey)));
                        }
                    }
                    config.append("}");
                    if (!unitKey.equals(Unit.CENTER))
                        config.append(",");

                }
            }
            config.append("]");
        }
        config.append("}");
        newMap.put("config", config.toString());
        return newMap;
    }

    public String getParentId() {
        return parentElementId;
    }

    public void setParentId(String parentId) {
        this.parentElementId = parentId;
    }

    public boolean isLinkedWithParent() {
        return linkedWithParent;
    }

}
