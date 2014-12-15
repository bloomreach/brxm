package org.onehippo.forge.contentblocksdemo.beans;

import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="contentblocksdemo:contentblocksdocument")
public class ContentBlocksDocument extends BaseDocument{

    public String getTitle() {
        return getProperty("contentblocksdemo:title");
    }
    
    public String getSummary() {
        return getProperty("contentblocksdemo:summary");
    }
    
    public Calendar getDate() {
        return getProperty("contentblocksdemo:date");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("contentblocksdemo:body");
    }

    public List<?> getVideos() {
        return getChildBeans("contentblocksdemo:videoblock");
    }

    public List<?> getImages() {
        return getChildBeans("contentblocksdemo:imageblock");
    }

    public List<?> getTexts() {
        return getChildBeans("contentblocksdemo:textblock");
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("contentblocksdemo:image", HippoGalleryImageSetBean.class);
    }


}
