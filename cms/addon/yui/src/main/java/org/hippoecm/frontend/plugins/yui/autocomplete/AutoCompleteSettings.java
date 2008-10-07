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

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper.JsConfig;

/**
 * This class encapsulates various settings for {@link AutoCompleteBehavior}
 */

public final class AutoCompleteSettings extends JsConfig {
    private static final long serialVersionUID = 1L;
    
    private static final String CONTAINER_ID = "containerId";  //String
    private static final String PRE_HIGHLIGHT_CLASSNAME = "prehighlightClassName"; //String
    private static final String USE_SHADOW = "useShadow";  //boolean
    private static final String USE_IFRAME = "useIFrame";  //boolean
    private static final String MAX_RESULTS_DISPLAYED = "maxResultsDisplayed";  //int
    private static final String MIN_QUERY_LENGTH = "minQueryLength";  //int
    private static final String QUERY_DELAY = "queryDelay";  //double
    private static final String SUBMIT_ONLY_ON_ENTER = "submitOnlyOnEnter";  //boolean
    private static final String SCHEMA_RESULT_LIST = "schemaResultList";  //String
    private static final String SCHEMA_FIELDS = "schemaFields";  //String[]
    private static final String SCHEMA_META_FIELDS = "schemaMetaFields";  //Map<String,String>

    private static final int NUMBER_OF_SETTINGS = 11;

    public AutoCompleteSettings() {
        super(NUMBER_OF_SETTINGS);
    }
    
    //this constructor can be replaced by the same pluginc onfig approach used with the layout/wireframes
    public AutoCompleteSettings(IPluginConfig config) {
        this();

        //initialize JsConfig map. 
        put(CONTAINER_ID, config.getString(CONTAINER_ID));
        put(PRE_HIGHLIGHT_CLASSNAME, config.getString(PRE_HIGHLIGHT_CLASSNAME));
        put(USE_SHADOW, config.getBoolean(USE_SHADOW));
        put(USE_IFRAME, config.getBoolean(USE_IFRAME));
        put(SUBMIT_ONLY_ON_ENTER, config.getBoolean(SUBMIT_ONLY_ON_ENTER));
        put(SCHEMA_RESULT_LIST, config.getString(SCHEMA_RESULT_LIST));
        put(SCHEMA_FIELDS, config.getStringArray(SCHEMA_FIELDS));
        put(SCHEMA_META_FIELDS, config.getPluginConfig(SCHEMA_META_FIELDS));
        
        //Don't override clientside defaults
        if(config.containsKey(MAX_RESULTS_DISPLAYED)) 
            put(MAX_RESULTS_DISPLAYED, config.getInt(MAX_RESULTS_DISPLAYED));
        
        if(config.containsKey(MIN_QUERY_LENGTH))
                put(MIN_QUERY_LENGTH, config.getInt(MIN_QUERY_LENGTH));
        
        if(config.containsKey(QUERY_DELAY))
            put(QUERY_DELAY, config.getDouble(QUERY_DELAY, 0.3));

    }

    public AutoCompleteSettings setContainerId(String containerId) {
        put(CONTAINER_ID, containerId);
        return this;
    }

    public AutoCompleteSettings setPrehighlightClassName(String className) {
        put(PRE_HIGHLIGHT_CLASSNAME, className);
        return this;
    }

    public AutoCompleteSettings setUseShadow(boolean useShadow) {
        put(USE_SHADOW, useShadow);
        return this;
    }

    public AutoCompleteSettings setUseIFrame(boolean useIFrame) {
        put(USE_IFRAME, useIFrame);
        return this;
    }

    public AutoCompleteSettings setMaxResultsDisplayed(int maxResultsDisplayed) {
        put(MAX_RESULTS_DISPLAYED, maxResultsDisplayed);
        return this;
    }

    public AutoCompleteSettings setMinQueryLength(int minQueryLength) {
        put(MIN_QUERY_LENGTH, minQueryLength);
        return this;
    }

    public AutoCompleteSettings  setQueryDelay(double queryDelay) {
        put(QUERY_DELAY, queryDelay);
        return this;
    }

    public AutoCompleteSettings setSubmitOnlyOnEnter(boolean submitOnEnter) {
        put(SUBMIT_ONLY_ON_ENTER, submitOnEnter);
        return this;
    }

    public AutoCompleteSettings setSchemaResultList(String schemaResultList) {
        put(SCHEMA_RESULT_LIST, schemaResultList);
        return this;
    }

    public AutoCompleteSettings setSchemaFields(String... schemaFields) {
        put(SCHEMA_FIELDS, schemaFields);
        return this;
    }

    public AutoCompleteSettings setSchemaMetaFields(Map<String, String> schemaMetaFields) {
        put(SCHEMA_META_FIELDS, schemaMetaFields);
        return this;
    }

}
