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
package org.hippoecm.hst.content.beans.standard;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippo:document")
public class HippoDocument extends HippoItem{

    private static Logger log = LoggerFactory.getLogger(HippoDocument.class);
    
    private Map<String, HippoHtmlWrapper> htmls = new HashMap<String, HippoHtmlWrapper>();
    
    /**
     * @param relPath
     * @return <code>HippoHtml</code> or <code>null</code> if no node exists as relPath or no node of type "hippostd:html"
     */
    public HippoHtml getHippoHtml(String relPath) {
        HippoHtmlWrapper wrapped = htmls.get(relPath);
        if(wrapped != null) {
            return wrapped.html;
        } else {
            Object o = getBean(relPath);
            if(o instanceof HippoHtml) { 
                wrapped = new HippoHtmlWrapper((HippoHtml)o);
                htmls.put(relPath, wrapped);
                return wrapped.html;
            } else {
                log.warn("Cannot get HippoHtml bean for relPath '{}' because returned bean is of a different class. Return null.", relPath);
                // even when null, put it in the map to avoid being refetched
                wrapped = new HippoHtmlWrapper((HippoHtml)null);
                htmls.put(relPath, wrapped);
                return null;
            }
        }
    }

    @Override
    public void detach(){
        super.detach();
        for(HippoHtmlWrapper wrapperHtml : this.htmls.values()) {
            if(wrapperHtml.html!= null) {
                wrapperHtml.html.detach();
            }
        }
    }
    
    @Override
    public void attach(Session session){
        super.attach(session);
        this.htmls.clear();
    }

    private class HippoHtmlWrapper{
        private HippoHtml html;
        private HippoHtmlWrapper(HippoHtml html){
            this.html = html;
        }
    }
}
