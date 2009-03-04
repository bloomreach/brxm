package org.hippoecm.hst.sitemenu;

import java.io.Serializable;
import java.util.List;

import org.hippoecm.hst.core.linking.HstLink;

public interface SiteMenuItem extends Serializable{
    
    String getName();
    
    List<SiteMenuItem> getChildMenuItems();
    
    HstLink getHstLink();
    
    boolean isSelected();
}
