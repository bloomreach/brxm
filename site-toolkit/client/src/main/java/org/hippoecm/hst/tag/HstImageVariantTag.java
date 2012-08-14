/*
 *  Copyright 2012 Hippo.
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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.rewriter.ImageVariant;
import org.hippoecm.hst.content.rewriter.impl.DefaultImageVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstImageVariantTag extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(HstImageVariantTag.class);

    /**
     * the name of the image variant to use
     */
    protected String name = null;

    /**
     * the name of variants to replaces. If <code>null</code> all variants will be replaced
     */
    protected String replaces = null;

    /**
     * whether to fallback to original variant when the <code>name</code> variant does not exist. Default false
     */
    protected boolean fallback = false;

    /**
     * The html tag this image variant tag is part of
     */
    HstHtmlTag htmlTag;

    @Override
    public int doStartTag() throws JspException{

        htmlTag = (HstHtmlTag) findAncestorWithClass(this, HstHtmlTag.class);

        if (htmlTag == null) {
            throw new JspException("the 'imagevariant' Tag must have a HST's 'html' tag as a parent");
        }
        return SKIP_BODY;
    }
    

    @Override
    public int doEndTag() throws JspException{
        try {
            if (StringUtils.isBlank(name)) {
                log.warn("For imageVariant tag the name attribute is not allowed to be null or empty. Skip image variant");
            } else {

                List<String> replaceVariants = null;
                if (StringUtils.isNotBlank(replaces)) {
                    replaceVariants = new ArrayList<String>();
                    if (replaces.indexOf(",") > -1) {
                        String[] elems = replaces.split(",");
                        for (String elem : elems) {
                            if (StringUtils.isNotBlank(elem)) {
                                replaceVariants.add(elem);
                            }
                        }
                    } else {
                        replaceVariants.add(replaces);
                    }
                }
                ImageVariant imageVariant = new DefaultImageVariant(name, replaceVariants, fallback);
                htmlTag.setImageVariant(imageVariant);
            }

            return EVAL_PAGE;
        } finally {
            cleanup();
        }
    }

    protected void cleanup() {
        name = null;
        replaces = null;
        htmlTag = null;
        fallback = false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReplaces(final String replaces) {
        this.replaces = replaces;
    }

    public void setFallback(final boolean fallback) {
        this.fallback = fallback;
    }
}
