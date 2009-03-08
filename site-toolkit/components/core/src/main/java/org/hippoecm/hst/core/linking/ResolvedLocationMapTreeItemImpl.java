package org.hippoecm.hst.core.linking;

public class ResolvedLocationMapTreeItemImpl implements ResolvedLocationMapTreeItem{

    private static final long serialVersionUID = 1L;
    
    private String path;
    private String hstSiteMapItemId;
    
    public ResolvedLocationMapTreeItemImpl(String path, String hstSiteMapItemId){
        this.path = path;
        this.hstSiteMapItemId = hstSiteMapItemId;
    }
    
    public String getHstSiteMapItemId() {
        return hstSiteMapItemId;
    }

    public String getPath() {
        return path;
    }

}
