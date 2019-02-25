/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You
 *  may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.configuration.internal;

import java.util.List;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * Internal only api to represent {@link HstSiteMapItem} by joining with {@link CanonicalInfo} in sitemapitem matcher
 */
public interface InternalHstSiteMapItem extends HstSiteMapItem, CanonicalInfo {

    /**
     * Internal only! Not an api.
     * 
     * @param pathElementValue any value splitted by slash in path like /path/to/element
     * @param excludedSiteList sites to be excluded from traverse
     * @return
     */
    InternalHstSiteMapItem getWildCardPatternChild(String pathElementValue,
            List<InternalHstSiteMapItem> excludedSiteList);

    /**
     * Internal only! Not an api.
     * 
     * @param pathElements list of path elements to traverse
     * @param position of the path element
     * @param excludedSiteList sites to be excluded from traverse
     * @return
     */
    InternalHstSiteMapItem getAnyPatternChild(String[] pathElements, int position,
            List<InternalHstSiteMapItem> excludedSiteList);

    /**
     * Internal only! Not an api.
     * Tries to match a path element value with a prefix and a postfix.
     * 
     * @param pathElementValue any value splitted by slash in path like /path/to/element
     * @param prefix of the wildcard
     * @param postfix of the wildcard
     * @return returns true if the value of the path element matches with prefix and postfix
     */
    boolean patternMatch(String pathElementValue, String prefix, String postfix);

    /**
     * 
     * @return prefix of the wildcard definition
     */
    String getWildCardPrefix();

    /**
     * 
     * @return postfix of the wildcard definition
     */
    String getWildCardPostfix();

}