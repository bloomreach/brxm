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

package org.hippoecm.frontend.plugins.yui.autocomplete;

import java.util.Map;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.DoubleSetting;
import org.hippoecm.frontend.plugins.yui.javascript.IntSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringArraySetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringMapSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

/**
 * This class encapsulates various settings for {@link AutoCompleteBehavior}
 */

public final class AutoCompleteSettings extends AjaxSettings {

    private static final long serialVersionUID = 1L;

    private static final StringSetting CONTAINER_ID = new StringSetting("containerId");
    private static final StringSetting PRE_HIGHLIGHT_CLASSNAME = new StringSetting("prehighlightClassName");
    private static final BooleanSetting USE_SHADOW = new BooleanSetting("useShadow");
    private static final BooleanSetting USE_IFRAME = new BooleanSetting("useIframe");
    private static final BooleanSetting ANIM_VERT = new BooleanSetting("animVert");
    private static final IntSetting MAX_RESULTS_DISPLAYED = new IntSetting("maxResultsDisplayed", 10);
    private static final IntSetting MIN_QUERY_LENGTH = new IntSetting("minQueryLength", 1);
    private static final DoubleSetting QUERY_DELAY = new DoubleSetting("queryDelay", 0.3);
    private static final BooleanSetting SUBMIT_ONLY_ON_ENTER = new BooleanSetting("submitOnlyOnEnter");
    private static final StringSetting SCHEMA_RESULT_LIST = new StringSetting("schemaResultList");
    private static final StringArraySetting SCHEMA_FIELDS = new StringArraySetting("schemaFields");
    private static final StringMapSetting SCHEMA_META_FIELDS = new StringMapSetting("schemaMetaFields");

    protected static final YuiType TYPE = new YuiType(AjaxSettings.TYPE, CONTAINER_ID, PRE_HIGHLIGHT_CLASSNAME, USE_SHADOW, USE_IFRAME, ANIM_VERT, MAX_RESULTS_DISPLAYED,
            MIN_QUERY_LENGTH, QUERY_DELAY, SUBMIT_ONLY_ON_ENTER, SCHEMA_RESULT_LIST, SCHEMA_FIELDS,
            SCHEMA_META_FIELDS);
    
    public AutoCompleteSettings(IPluginConfig config) {
        super(TYPE, config);
    }

    public AutoCompleteSettings setContainerId(String containerId) {
        CONTAINER_ID.set(containerId, this);
        return this;
    }

    public AutoCompleteSettings setPrehighlightClassName(String className) {
        PRE_HIGHLIGHT_CLASSNAME.set(className, this);
        return this;
    }

    public AutoCompleteSettings setUseShadow(boolean useShadow) {
        USE_SHADOW.set(useShadow, this);
        return this;
    }

    public AutoCompleteSettings setUseIFrame(boolean useIFrame) {
        USE_IFRAME.set(useIFrame, this);
        return this;
    }

    public AutoCompleteSettings setMaxResultsDisplayed(int maxResultsDisplayed) {
        MAX_RESULTS_DISPLAYED.set(maxResultsDisplayed, this);
        return this;
    }

    public AutoCompleteSettings setMinQueryLength(int minQueryLength) {
        MIN_QUERY_LENGTH.set(minQueryLength, this);
        return this;
    }

    public AutoCompleteSettings setQueryDelay(double queryDelay) {
        QUERY_DELAY.set(queryDelay, this);
        return this;
    }

    public AutoCompleteSettings setSubmitOnlyOnEnter(boolean submitOnEnter) {
        SUBMIT_ONLY_ON_ENTER.set(submitOnEnter, this);
        return this;
    }

    public AutoCompleteSettings setSchemaResultList(String schemaResultList) {
        SCHEMA_RESULT_LIST.set(schemaResultList, this);
        return this;
    }

    public AutoCompleteSettings setSchemaFields(String... schemaFields) {
        SCHEMA_FIELDS.set(schemaFields, this);
        return this;
    }

    public AutoCompleteSettings setSchemaMetaFields(Map<String, Object> schemaMetaFields) {
        SCHEMA_META_FIELDS.set(schemaMetaFields, this);
        return this;
    }

}
