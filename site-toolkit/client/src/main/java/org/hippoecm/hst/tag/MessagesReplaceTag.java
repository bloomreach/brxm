/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.taglibs.standard.tag.common.fmt.BundleSupport;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.hippoecm.hst.utils.MessageUtils;
import org.hippoecm.hst.utils.TagUtils;

/**
 * Messages Replacing Tag by resource bundle
 */
public class MessagesReplaceTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

    protected String var;
    protected ResourceBundle bundle;
    protected String basename;
    protected Locale locale;
    protected String localeString;
    protected String variablePrefix;
    protected String variableSuffix;
    protected Character escapeChar;
    protected String scope;
    protected boolean escapeMessageXml = true;

    @Override
    public int doStartTag() throws JspException{
        if (var != null) {
            TagUtils.removeVar(var, pageContext, scope);
        }

        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException{
        if (bundle == null) {
            if (locale == null) {
                if (StringUtils.isNotEmpty(localeString)) {
                    locale = LocaleUtils.toLocale(localeString);
                } else {
                    locale = TagUtils.getLocale(pageContext);
                }
            }
    
            if (StringUtils.isNotEmpty(basename)) {
                bundle = ResourceBundleUtils.getBundle(basename, locale);
            }

            if (bundle == null) {
                LocalizationContext locCtx = getLocalizationContext();

                if (locCtx != null) {
                    bundle = locCtx.getResourceBundle();
                }
            }
        }

        try {
            if (bodyContent != null) {
                String textContent = bodyContent.getString();

                if (textContent != null) {
                    if (bundle != null) {
                        textContent = MessageUtils.replaceMessagesByBundle(bundle, textContent,
                                                                           variablePrefix, variableSuffix,
                                                                           escapeChar, isEscapeMessageXml());
                    }

                    if (var == null) {
                        try {
                            JspWriter writer = pageContext.getOut();
                            writer.print(textContent);
                        } catch (IOException ioe) {
                            cleanup();
                            throw new JspException(" Exception: cannot write to the output writer.");
                        }
                    } else {
                        pageContext.setAttribute(var, textContent, TagUtils.getScopeByName(scope));
                    }
                }
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        var = null;
        bundle = null;
        basename = null;
        locale = null;
        localeString = null;
        scope = null;
        escapeMessageXml = true;
    }

    @Override
    public void release(){
        super.release();        
    }

    public String getVar() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public String getLocaleString() {
        return localeString;
    }

    public void setLocaleString(String localeString) {
        this.localeString = localeString;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public String getVariableSuffix() {
        return variableSuffix;
    }

    public String getScope() {
        return scope;
    }

    public void setVariableSuffix(String variableSuffix) {
        this.variableSuffix = variableSuffix;
    }

    public Character getEscapeChar() {
        return escapeChar;
    }

    public void setEscapeChar(Character escapeChar) {
        this.escapeChar = escapeChar;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isEscapeMessageXml() {
        return escapeMessageXml;
    }

    public void setEscapeMessageXml(boolean escapeMessageXml) {
        this.escapeMessageXml = escapeMessageXml;
    }

    protected LocalizationContext getLocalizationContext() {
        LocalizationContext locCtx = null;

        Tag t = findAncestorWithClass(this, BundleSupport.class);

        if (t != null) {
            BundleSupport parent = (BundleSupport) t;
            locCtx = parent.getLocalizationContext();
        } else {
            locCtx = BundleSupport.getLocalizationContext(pageContext);
        }

        return locCtx;
    }

    /* -------------------------------------------------------------------*/

    /**
     * TagExtraInfo class for MsgReplaceTag.
     */
    public static class TEI extends TagExtraInfo {

        public VariableInfo [] getVariableInfo(TagData tagData) {
            List<VariableInfo> viList = new ArrayList<VariableInfo>();

            Object attr = tagData.getAttributeString("var");
            if (attr != null) {
                viList.add(new VariableInfo("var", "java.lang.String", true, VariableInfo.AT_BEGIN));
            }

            attr = tagData.getAttribute("bundle");
            if (attr != null) {
                viList.add(new VariableInfo("bundle", "java.util.ResourceBundle", true, VariableInfo.AT_BEGIN));
            }

            attr = tagData.getAttributeString("basename");
            if (attr != null) {
                viList.add(new VariableInfo("basename", "java.lang.String", true, VariableInfo.AT_BEGIN));
            }

            attr = tagData.getAttributeString("localeString");
            if (attr != null) {
                viList.add(new VariableInfo("localeString", "java.lang.String", true, VariableInfo.AT_BEGIN));
            }

            attr = tagData.getAttributeString("locale");
            if (attr != null) {
                viList.add(new VariableInfo("locale", "java.util.Locale", true, VariableInfo.AT_BEGIN));
            }

            attr = tagData.getAttributeString("variablePrefix");
            if (attr != null) {
                viList.add(new VariableInfo("variablePrefix", "java.lang.String", true, VariableInfo.AT_BEGIN));
            }

            attr = tagData.getAttributeString("variableSuffix");
            if (attr != null) {
                viList.add(new VariableInfo("variableSuffix", "java.lang.String", true, VariableInfo.AT_BEGIN));
            }

            attr = tagData.getAttributeString("escapeChar");
            if (attr != null) {
                viList.add(new VariableInfo("escapeChar", "java.lang.Character", true, VariableInfo.AT_BEGIN));
            }

            return viList.toArray(new VariableInfo[viList.size()]);
        }

    }

}
