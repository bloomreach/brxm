package org.onehippo.forge.polldemo.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="polldemo:newsdocument")
public class NewsDocument extends BaseDocument{

    public Calendar getDate() {
        return getProperty("polldemo:date");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("polldemo:body");    
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("polldemo:image", HippoGalleryImageSetBean.class);
    }

    public String getSummary() {
        return getProperty("polldemo:summary");
    }

    public String getTitle() {
        return getProperty("polldemo:title");
    }

}
