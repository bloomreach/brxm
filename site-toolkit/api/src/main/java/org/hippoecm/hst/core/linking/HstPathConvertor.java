/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.linking;

import java.io.Serializable;

import org.hippoecm.hst.configuration.HstSite;

/**
 * HstPathConvertor implementations should be able to rewrite a path, which in general is the path location
 * of a jcr Node to a link. A link is generally composed of two parts: The first part is the path to match a SiteMapItem.
 * A SiteMapItem has an attr relativeContentPath, indicating its 'repository content start path'. This is wrt to the content
 * entry path of the HstSite (in other words, the root content of this subsite). The second part of the
 * link is with respect to this relativeContentPath. 
 * 
 * Example: 
 * 
 * 0: HstSite starts its content in /content/myproject
 * 1: Suppose we have a SiteMapItem which can be accessed through the path : /news/recentsports
 * 2: The SiteMapItem has a relativeContentPath that is: /news/sports/2009
 * 3: The path we would like to find a link of is: /content/myproject/news/sports/2009/10/04/tennis/federer-vs-nadal
 * 
 * The link we would want to get is as follows:
 * 
 * 1: as the HstSite starts in /content/myproject, we can skip this part of the link (note that if that path did start with
 * something else, we would have to search for a link in a different site)
 * 2: the part '/news/sports/2009' is accounted for (and replaced) by the relativeContentPath of the SiteMapItem
 * 3: the /10/04/tennis/federer-vs-nadal is the remainder.
 * 
 * So, the path
 * 
 * /content/myproject/news/sports/2009/10/04/tennis/federer-vs-nadal
 * 
 * can be translated to the link:
 * 
 * /news/recentsports/10/04/tennis/federer-vs-nadal
 * 
 * Note, that the HstPathConvertor implementations should try to find the 'best' matching SiteMapItem. 'Best' can be seen
 * as the most specific relativeContentPath. So, in the example above, if we would also have a SiteMapItem that has a 
 * relativeContentPath '/news/sports/2009/10/04/tennis' and can be accessed through the path /news/oct_4th_tennis, then
 * the path above would be better translated to:
 * 
 * /news/oct_4th_tennis/federer-vs-nadal
 */
public interface HstPathConvertor {

    /**
     * @param path
     * @param hstSite
     * @return ConversionResult object if the path is convertable with this hstSite. If the path can not be 
     * converted within this hstSite, null is returned
     */
    ConversionResult convert(String path, HstSite hstSite);
    
    /**
     * Lighweight serializable object as it is likely that is must be cached & that there will 
     * be many of them
     *
     */
    interface ConversionResult extends Serializable{
        
        /**
         * @return String path relative to the relativeContentPath of the HstSiteMapItem. For a perfect match
         * it can be "" or null
         */
        String getPath();
        
        String getSiteMapItemId();
        
    }
}
