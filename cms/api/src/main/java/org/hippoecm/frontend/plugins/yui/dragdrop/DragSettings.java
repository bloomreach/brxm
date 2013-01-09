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

package org.hippoecm.frontend.plugins.yui.dragdrop;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

public class DragSettings extends DragDropSettings {

    private static final long serialVersionUID = 1L;

    private static final BooleanSetting CENTER_FRAME = new BooleanSetting("centerFrame", true);
    private static final BooleanSetting RESIZE_FRAME = new BooleanSetting("resizeFrame", false);
    private static final StringSetting LABEL = new StringSetting("label", null);

    private static final StringSetting IE_FALLBACK_CLASS = new StringSetting("ieFallbackClass", "ie-fallback-marker");
    private static final StringSetting WRAPPED_MODEL_CLASS = new StringSetting("wrappedModelClass",
            "YAHOO.hippo.DDModel", false);
    private static final StringSetting FIRST_ANCESTOR_TO_BLUR = new StringSetting("firstAncestorToBlur", "");

    private static final YuiType TYPE = new YuiType(DragDropSettings.TYPE, CENTER_FRAME, RESIZE_FRAME, LABEL,
            IE_FALLBACK_CLASS, WRAPPED_MODEL_CLASS, FIRST_ANCESTOR_TO_BLUR);

    public DragSettings(IPluginConfig config) {
        super(TYPE, config);
    }

    public DragSettings setCenterFrame(boolean center) {
        CENTER_FRAME.set(center, this);
        return this;
    }

    public DragSettings setResizeFrame(boolean resize) {
        RESIZE_FRAME.set(resize, this);
        return this;
    }

    public DragSettings setLabel(String label) {
        LABEL.set(label, this);
        return this;
    }

    public DragSettings setIeFallbackClass(String className) {
        IE_FALLBACK_CLASS.set(className, this);
        return this;
    }

    public DragSettings setWrappedModelClass(String string) {
        WRAPPED_MODEL_CLASS.set(string, this);
        return this;
    }

    public DragSettings setFirstAncestorToBlur(String string) {
        FIRST_ANCESTOR_TO_BLUR.set(string, this);
        return this;
    }

}
