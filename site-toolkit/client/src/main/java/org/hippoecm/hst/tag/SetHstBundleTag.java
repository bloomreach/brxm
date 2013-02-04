/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.tag;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SetHstBundleTag
 */
public class SetHstBundleTag extends TagSupport {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(SetHstBundleTag.class);

    protected String basename;
    protected boolean fallbackToJavaResourceBundle = true;

    private int scope;
    private String var;

    public SetHstBundleTag() {
        super();
        init();
    }

    private void init() {
        basename = null;
        fallbackToJavaResourceBundle = true;
        scope = PageContext.PAGE_SCOPE;
    }

    // *********************************************************************
    // Tag attributes known at translation time
    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = getScope(scope);
    }

    // for tag attribute
    public void setBasename(String basename) throws JspTagException {
        this.basename = basename;
    }

    public void setFallbackToJavaResourceBundle(boolean fallbackToJavaResourceBundle) throws JspTagException {
        this.fallbackToJavaResourceBundle = fallbackToJavaResourceBundle;
    }

    public int doEndTag() throws JspException {
        try {
            LocalizationContext locCtxt = getLocalizationContext(pageContext, basename, fallbackToJavaResourceBundle);

            if (var != null) {
                pageContext.setAttribute(var, locCtxt, scope);
            } else {
                Config.set(pageContext, Config.FMT_LOCALIZATION_CONTEXT, locCtxt, scope);
            }
            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        init();
    }

    public void release() {
        init();
    }

    public static LocalizationContext getLocalizationContext(PageContext pc, String basename, boolean fallbackToJavaResourceBundle) {
        HstRequest hstRequest = HstRequestUtils.getHstRequest((HttpServletRequest) pc.getRequest());
        Locale locale = (hstRequest != null ? hstRequest.getLocale() : pc.getRequest().getLocale());
        ResourceBundle bundle = null;

        try {
            bundle = ResourceBundleUtils.getBundle(hstRequest, basename, locale, fallbackToJavaResourceBundle);
        } catch (Exception e) {
            log.warn("Failed to get bundle for basename: {}. {}", basename, e);
        }

        if (bundle == null) {
            return new LocalizationContext();
        } else {
            return new LocalizationContext(bundle);
        }
    }

    private static int getScope(String scope) {
        if ("request".equalsIgnoreCase(scope)) {
            return PageContext.REQUEST_SCOPE;
        } else if ("session".equalsIgnoreCase(scope)) {
            return PageContext.SESSION_SCOPE;
        } else if ("application".equalsIgnoreCase(scope)) {
            return PageContext.APPLICATION_SCOPE;
        }

        return PageContext.PAGE_SCOPE;
    }

}