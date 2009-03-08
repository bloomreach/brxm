package org.hippoecm.hst.core.linking;

import java.io.Serializable;

public interface ResolvedLocationMapTreeItem extends Serializable{
    
    public String getHstSiteMapItemId();
    public String getPath();
}
