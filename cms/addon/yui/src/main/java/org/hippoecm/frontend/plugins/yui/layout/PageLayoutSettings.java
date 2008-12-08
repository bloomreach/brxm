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
import org.hippoecm.frontend.plugins.yui.javascript.IntSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;

public class PageLayoutSettings extends AjaxSettings {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    private static final StringSetting ROOT_ID  = new StringSetting("rootId", "doc3");

    private static final IntSetting HEADER_HEIGHT = new IntSetting("headerHeight");
    private static final IntSetting FOOTER_HEIGHT = new IntSetting("headerHeight");

    private static final StringSetting HEADER_GUTTER = new StringSetting("headerGutter");
    private static final StringSetting BODY_GUTTER = new StringSetting("bodyGutter");
    private static final StringSetting FOOTER_GUTTER = new StringSetting("footerGutter");

    public PageLayoutSettings(IPluginConfig config) {
        super(config);
    }

    @Override
    protected void initValues() {
        super.initValues();
        add(ROOT_ID, HEADER_HEIGHT, FOOTER_HEIGHT, HEADER_GUTTER, BODY_GUTTER, FOOTER_GUTTER);
    }

    public String getRootId() {
        return ROOT_ID.get(this);
    }
    
}
