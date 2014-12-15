package org.onehippo.forge.contentblocksdemo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoHtml;


@Node(jcrType="contentblocksdemo:textblock")
public class ContentBlockText extends HippoCompound{

    public String getType(){
        return "text";
    }

    public HippoHtml getText(){
        return getHippoHtml("contentblocksdemo:text");
    }



}
