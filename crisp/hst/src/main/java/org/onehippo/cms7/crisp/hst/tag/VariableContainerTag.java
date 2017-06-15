/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.hst.tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Abstract JSTL Tag Library base class that may embed tags processed by {@link VariableTag}.
 */
public abstract class VariableContainerTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private Map<String, Object> variablesMap = new LinkedHashMap<>();

    protected void cleanup() {
        if (variablesMap != null) {
            variablesMap.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    /**
     * Adds a variable by the name and value to the variables map. 
     * @param name variable name
     * @param value variable value
     */
    protected void addVariable(String name, String value) {
        if (name == null) {
            throw new IllegalArgumentException("The variable name must not be null!");
        }

        if (variablesMap == null) {
            variablesMap = new LinkedHashMap<>();
        }

        variablesMap.put(name, value);
    }

    protected Map<String, Object> getVariablesMap() {
        return (variablesMap != null) ? variablesMap : Collections.emptyMap();
    }
}