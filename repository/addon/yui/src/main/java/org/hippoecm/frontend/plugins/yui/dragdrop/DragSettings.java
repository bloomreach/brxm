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

package org.hippoecm.frontend.plugins.yui.dragdrop;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class DragSettings extends DragDropSettings {
    private static final long serialVersionUID = 1L;
    
    private static final String CENTER_FRAME = "centerFrame"; //boolean
    private static final String RESIZE_FRAME = "resizeFrame"; //boolean
    private static final String LABEL = "label"; //String
    private static final String IE_FALLBACK_CLASS_NAME = "ieFallbackClassName"; //String
    private static final String WRAPPED_MODEL_CLASS = "wrappedModelClass"; //String
    private static final String FIRST_ANCESTOR_TO_BLUR = "firstAncestorToBlur"; //String (optional)
    
    private static final String  DEFAULT_IE_FALLBACK_CLASS_NAME = "ie-fallback-marker";
    private static final boolean DEFAULT_CENTER_FRAME = true;
    private static final boolean DEFAULT_RESIZE_FRAME = false;
    private static final String  DEFAULT_WRAPPED_MODEL_CLASS = "YAHOO.hippo.DDModel"; 

    public DragSettings() {
        setIeFallbackClassName(DEFAULT_IE_FALLBACK_CLASS_NAME);
        setCenterFrame(DEFAULT_CENTER_FRAME);
        setResizeFrame(DEFAULT_RESIZE_FRAME);
        setWrappedModelClass(DEFAULT_WRAPPED_MODEL_CLASS);
        setFirstAncestorToBlur("");
    }

    public DragSettings(IPluginConfig config) {
        super(config);
        setIeFallbackClassName(config.getString(IE_FALLBACK_CLASS_NAME, DEFAULT_IE_FALLBACK_CLASS_NAME));
        setCenterFrame(config.getBoolean(CENTER_FRAME));
        setResizeFrame(DEFAULT_RESIZE_FRAME);
        setWrappedModelClass(DEFAULT_WRAPPED_MODEL_CLASS);
        setFirstAncestorToBlur(config.getString(FIRST_ANCESTOR_TO_BLUR, ""));
    }
    
    public DragSettings setCenterFrame(boolean center) {
        put(CENTER_FRAME, center);
        return this;
    }

    public DragSettings setResizeFrame(boolean resize) {
        put(RESIZE_FRAME, resize);
        return this;
    }

    public DragSettings setLabel(String label) {
        put(LABEL, label);
        return this;
    }

    private DragSettings setIeFallbackClassName(String className) {
        put(IE_FALLBACK_CLASS_NAME, className);
        return this;
    }

    public DragSettings setWrappedModelClass(String string) {
        put(WRAPPED_MODEL_CLASS, string, false);
        return this;
    }

    public DragSettings setFirstAncestorToBlur(String string) {
        put(FIRST_ANCESTOR_TO_BLUR, string);
        return this;
    }

}
