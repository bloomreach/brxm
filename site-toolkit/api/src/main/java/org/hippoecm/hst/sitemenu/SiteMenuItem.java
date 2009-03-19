package org.hippoecm.hst.sitemenu;

import java.io.Serializable;
import java.util.List;

import org.hippoecm.hst.core.linking.HstLink;

/**
 * Interface for the implementation of a SiteMenuItem. Note that implementations are recommended to return an unmodifiable
 * (Sorted)List for the {@link #getChildMenuItems()}
 *
 */
public interface SiteMenuItem extends Serializable{
    /**
     * 
     * @return the name of this SiteMenuItem
     */
    String getName();
    
    /**
     * 
     * @return all direct child SiteMenuItem's of this item
     */
    List<SiteMenuItem> getChildMenuItems();
    
    /**
     * 
     * @return a HstLink that contains a proper link for this SiteMenuItem
     */
    HstLink getHstLink();
    
    /**
     * @return <code>true</code> is the SiteMenuItem is selected
     */
    boolean isSelected();
}
