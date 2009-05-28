package org.hippoecm.hst.utilities;

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;

public class SimpleSearchResultExample {

    private int size;
    private List<HippoBean> hits;
    private String query;
    
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<HippoBean> getHits(){
        return this.hits;
    }
    
    public void setHits(List<HippoBean> hits) {
        this.hits = hits;
    }
    
    public int getSize(){
        return this.size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }

}
