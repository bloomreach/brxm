package org.onehippo.forge.contentblocksdemo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

import java.util.List;

@Node(jcrType="contentblocksdemo:providercompounddocument")
public class ProviderCompoundDocument extends BaseDocument{
    
    public String getTitle() {
        return getProperty("contentblocksdemo:title");
    }

    public String getSummary() {
        return getProperty("contentblocksdemo:summary");
    }
    
    public HippoHtml getHtml(){
        return getHippoHtml("contentblocksdemo:body");
    }

    public List<?> getContentBlocks(){
        return getChildBeansByName("contentblocksdemo:contentblocksitem");
    }

}
