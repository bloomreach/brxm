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

import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="demosite:basedocument")
public class BaseBean extends HippoDocument implements ContentNodeBinder{
    
    public static final Logger log = LoggerFactory.getLogger(BaseBean.class);

    private String title;
    private String summary;
    private String html;
    
    protected final static String HTML_NODEPATH = "demosite:body";

    @IndexField
    public String getTitle() {
        return title == null ? (String)getProperty("demosite:title"): title ;
    }
    
    public void setTitle(String title) { 
        this.title = title;
    }


    @IndexField
    public String getSummary() {
        return summary == null ? (String)getProperty("demosite:summary"): summary ;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @IndexField
    public String getHtmlContent(){
        return getHippoHtml(HTML_NODEPATH).getContent();
    }

     public HippoHtml getHtml(){
        return getHippoHtml(HTML_NODEPATH);
    }
    
    public void setHtml(String html) throws ContentNodeBindingException{
        this.html = html;
    }
    
    public void addHtml(String body) throws RepositoryException {
        javax.jcr.Node n = this.getNode().addNode(HTML_NODEPATH, "hippostd:html");
        n.setProperty("hippostd:content", body);
    }
    
   /**
    * to be overridden by beans having a date. By having it in the basebean as well, 
    * the jsp el can always try a var.date without getting an expression language exception
    * @return Calendar obj of the bean or <code>null</code>
    */
    public Calendar getDate() {
        return null;
    }
    
    public boolean isPublished() {
        String[] availability = getProperty("hippo:availability");
        return ArrayUtils.contains(availability, "live");
    }
    
    /**
     * to be overridden by beans having an gallery image. By having it in the basebean as well, 
     * the jsp el can always try a var.image without getting an expression language exception
     * @return
     */
    public HippoGalleryImageSetBean getImage(){
        return null;
    }
    
    public boolean bind(Object content, javax.jcr.Node node) throws ContentNodeBindingException {
        try {
            BaseBean bean =  (BaseBean) content;
            node.setProperty("demosite:title", bean.getTitle());
            node.setProperty("demosite:summary", bean.getSummary());
            
            if(this.html != null) {
                if(node.hasNode(HTML_NODEPATH)) {
                    javax.jcr.Node htmlNode = node.getNode(HTML_NODEPATH);
                    if(!htmlNode.isNodeType("hippostd:html")) {
                        throw new ContentNodeBindingException("Expected html node of type 'hippostd:html' but was '"+htmlNode.getPrimaryNodeType().getName()+"'");
                    }
                    htmlNode.setProperty("hippostd:content", html);
                } else {
                    javax.jcr.Node html =  node.addNode(HTML_NODEPATH, "hippostd:html");
                    html.setProperty("hippostd:content", html);
                }
            }
        } catch (Exception e) {
            throw new ContentNodeBindingException(e);
        }
        
        return true;
    }
}
