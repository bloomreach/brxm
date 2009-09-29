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

package org.hippoecm.frontend.plugins.yui.datetime;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

public class YuiDatePickerSettings extends YuiObject {
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private final static StringSetting DATE_PATTERN = new StringSetting("datePattern", "d-M-yy");
    private final static BooleanSetting HIDE_ON_SELECT = new BooleanSetting("hideOnSelect", true);
    private final static BooleanSetting ALIGN_WITH_ICON = new BooleanSetting("alignWithIcon", true);
    private final static BooleanSetting FIRE_CHANGE_EVENT = new BooleanSetting("fireChangeEvent", true);
    
    protected static final YuiType TYPE = new YuiType(DATE_PATTERN, HIDE_ON_SELECT, ALIGN_WITH_ICON, FIRE_CHANGE_EVENT);

    public YuiDatePickerSettings(IPluginConfig config) {
        super(TYPE, config);
    }
    
    public void setDatePattern(String pattern) {
        DATE_PATTERN.set(pattern, this);
    }

}
