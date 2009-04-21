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

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.Node;

@Node(jcrType="hippo:document")
public class HippoDocument extends HippoItem{

    private HippoHtml html;
    private boolean initializedHtml;
    
    /**
     * @param relPath
     * @return <code>HippoHtml</code> or <code>null</code> if no node exists as relPath or no node of type "hippostd:html"
     */
    public HippoHtml getHippoHtml(String relPath) {
        // you cannot check for html not being null, because getObject might return null. Therefore, use 
        // boolean initializedHtml
        if(initializedHtml) {
            return html;
        } else {
            initializedHtml = true;
            html = getBean(relPath);
            return html;
        }
    }

    @Override
    public void detach(){
        super.detach();
        if(this.html != null) {
            this.html.detach();
        }
    }
    
    @Override
    public void attach(Session session){
        super.attach(session);
        this.html = null;
        this.initializedHtml = false;
    }

    
}
