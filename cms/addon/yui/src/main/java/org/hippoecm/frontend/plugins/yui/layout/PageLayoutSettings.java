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

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.IntSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiId;
import org.hippoecm.frontend.plugins.yui.javascript.YuiIdSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

public class PageLayoutSettings extends AjaxSettings {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final YuiIdSetting ROOT_ID = new YuiIdSetting("rootId", new YuiId("doc3"));

    private static final IntSetting HEADER_HEIGHT = new IntSetting("headerHeight");
    private static final IntSetting FOOTER_HEIGHT = new IntSetting("footerHeight");
    private static final IntSetting LEFT_WIDTH = new IntSetting("leftWidth");
    private static final IntSetting RIGHT_WIDTH = new IntSetting("rightWidth");

    private static final StringSetting HEADER_GUTTER = new StringSetting("headerGutter");
    private static final StringSetting BODY_GUTTER = new StringSetting("bodyGutter");
    private static final StringSetting FOOTER_GUTTER = new StringSetting("footerGutter");
    private static final StringSetting LEFT_GUTTER = new StringSetting("leftGutter");
    private static final StringSetting RIGHT_GUTTER = new StringSetting("rightGutter");

    private static final BooleanSetting BODY_SCROLL = new BooleanSetting("bodyScroll");
    private static final BooleanSetting HEADER_SCROLL = new BooleanSetting("headerScroll");
    private static final BooleanSetting FOOTER_SCROLL = new BooleanSetting("footerScroll");
    private static final BooleanSetting LEFT_SCROLL = new BooleanSetting("leftScroll");
    private static final BooleanSetting RIGHT_SCROLL = new BooleanSetting("rightScroll");

    private static final BooleanSetting HEADER_RESIZE = new BooleanSetting("headerResize");
    private static final BooleanSetting FOOTER_RESIZE = new BooleanSetting("footerResize");
    private static final BooleanSetting LEFT_RESIZE = new BooleanSetting("leftResize");
    private static final BooleanSetting RIGHT_RESIZE = new BooleanSetting("rightResize");

    
    protected static final YuiType TYPE = new YuiType(AjaxSettings.TYPE, ROOT_ID, 
            HEADER_HEIGHT, FOOTER_HEIGHT, LEFT_WIDTH, RIGHT_WIDTH, 
            HEADER_GUTTER, BODY_GUTTER, FOOTER_GUTTER, LEFT_GUTTER, RIGHT_GUTTER,
            HEADER_SCROLL, FOOTER_SCROLL, LEFT_SCROLL, RIGHT_SCROLL, BODY_SCROLL,
            HEADER_RESIZE, FOOTER_RESIZE, LEFT_RESIZE, RIGHT_RESIZE);

    public PageLayoutSettings(IPluginConfig config) {
        super(TYPE, config);
    }

    public YuiId getRootId() {
        return ROOT_ID.get(this);
    }

}
