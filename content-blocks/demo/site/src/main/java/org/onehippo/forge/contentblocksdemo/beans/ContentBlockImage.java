package org.onehippo.forge.contentblocksdemo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;


@Node(jcrType="contentblocksdemo:imageblock")
public class ContentBlockImage extends HippoCompound{

    public String getType(){
        return "image";
    }

    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("contentblocksdemo:image", HippoGalleryImageSetBean.class);
    }



}
