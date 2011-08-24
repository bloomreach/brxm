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

import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;

@Node(jcrType="demosite:commentdocument")
public class CommentBean extends TextBean {

    private Calendar date;
    private String commentToUuidOfHandle;
    
    @Override
    public Calendar getDate() {
        return date == null ? (Calendar)getProperty("demosite:date"): date;
    }
    
    public void setDate(Calendar date) {
        this.date = date;
    }

    public void setCommentTo(String commentToUuidOfHandle) {
        this.commentToUuidOfHandle = commentToUuidOfHandle;
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
    
    public boolean bind(Object content, javax.jcr.Node node) throws ContentNodeBindingException {
        super.bind(content, node);
        try {
            BaseBean bean =  (BaseBean) content;
            node.setProperty("demosite:date", bean.getDate());
            javax.jcr.Node commentLink = null;
            if(node.hasNode("demosite:commentlink")) {
                 commentLink = node.getNode("demosite:commentlink");
            } else {
                commentLink = node.addNode("demosite:commentlink", "demosite:commentlink");
            }
            commentLink.setProperty("hippo:docbase", commentToUuidOfHandle);
            commentLink.setProperty("hippo:values", new String[0]);
            commentLink.setProperty("hippo:modes", new String[0]);
            commentLink.setProperty("hippo:facets", new String[0]);
            
            
        } catch (Exception e) {
            throw new ContentNodeBindingException(e);
        }
        return true;
    }
    
}
