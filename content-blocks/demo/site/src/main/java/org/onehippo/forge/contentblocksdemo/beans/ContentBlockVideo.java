package org.onehippo.forge.contentblocksdemo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoCompound;

@Node(jcrType="contentblocksdemo:videoblock")
public class ContentBlockVideo extends HippoCompound{

    public String getType(){
        return "video";
    }

    public String getVideo() {
        return getProperty("contentblocksdemo:video");
    }
}
