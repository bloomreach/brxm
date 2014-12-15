package org.onehippo.forge.polldemo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="polldemo:textdocument")
public class TextDocument extends BaseDocument{
    
    public HippoHtml getHtml(){
        return getHippoHtml("polldemo:body");    
    }

    public String getSummary() {
        return getProperty("polldemo:summary");
    }
 
    public String getTitle() {
        return getProperty("polldemo:title");
    }

}
