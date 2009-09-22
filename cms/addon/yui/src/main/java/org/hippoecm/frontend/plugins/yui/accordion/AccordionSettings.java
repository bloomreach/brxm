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

package org.hippoecm.frontend.plugins.yui.accordion;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.IntSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

public class AccordionSettings extends YuiObject {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    //Key value for plugin configuration
    public static final String CONFIG_KEY = "yui.config.accordion";

    //FIXME: remove when new resize/render event listeners are comitted
    //if set resize updates will be throttled
    private static final BooleanSetting THROTTLE_UPDATE = new BooleanSetting("throttleUpdate", true);

    //timeout of throttle
    private static final IntSetting TIMEOUT_LENGTH = new IntSetting("timeoutLength", 200);

    //Classname of the ancestor that should dictate the possible dimensions of the accordion
    private static final StringSetting ANCESTOR_CLASSNAME = new StringSetting("ancestorClassname", "yui-layout-bd"); //default to find the nearest layout

    //Classname of the elements that represent a unit of the accordion
    private static final StringSetting UNIT_CLASSNAME = new StringSetting("unitClassname", "hippo-accordion-unit");

    //Height of a unit header
    private static final IntSetting UNIT_HEADER_HEIGHT = new IntSetting("unitHeaderHeight", 25);

    //if set the available height for the accordion to render in will be calculated by adding
    //the width and height as returned by YAHOO.util.Dom.getRegion(), else a shortcut will be taken
    //by multiplying the number of units with the unitHeaderHeight
    private static final BooleanSetting CALCULATE_TOTAL_HEIGHT = new BooleanSetting("calculateTotalHeight");

    private static final BooleanSetting REGISTER_RESIZE_LISTENER = new BooleanSetting("registerResizeListener", true);
    private static final BooleanSetting REGISTER_RENDER_LISTENER = new BooleanSetting("registerRenderListener", true);

    protected static final YuiType TYPE = new YuiType(THROTTLE_UPDATE, TIMEOUT_LENGTH, ANCESTOR_CLASSNAME,
            UNIT_CLASSNAME, UNIT_HEADER_HEIGHT, CALCULATE_TOTAL_HEIGHT, REGISTER_RESIZE_LISTENER,
            REGISTER_RENDER_LISTENER);

    public AccordionSettings(IPluginConfig config) {
        super(TYPE, config);
    }

}
