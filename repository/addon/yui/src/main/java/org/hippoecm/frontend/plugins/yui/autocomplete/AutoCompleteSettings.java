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

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugins.yui.util.HeaderContributorHelper.JsConfig;

/**
 * This class encapsulates various settings for {@link AutoCompleteBehavior}
 */

public final class AutoCompleteSettings implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String containerId = "auto-complete-container";
    private String prehighlightClassName = "yui-ac-prehighlight";
    private boolean useShadow = true;
    private boolean useIFrame = false;
    private int maxResultsDisplayed = 10;
    private int minQueryLength = 1;
    private double queryDelay = 0.2;
    
    private String schemaResultList = "response.results";
    private String[] schemaFields = null;
    private Map<String, String> schemaMetaFields = null;
    
    //Hippo parameters
    private boolean submitOnlyOnEnter = false;
    
    JsConfig getJsConfig() {
        JsConfig conf = new JsConfig();
        conf.put("containerId", containerId);
        conf.put("prehighlightClassName", prehighlightClassName);
        conf.put("useShadow", useShadow);
        conf.put("useIFrame", useIFrame);
        conf.put("submitOnlyOnEnter", submitOnlyOnEnter);
        conf.put("maxResultsDisplayed", maxResultsDisplayed);
        conf.put("minQueryLength", minQueryLength);
        conf.put("queryDelay", queryDelay);
        conf.put("schemaResultList", schemaResultList);
        conf.put("schemaFields", schemaFields);
        conf.put("schemaMetaFields", schemaMetaFields);
        return conf;
    }

    public String getContainerId() {
        return containerId;
    }

    public AutoCompleteSettings setContainerId(String containerId) {
        this.containerId = containerId;
        return this;
    }

    public String getPrehighlightClassName() {
        return prehighlightClassName;
    }

    public AutoCompleteSettings setPrehighlightClassName(String className) {
        this.prehighlightClassName = className;
        return this;
    }

    public boolean isUseShadow() {
        return useShadow;
    }

    public AutoCompleteSettings setUseShadow(boolean useShadow) {
        this.useShadow = useShadow;
        return this;
    }

    public boolean isUseIFrame() {
        return useIFrame;
    }

    public AutoCompleteSettings setUseIFrame(boolean useIFrame) {
        this.useIFrame = useIFrame;
        return this;
    }

    public int getMaxResultsDisplayed() {
        return maxResultsDisplayed;
    }

    public AutoCompleteSettings setMaxResultsDisplayed(int maxResultsDisplayed) {
        this.maxResultsDisplayed = maxResultsDisplayed;
        return this;
    }

    public int getMinQueryLength() {
        return minQueryLength;
    }

    public AutoCompleteSettings setMinQueryLength(int minQueryLength) {
        this.minQueryLength = minQueryLength;
        return this;
    }

    public double getQueryDelay() {
        return queryDelay;
    }

    public AutoCompleteSettings  setQueryDelay(double queryDelay) {
        this.queryDelay = queryDelay;
        return this;
    }

    public boolean isSubmitOnlyOnEnter() {
        return submitOnlyOnEnter;
    }

    public AutoCompleteSettings setSubmitOnlyOnEnter(boolean submitOnEnter) {
        this.submitOnlyOnEnter = submitOnEnter;
        return this;
    }
    
    public String getSchemaResultList() {
        return schemaResultList;
    }

    public AutoCompleteSettings setSchemaResultList(String schemaResultList) {
        this.schemaResultList = schemaResultList;
        return this;
    }

    public String[] getSchemaFields() {
        return schemaFields;
    }

    public AutoCompleteSettings setSchemaFields(String... schemaFields) {
        this.schemaFields = schemaFields;
        return this;
    }

    public Map<String, String> getSchemaMetaFields() {
        return schemaMetaFields;
    }

    public AutoCompleteSettings setSchemaMetaFields(Map<String, String> schemaMetaFields) {
        this.schemaMetaFields = schemaMetaFields;
        return this;
    }

}
