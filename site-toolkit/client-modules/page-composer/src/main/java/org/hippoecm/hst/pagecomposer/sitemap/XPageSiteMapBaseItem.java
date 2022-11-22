/*
 * Copyright 2022 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.hst.pagecomposer.sitemap;

public class XPageSiteMapBaseItem {

    private String name;

    private String pageTitle;

    private String pathInfo;

    private String absoluteJcrPath;


    public String getName() {
        return name;
    }


    public void setName(final String name) {
        this.name = name;
    }


    public String getPathInfo() {
        return pathInfo;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(final String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    /**
     * @return absolute jcr path to the handle node
     */
    public String getAbsoluteJcrPath() {
        return absoluteJcrPath;
    }

    public void setAbsoluteJcrPath(final String absoluteJcrPath) {
        this.absoluteJcrPath = absoluteJcrPath;
    }

}
