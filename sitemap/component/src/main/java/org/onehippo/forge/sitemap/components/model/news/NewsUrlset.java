/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.sitemap.components.model.news;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement(name = "urlset")
public class NewsUrlset
{
    // Google news site map can contain a maximum of 1000 entries
    private static final int MAXIMUM_ENTRIES = 1000;

    private final List<NewsUrl> urls;

    public NewsUrlset() {
        urls = new ArrayList<NewsUrl>();
    }

    public void addUrl(final NewsUrl newsUrl) {
        if (urls.size() < MAXIMUM_ENTRIES) {
            urls.add(newsUrl);
        } else {
            throw new IllegalStateException("NewsUrlset can only contain " + MAXIMUM_ENTRIES + " entries.");
        }
    }

    public boolean contains(NewsUrl url) {
        return urls.contains(url);
    }

    @XmlElement(name = "url")
    public List<NewsUrl> getUrls() {
        return Collections.unmodifiableList(urls);
    }
}
