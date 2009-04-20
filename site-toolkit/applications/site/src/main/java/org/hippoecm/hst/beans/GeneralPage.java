package org.hippoecm.hst.beans;

import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

public class GeneralPage extends HippoDocument {
    protected String title;

    public String getTitle() {
        return getProperty("testproject:title");
    }
    
    public HippoHtml getBody(){
        return getHippoHtml("testproject:body");
    }
    
}
