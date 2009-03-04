package org.hippoecm.hst.sitemenu;

import java.io.Serializable;
import java.util.List;

public interface SiteMenu extends Serializable{

    String getName();
    boolean isSelected();
    List<SiteMenuItem> getSiteMenuItems();
}
