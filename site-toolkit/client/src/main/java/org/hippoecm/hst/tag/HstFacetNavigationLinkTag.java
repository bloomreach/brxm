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
package org.hippoecm.hst.tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetSubNavigation;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract supporting class for Hst Link tags
 */

public class HstFacetNavigationLinkTag extends TagSupport {
    

    private final static Logger log = LoggerFactory.getLogger(HstFacetNavigationLinkTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HippoFacetSubNavigation current;
    protected HippoFacetSubNavigation remove;
    protected List<HippoFacetSubNavigation> removeList;
   
    protected String var;
    
    protected Map<String, List<String>> parametersMap = new HashMap<String, List<String>>();
    
    /* (non-Javadoc)
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException{
    
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
        try {
            if(this.current == null || (this.remove == null && (this.removeList == null || this.removeList.isEmpty()))) {
                log.warn("Cannot remove a facet-value combi because 'current' or 'remove(List)' is null or empty");
                return EVAL_PAGE;
            }

            final HstRequestContext requestContext = RequestContextProvider.get();
            if(requestContext == null) {
                log.warn("There is no HstRequestContext. Cannot create an HstLink outside the hst request processing. Return");
                return EVAL_PAGE;
            }

            HstLink link = requestContext.getHstLinkCreator().create(current.getNode(), requestContext,null, true, true);

            if(link == null || link.getPath() == null) {
                log.warn("Unable to rewrite link for '{}'. Return EVAL_PAGE", current.getPath());
                return EVAL_PAGE;
            }

            // now strip of the facet-value combi(s) that needs to be stripped of
            String path = link.getPath();

            List<HippoFacetSubNavigation> combinedRemovedList = new ArrayList<HippoFacetSubNavigation>();
            if(this.removeList != null) {
                combinedRemovedList.addAll(this.removeList);
            }
            if(this.remove != null) {
                combinedRemovedList.add(this.remove);
            }

            for(HippoFacetSubNavigation toRemove : combinedRemovedList) {
                String removeFacetValue = "/"+toRemove.getFacetValueCombi().getKey()+"/"+toRemove.getFacetValueCombi().getValue();
                if(path.contains(removeFacetValue)) {
                    path = path.replace(removeFacetValue, "");
                    log.debug("Removed facetvalue combi. Link from '{}' --> '{}'", path, link.getPath());
                } else {
                    log.warn("Cannot remove '{}' from the current faceted navigation url '{}'.", removeFacetValue, path);
                }
            }
            link.setPath(path);
            String urlString = link.toUrlForm(requestContext, false);


            HttpServletRequest servletRequest = (HttpServletRequest) pageContext.getRequest();
            // append again the current queryString as we are context relative
            if(servletRequest.getQueryString() != null && !"".equals(servletRequest.getQueryString())) {
                urlString += "?"+servletRequest.getQueryString();
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
                pageContext.setAttribute(var, urlString, varScope);
            }
            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        parametersMap.clear();
        var = null;
        current = null;
        remove = null;
        removeList = null;
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
    
    public HippoFacetSubNavigation getCurrent(){
        return this.current;
    }
    
    
    public void setCurrent(HippoFacetSubNavigation current) {
        this.current = current;
    }
    
    public HippoFacetSubNavigation getRemove(){
        return this.remove;
    }
    
    public void setRemove(HippoFacetSubNavigation remove) {
        this.remove = remove;
    }
    
    public List<HippoFacetSubNavigation> getRemoveList(){
        return this.removeList;
    }
    
    public void setRemoveList(List<HippoFacetSubNavigation> removeList) {
        this.removeList = removeList;
    }
   
    /**
     * Sets the var property.
     * @param var The var to set
     * @return void
     */
    public void setVar(String var) {
        this.var = var;
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
