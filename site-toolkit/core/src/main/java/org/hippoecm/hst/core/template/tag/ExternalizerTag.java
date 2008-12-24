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

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.template.node.el.ContentELNode;
import org.hippoecm.hst.core.template.node.el.ContentELNodeImpl;
import org.hippoecm.hst.core.template.node.el.ELNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expose a node with EL stuff to the pageContext. For now only the current PageNode!
 * 
 *
 */
public class ExternalizerTag extends SimpleTagSupport {
    private static final Logger log = LoggerFactory.getLogger(LinkTag.class);

    private ELNode item;
    

    @Override
    public void doTag() throws JspException, IOException {
        if(item instanceof ContentELNode) {
            log.debug("Externalizing ContentELNode. From now on, links are rewritten to external urls");
           ((ContentELNode)item).setExternalize(true);
        } else {
            log.warn("Cannot externalize an item that is not of type ContentElNode");
        }
    }

    public ELNode getItem(){
        return item;
    }
    
    public void setItem(ELNode item){
        this.item = item;
    }

}
