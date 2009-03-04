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
package org.hippoecm.hst.tag;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstURL;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstLinkTag extends BaseHstURLTag {
    
    private static final long serialVersionUID = 1L;

    protected String value;
    
    protected String var;
    
    protected String context;
    
    protected String scope;

    protected Boolean escapeXml = true;
        
    protected Map<String, List<String>> parametersMap = new HashMap<String, List<String>>();
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
        
        HstURL url = getUrl();
        
        if(url == null){
            throw new IllegalStateException("internal error: url not set");
        }
        
        if (var != null) {
            pageContext.removeAttribute(var, PageContext.PAGE_SCOPE);
        }
        
        return EVAL_BODY_INCLUDE;
    }
    
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() throws JspException{
        
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
        String characterEncoding = response.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        StringBuilder url = new StringBuilder();

        if (this.context != null) {
            url.append(this.context);
        } else {
            url.append(request.getContextPath());
            url.append(request.getServletPath());
        }
        
        if (!this.value.startsWith("/")) {
            url.append("/");
        }
        
        url.append(this.value);
        
        boolean firstParamDone = (url.indexOf("?") >= 0);
        
        for (Map.Entry<String, List<String>> entry : this.parametersMap.entrySet()) {
            String name = entry.getKey();
            
            for (String value : entry.getValue()) {
                String encodedValue = value;
                try {
                    encodedValue = URLEncoder.encode(value, characterEncoding);
                } catch (Exception e) {
                }
                
                url.append(firstParamDone ? "&" : ":");
                url.append(name).append("=");
                url.append(encodedValue);
                
                firstParamDone = true;
            }
        }
        
        String urlString = response.encodeURL(url.toString());

        if (escapeXml)
        {
             urlString = doEscapeXml(urlString);
        }
        
        if (var == null) {
            try {               
                JspWriter writer = pageContext.getOut();
                writer.print(urlString);
            } catch (IOException ioe) {
                throw new JspException(
                    "Portlet/ResourceURL-Tag Exception: cannot write to the output writer.");
            }
        } 
        else {
            int varScope = PageContext.PAGE_SCOPE;
            
            if (this.scope != null) {
                if ("request".equals(this.scope)) {
                    varScope = PageContext.REQUEST_SCOPE;
                } else if ("session".equals(this.scope)) {
                    varScope = PageContext.SESSION_SCOPE;
                } else if ("application".equals(this.scope)) {
                    varScope = PageContext.APPLICATION_SCOPE;
                }
            }
            
            pageContext.setAttribute(var, urlString, varScope);
        }
        
        /*cleanup*/
        parametersMap.clear();
        var = null;
        scope = null;
        context = null;
        
        return EVAL_PAGE;
    }
    
    

    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#release()
     */
    @Override
    public void release(){
        super.release();        
    }
    
    
    /**
     * Returns the var property.
     * @return String
     */
    public String getVar() {
        return var;
    }
    
    public String getContext() {
        return context;
    }
    
    public String getScope() {
        return scope;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Returns escapeXml property.
     * @return Boolean
     */
    public Boolean getEscapeXml() {
        return escapeXml;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    /**
     * Sets the escapeXml property.
     * @param escapeXml
     * @return void
     */
    public void setEscapeXml(Boolean escapeXml) {
        this.escapeXml = escapeXml;
    }
    
    
    /**
     * Adds a key,value pair to the parameter map. 
     * @param key String
     * @param value String
     * @return void
     */
    protected void addParameter(String key,String value) {
        if((key == null) || (key.length() == 0)){
            throw new IllegalArgumentException(
                    "the argument key must not be null or empty!");
        }
        
        if((value == null) || (value.length() == 0)){//remove parameter
            if(parametersMap.containsKey(key)){
                parametersMap.remove(key);
            }
            removedParametersList.add(key);
        }
        else{//add value
            List<String> valueList = null;
        
            if(parametersMap.containsKey(key)){
                valueList = parametersMap.get(key);//get old value list                 
            }
            else{
                valueList = new ArrayList<String>();// create new value list                        
            }
        
            valueList.add(value);
        
            parametersMap.put(key, valueList);
        }
    }
    
    
    /**
     * Copies the parameters from map to the BaseURL.
     * @param url BaseURL
     * @return void
     */
    protected void setUrlParameters(HstURL url) {
        for(String key : parametersMap.keySet()) {
            
            List<String> valueList = parametersMap.get(key);
        
            String[] valueArray = valueList.toArray(new String[0]);
            
            url.setParameter(key, valueArray);
        }
        
        for (String key : removedParametersList) {
            url.setParameter(key, (String) null);
        }
    }
    
    protected HstURL getUrl() {
        return null;
    }

    protected void setUrl(HstURL url) {
        
    }
    
    
    /**
     * Replaces in String str the characters &,>,<,",' 
     * with their corresponding character entity codes.
     * @param str - the String where to replace 
     * @return String 
     */
    protected String doEscapeXml(String str) {
        if(!isEmpty(str)){
            str = str.replaceAll("&", "&amp;");
            str = str.replaceAll("<", "&lt;");
            str = str.replaceAll(">", "&gt;");
            str = str.replaceAll("\"", "&#034;");
            str = str.replaceAll("'", "&#039;");
        }
        return str;
    }
       
    
    /**
     * Checks if the string is empty. 
     * @param str String
     * @return boolean
     */
    private boolean isEmpty(String str) {
        return ((str == null) || (str.length() == 0));
    }
    
    
    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstURLTag.
     */
    public static class TEI extends TagExtraInfo {
        
        public VariableInfo[] getVariableInfo(TagData tagData) {
            VariableInfo vi[] = null;
            String var = tagData.getAttributeString("var");
            if (var != null) {
                vi = new VariableInfo[1];
                vi[0] =
                    new VariableInfo(var, "java.lang.String", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
}
