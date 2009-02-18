package org.hippoecm.hst.configuration;


/**
 * Interface for inverted HstSiteMapItem tree where the tree is driven by the relativeContentPath's 
 * of all HstSiteMapItem's instead of the tree based on the HstSiteMapItem hierarchy.
 * 
 */
public interface LocationMapTree {

   LocationMapTreeItem getTreeItem(String name);

   /**
    * 
    * @param path
    * @return best matching LocationMapTreeItem and null if no matching one is found
    */
   LocationMapTreeItem find(String path);
   
   String getCanonicalSiteContentPath();
}
