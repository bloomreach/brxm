/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.tag;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.jsp.JspException;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.VariableInfo;

import org.hippoecm.hst.site.HstServices;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyTag extends TagSupport {

    private final static Logger log = LoggerFactory.getLogger(TaxonomyTag.class);

    protected String var;
    private String[] keys;

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

        if(keys == null || keys.length == 0) {
            return EVAL_PAGE;
        }
        TaxonomyManager taxonomyManager = HstServices.getComponentManager().getComponent(TaxonomyManager.class.getName());
        if (taxonomyManager == null) {
            log.warn("There is no TaxonomyManager available. Configure this in your Spring configuration");
            return EVAL_PAGE;
        }

        List<LinkedList<Category>> resolvedTaxonomyItemLists = new ArrayList<>();
        for(String taxonomyItemKey : keys) {
            for(Taxonomy taxonomy : taxonomyManager.getTaxonomies().getRootTaxonomies()) {
                if(taxonomy.getCategoryByKey(taxonomyItemKey) != null) {
                    Category taxonomyItem = taxonomy.getCategoryByKey(taxonomyItemKey);
                    LinkedList<Category> taxonomyItemList = new LinkedList<>(taxonomyItem.getAncestors());

                    taxonomyItemList.addLast(taxonomyItem);

                    resolvedTaxonomyItemLists.add(taxonomyItemList);
                    break;
                }
            }
        }

        pageContext.setAttribute(var, resolvedTaxonomyItemLists);

        this.var = null;
        this.keys = null;
        return EVAL_PAGE;
    }


    /**
     * Returns the var property.
     * @return String
     */
    public String getVar() {
        return var;
    }

    /**
     * Returns the uuid property.
     * @return String
     */
    public String[] getKeys() {
        return this.keys;
    }



    /**
     * Sets the var property.
     * @param var The var to set
     */
    public void setVar(String var) {
        this.var = var;
    }

    /**
     * Sets the uuid property.
     * @param keys The uuid to set
     */
    public void setKeys(String[] keys) {
        this.keys = keys;
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
                    new VariableInfo(var, "java.util.List", true,
                                 VariableInfo.AT_BEGIN);
            }
            return vi;
        }

    }
}



