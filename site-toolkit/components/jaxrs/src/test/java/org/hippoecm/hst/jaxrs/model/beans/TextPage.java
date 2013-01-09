/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.model.beans;

import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.Node;

@Node(jcrType="testproject:textpage")
public class TextPage extends GeneralPage implements ContentNodeBinder {
    
    protected String summary;
    protected String bodyContent;
    
    @Override
    public String getTitle() {
        if (title != null) {
            return title;
        } else {
            return getProperty("testproject:title");
        }
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        if (summary != null) {
            return summary;
        } else {
            return getProperty("testproject:summary");
        }
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getBodyContent() {
        if (bodyContent != null) {
            return bodyContent;
        } else {
            return getHippoHtml("testproject:body").getContent();
        }
    }
    
    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }
    
    public boolean bind(Object content, javax.jcr.Node node) throws ContentNodeBindingException {
        try {
            TextPage commentPage = (TextPage) content;
            node.setProperty("testproject:title", commentPage.getTitle());
            node.setProperty("testproject:summary", commentPage.getSummary());
            javax.jcr.Node body = node.getNode("testproject:body");
            body.setProperty("hippostd:content", commentPage.getBodyContent());
        } catch (Exception e) {
            throw new ContentNodeBindingException(e);
        }
        
        // FIXME: return true only if actual changes happen.
        return true;
    }
    
}
