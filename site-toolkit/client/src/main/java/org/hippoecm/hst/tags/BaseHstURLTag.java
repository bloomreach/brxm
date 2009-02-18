package org.hippoecm.hst.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.core.component.HstURL;

/**
 * Abstract supporting class for Hst URL tags (action, redner and resource)
 */

public abstract class BaseHstURLTag extends TagSupport {
    
    private static final long serialVersionUID = 1L;

    protected String var = null;
    
    protected String type = null;
    
    protected Boolean escapeXml = true;
        
    protected Map<String, List<String>> parametersMap = 
        new HashMap<String, List<String>>();

    protected List<String> removedParametersList = 
        new ArrayList<String>();
    

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
        
        HstURL url = getUrl();
        
        if(url == null){
            throw new IllegalStateException("internal error: url not set");
        }
        
        url.setType(getType());
        
        setUrlParameters(url);
        
        HttpServletResponse response = 
            (HttpServletResponse) pageContext.getResponse();
        
        //  properly encoding urls to allow non-cookie enabled sessions - PLUTO-252 
        String urlString = response.encodeURL(url.toString());

        if(escapeXml)
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
            pageContext.setAttribute(var, urlString,
                                     PageContext.PAGE_SCOPE);
        }
        
        /*cleanup*/
        parametersMap.clear();
        removedParametersList.clear();
        
        setUrl(null);
        
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
    
    /**
     * Returns the type property.
     * @return String
     */
    public String getType() {
        return type;
    }
    
    /**
     * Returns escapeXml property.
     * @return Boolean
     */
    public Boolean getEscapeXml() {
        return escapeXml;
    }
    
    
    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    /**
     * Sets the type property.
     * @param type The type to set
     * @return void
     */
    public void setType(String type) {
        this.type = type;
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
        
        Set<String> keySet = parametersMap.keySet();
                
        for(String key : keySet){
            
            List<String> valueList = parametersMap.get(key);
        
            String[] valueArray = valueList.toArray(new String[0]);
            
            url.setParameter(key, valueArray);
        }
    }
    
    
    /**
     * @return the url
     */
    protected abstract HstURL getUrl();


    /**
     * @param url the url to set
     */
    protected abstract void setUrl(HstURL url);
    
    
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
