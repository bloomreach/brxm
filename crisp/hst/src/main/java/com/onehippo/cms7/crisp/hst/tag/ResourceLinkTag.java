/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.hst.tag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.utils.TagUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import com.onehippo.cms7.crisp.api.resource.Resource;
import com.onehippo.cms7.crisp.api.resource.ResourceException;
import com.onehippo.cms7.crisp.api.resource.ResourceLink;
import com.onehippo.cms7.crisp.hst.module.CrispHstServices;

/**
 * JSTL Tag library generating a URI link for a {@link Resource} object in a specific <strong>resource space</strong>.
 */
public class ResourceLinkTag extends VariableContainerTag {

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(ResourceLinkTag.class);

    private String resourceSpace;

    private Resource resource;

    private String var;

    private String scope;

    private Boolean escapeXml = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public int doStartTag() throws JspException {
        if (var != null) {
            pageContext.removeAttribute(var, PageContext.PAGE_SCOPE);
        }

        return EVAL_BODY_INCLUDE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int doEndTag() throws JspException {
        try {
            String urlString = null;

            ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker();
            ResourceLink link = broker.resolveLink(resourceSpace, resource, getVariablesMap());

            if (link != null) {
                urlString = link.getUri();
            }

            if (StringUtils.isNotBlank(urlString)) {
                if (escapeXml) {
                    urlString = HstRequestUtils.escapeXml(urlString);
                }

                TagUtils.writeOrSetVar(urlString, var, pageContext, scope);
            } else {
                log.warn("Blank resource URI was returned.");
            }
        } catch (ResourceException e) {
            log.warn("Failed to create a resource link.", e);
        } finally {
            cleanup();
        }

        return EVAL_PAGE;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        var = null;
        resourceSpace = null;
        resource = null;
        scope = null;
        escapeXml = true;
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

    public String getResourceSpace() {
        return resourceSpace;
    }

    public Resource getResource() {
        return resource;
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
     */
    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setResourceSpace(String resourceSpace) {
        this.resourceSpace = resourceSpace;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    /**
     * Sets the escapeXml property.
     * @param escapeXml whether or not escape generated markup string
     */
    public void setEscapeXml(Boolean escapeXml) {
        this.escapeXml = escapeXml;
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
                vi[0] = new VariableInfo(var, "java.lang.String", true, VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
}
