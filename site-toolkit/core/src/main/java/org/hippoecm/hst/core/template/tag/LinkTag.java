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
package org.hippoecm.hst.core.template.tag;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.mapping.RelativeURLMappingImpl;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.node.el.ELNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expose a node with EL stuff to the pageContext. For now only the current PageNode!
 * 
 *
 */
public class LinkTag extends SimpleTagSupport {
    private static final Logger log = LoggerFactory.getLogger(LinkTag.class);

    private String var;
    private String location;
    private String staticattr;
    private ELNode item;

    @Override
    public void doTag() throws JspException, IOException {
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        URLMapping urlMapping = (URLMapping)request.getAttribute(HSTHttpAttributes.URL_MAPPING_ATTR);
        String href = null;
        if(urlMapping == null) {
            log.debug("urlMapping not set as attribute on request. Cannot rewrite a link. Try to make it relative only for staticattr");
            if(staticattr != null ) {
                href =  RelativeURLMappingImpl.computeRelativeUrl(staticattr, request.getRequestURI());
            }
        } else {
            if(item!= null) {
                href = urlMapping.rewriteLocation(item.getJcrNode());
            } else if(location != null ) {
                href = urlMapping.rewriteLocation(location);
            } else if(staticattr != null ) {
                href = urlMapping.getLocation(staticattr);
            }
        }
        if(href != null) {
            Link link = new Link();
            link.setHref(href);
            link.setSrc(href);
            link.setLocation(href);
            pageContext.setAttribute(getVar(), link);
        }
    }

    public ELNode getItem(){
        return item;
    }
    

    public void setItem(ELNode item){
        this.item = item;
    }

    public String getVar() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }
    
    public String getLocation() {
        return location;
    }

    public void setStatic(String staticattr) {
        this.staticattr = staticattr;
    }
    
    public String getStatic() {
        return staticattr;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    
    public class Link {
        /*
         * three link attributes, all returning the same value. 
         * in jsp, you can use link.href, link.src or link.location
         */
        private String href;
        private String src;
        private String location;
       
        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
        
    }

}
