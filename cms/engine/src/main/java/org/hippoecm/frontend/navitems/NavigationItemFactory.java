/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.navitems;

/**
 * Factory interface for creating instances of Navigation Items
 */
public interface NavigationItemFactory {

    /**
     * Create a new {@link NavigationItem} for the perspective with the given appIframe url.
     *
     * @param perspectiveClassName fully qualified class name of a perspective
     * @param appIframeUrl         the url of the iframe that the navigation items must belong to.
     * @return a new instance
     */
    NavigationItem newInstance(String perspectiveClassName, String appIframeUrl);

}
