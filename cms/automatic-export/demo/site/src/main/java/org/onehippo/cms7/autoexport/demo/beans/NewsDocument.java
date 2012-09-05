package org.onehippo.cms7.autoexport.demo.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="autoexportdemo:newsdocument")
public class NewsDocument extends BaseDocument{

    public Calendar getDate() {
        return getProperty("autoexportdemo:date");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("autoexportdemo:body");    
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("autoexportdemo:image", HippoGalleryImageSetBean.class);
    }

    public String getSummary() {
        return getProperty("autoexportdemo:summary");
    }

    public String getTitle() {
        return getProperty("autoexportdemo:title");
    }

}
