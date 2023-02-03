/*
 * Copyright 2009-2023 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.sitemap.components.beans;

import java.io.Serializable;


public class SiteMapItem implements Serializable
{
    
    private static final long serialVersionUID = 1L;
    
    private String title;
    private String url;
    
    /**
     * @param title
     * @param url
     */
    public SiteMapItem(String title, String url)
    {
        this.title = title;
        this.url = url;
    }

    /**
     * @return the title
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return the url
     */
    public String getUrl()
    {
        return url;
    }
    /**
     * @param title the title to set
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * @param url the url to set
     */
    public void setUrl(String url)
    {
        this.url = url;
    }
    
}
