package org.onehippo.cms7.essentials.site.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="hippoplugins:newsdocument")
public class NewsDocument extends BaseDocument{

    
    public String getSummary() {
        return getProperty("hippoplugins:summary");
    }
    
    public Calendar getDate() {
        return getProperty("hippoplugins:date");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("hippoplugins:body");    
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("hippoplugins:image", HippoGalleryImageSetBean.class);
    }


}
