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
package org.hippoecm.hst.caching;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.hippoecm.hst.caching.validity.ExpiresValidity;
import org.hippoecm.hst.core.request.HstRequestContext;

public class CacheTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;
    
    private String nameExpr;  // tag attribute
    private String keyExpr;  // tag attribute
    private CacheKey key; // parsed tag attribute

    private transient Cache cache;   // cache
    private CachedResponse cachedResponse;
    private static final int TTL = 60; // time to live seconds 
    
    @Override
    public int doStartTag() throws JspException {
        key = new CacheKey(nameExpr, CacheTag.class);
        HstRequestContext hstRequestContext = (HstRequestContext)pageContext.getAttribute(HstRequestContext.class.getName());
        if(hstRequestContext == null) {
           // log.warn("hstRequestContext is null");
            return EVAL_BODY_BUFFERED;
        }
        
        this.cache = CacheManagerImpl.getCache(hstRequestContext.getUserID());
        this.cachedResponse = this.cache.get(this.key);
        if (this.cachedResponse != null) {
            return SKIP_BODY;
        } else {
            return EVAL_BODY_BUFFERED;
        }
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            String body = null;
            if (this.cachedResponse == null) {
                if (bodyContent == null || bodyContent.getString() == null) {
                    body = "";
                } else {
                    body = bodyContent.getString().trim();
                }
                CachedResponse newCachedResponse = new CachedResponseImpl(new ExpiresValidity(TTL*1000), body);
                this.cache.store(this.key, newCachedResponse);
            } else {
                body = (String)this.cachedResponse.getResponse();
            }
            pageContext.getOut().write(body);
        } catch (IOException ex) {
            throw new JspException(ex);
        }
        return EVAL_PAGE;
    }


    public void setName(String nameExpr) {
        this.nameExpr = nameExpr;
    }

    public void setKey(String keyExpr) {
        this.keyExpr = keyExpr;
    }

}
