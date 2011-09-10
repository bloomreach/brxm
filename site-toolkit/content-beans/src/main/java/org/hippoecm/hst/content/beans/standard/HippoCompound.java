/*
 *  Copyright 2011 Hippo.
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

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.BeanWrapper;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Node(jcrType="hippo:compound")
public class HippoCompound extends HippoItem {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoCompound.class);

    private Map<String, BeanWrapper<HippoHtml>> htmls = new HashMap<String, BeanWrapper<HippoHtml>>();

    /**
     * @param relPath
     * @return <code>HippoHtml</code> or <code>null</code> if no node exists as relPath or no node of type "hippostd:html"
     */
    public HippoHtml getHippoHtml(String relPath) {
        BeanWrapper<HippoHtml> wrapped = htmls.get(relPath);
        if(wrapped != null) {
            return wrapped.getBean();
        } else {
            Object o = getBean(relPath);
            if(o == null) {
                if(log.isDebugEnabled()) {
                    log.debug("No bean found for relPath '{}' at '{}'", relPath, this.getPath());
                }
                wrapped = new BeanWrapper<HippoHtml>(null);
                htmls.put(relPath, wrapped);
                return null;
            } else if(o instanceof HippoHtml) {
                wrapped = new BeanWrapper<HippoHtml>((HippoHtml)o);
                htmls.put(relPath, wrapped);
                return wrapped.getBean();
            } else {
                log.warn("Cannot get HippoHtml bean for relPath '{}' at '{}' because returned bean is not of type HippoHtml but is '"+o.getClass().getName()+"'", relPath, this.getPath());
                // even when null, put it in the map to avoid being refetched
                wrapped = new BeanWrapper<HippoHtml>(null);
                htmls.put(relPath, wrapped);
                return null;
            }
        }
    }

}
