package org.onehippo.cms7.crisp.hst.tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

public abstract class VariableContainerTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private Map<String, Object> variablesMap = new LinkedHashMap<>();

    protected void cleanup() {
        if (variablesMap != null) {
            variablesMap.clear();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    /**
     * Adds a variable by the name and value to the variables map. 
     * @param key String
     * @param value String
     * @return void
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