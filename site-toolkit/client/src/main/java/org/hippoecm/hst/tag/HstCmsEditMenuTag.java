/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstCmsEditMenuTag extends TagSupport  {
    
    private final static Logger log = LoggerFactory.getLogger(HstCmsEditMenuTag.class);
    
    private static final long serialVersionUID = 1L;

    protected HstSiteMenu menu;
    
    protected String var;
    
    protected String scope;

    
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

            if (menu == null) {
                log.warn("Cannot create a cms edit menu because no menu present");
                return EVAL_PAGE;
            }

            HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext == null) {
                log.warn("Cannot create a cms edit menu outside the hst request processing for '{}'");
                return EVAL_PAGE;
            }

            if (!requestContext.isCmsRequest()) {
                log.debug("Skipping cms edit url because not cms preview.");
                return EVAL_PAGE;
            }

            if (var == null) {
                try {
                    write(menu.getCanonicalIdentifier(), menu.isInherited());
                 } catch (IOException ioe) {
                    throw new JspException("ResourceURL-Tag Exception: cannot write to the output writer.");
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

                pageContext.setAttribute(var, menu.getCanonicalIdentifier(), varScope);
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        var = null;
        menu = null;
        scope = null;
    }

    protected void write(String menuId, boolean inherited) throws IOException {
        JspWriter writer = pageContext.getOut();
        StringBuilder htmlComment = new StringBuilder(); 
        htmlComment.append("<!-- ");
        htmlComment.append(" {\"type\":\"menu\"");
        // add uuid
        htmlComment.append(", \"uuid\":\"");
        htmlComment.append(menuId);
        if (inherited) {
            htmlComment.append(", \"inherited\":\"true\"");
        }
        htmlComment.append("\" } -->");
        writer.print(htmlComment.toString());
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
    
    public String getScope() {
        return scope;
    }
    
    public HstSiteMenu getMenu(){
        return menu;
    }
     
    public void setMenu(HstSiteMenu menu) {
        this.menu = menu;
    }
    
    /**
     * Sets the var property.
     * @param var The var to set
     */
    public void setVar(String var) {
        this.var = var;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    /* -------------------------------------------------------------------*/
        
    /**
     * TagExtraInfo class for HstCmsEditLinkTag.
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





   
