/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.sitemenu;

import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

/**
 * The HstSiteMenuItem's is a tree of HstSiteMenuItem. The root item does not have a parent. 
 */
public interface HstSiteMenuItem extends CommonMenuItem {
   
    /**
     * 
     * @return all direct child SiteMenuItem's of this item
     */
    List<HstSiteMenuItem> getChildMenuItems();
    
    /**
     * 
     * @return parent <code>HstSiteMenuItem</code> or <code>null</code> if it is a root item 
     */
    HstSiteMenuItem getParentItem();
    
    /**
     * 
     * @return the container <code>HstSiteMenu</code> of this <code>HstSiteMenuItem</code>
     */
    HstSiteMenu getHstSiteMenu();
    
    
    /**
     * A HstSiteMenuItem can contain a Map of parameters. A parameter from this Map can be accessed through this method. If it is not present, <code>null</code>
     * will be returned. The parameters are fetched from the {@link HstSiteMenuItemConfiguration#getParameter(String)}, but in the value possible property placeholders in the 
     * value are replaced by the wildcards from {@link HstSiteMapItem} that was matched. When there are property placeholders that can not be resolved, the value
     * is set to <code>null</code>
     * 
     * Parameters are inherited from ancestor HstSiteMenuItem's. When this HstSiteMenuItem configures the same parameter as an ancestor, the
     * value from the ancestor is overwritten. 
     * 
     * @param name the name of the parameter
     * @return the value of the parameter or <code>null</code> when not present or has unresolved property placeholders
     */
    String getParameter(String name);
    
    /**
     * The value of the local (no parameters inherited from ancestor items) parameter with possible property placeholders substituted
     * @see #getParameter(String) , only this method returns parameters without inheritance
     * @param name the name of the parameter
     * @return the value of the parameter or <code>null</code> when not present or has unresolved property placeholders
     */
    String getLocalParameter(String name);
    
    /**
     * Parameters are inherited from ancestor sitemenu items. When this sitemenu item configures the same parameter as an ancestor, the
     * value from the ancestor is overwritten. 
     * 
     * @see #getParameter(String) , only now the entire parameters map is returned.
     * @return the Map of parameters contained in this <code>HstSiteMenu</code>. If no parameters present, and empty map is returned
     */
    Map<String, String> getParameters();
    
    /**
     * @see #getParameters() , only this method returns parameters without inheritance
     * @return the Map of parameters contained in this <code>HstSiteMapItem</code>. If no parameters present, and empty map is returned
     */
    Map<String, String> getLocalParameters();
}
