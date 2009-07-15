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
package org.hippoecm.hst.demo.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.persistence.ContentNodeBinder;
import org.hippoecm.hst.persistence.ContentPersistenceBindingException;

@Node(jcrType="demosite:comment")
public class CommentBean extends TextBean implements ContentNodeBinder{

    private String newTitle;
    private String newBody;
    
    public void setTitle(String title) { 
        this.newTitle = title;
    }
    
    public void setBody(String body) { 
        this.newBody = body;
    }
    
    @Override
    public Calendar getDate() {
        return getProperty("demosite:date");
    }

    public BaseBean getCommentTo(){
        HippoBean bean = getBean("demosite:commentlink");
        if(!(bean instanceof CommentLinkBean)) {
            return null;
        }
        CommentLinkBean commentLinkBean = (CommentLinkBean)bean;
        if(commentLinkBean == null) {
            return null;
        }
        HippoBean b = commentLinkBean.getReferencedBean();
        if(b == null || !(b instanceof BaseBean)) {
            return null;
        } 
        return (BaseBean)b;
    }
    
    public boolean bind(Object content, javax.jcr.Node node) throws ContentPersistenceBindingException {
        try {
            if(this.newTitle != null) {
                node.setProperty("demosite:title", newTitle);
            }
            if(this.newBody != null) {
                javax.jcr.Node body = node.getNode("demosite:body");
                body.setProperty("hippostd:content", newBody);
            }
        } catch (Exception e) {
            throw new ContentPersistenceBindingException(e);
        }
        
        // FIXME: return true only if actual changes happen.
        return true;
    }
}
