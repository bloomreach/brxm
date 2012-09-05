package org.onehippo.cms7.autoexport.demo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="autoexportdemo:textdocument")
public class TextDocument extends BaseDocument{
    
    public HippoHtml getHtml(){
        return getHippoHtml("autoexportdemo:body");    
    }

    public String getSummary() {
        return getProperty("autoexportdemo:summary");
    }
 
    public String getTitle() {
        return getProperty("autoexportdemo:title");
    }

}
