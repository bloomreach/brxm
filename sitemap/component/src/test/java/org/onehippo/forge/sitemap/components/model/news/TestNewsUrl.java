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

import org.junit.Test;
import org.onehippo.forge.sitemap.generator.NewsSitemapGenerator;
import org.onehippo.forge.sitemap.components.model.news.info.AccessType;
import org.onehippo.forge.sitemap.components.model.news.info.Genre;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 */
public class TestNewsUrl {

    @Test
    public void generatesXml() throws Exception {
        NewsUrl newsUrl = new NewsUrl();
        newsUrl.setLoc("https://www.nu.nl");
        newsUrl.setLastmod(new GregorianCalendar());
        newsUrl.setPublication("nu.nl", "nl");
        newsUrl.setAccess(AccessType.SUBSCRIPTION);
        newsUrl.addGenre(Genre.BLOG);
        newsUrl.addGenre(Genre.PRESS_RELEASE);
        Calendar pubDate = new GregorianCalendar(2012, 1, 29);
        newsUrl.setPublicationDate(pubDate);
        newsUrl.setTitle("A great & 'new' \"sitemap\" <forge> plugin!");
        newsUrl.setGeoLocations("Amsterdam, Netherlands, Europe");
        newsUrl.addKeyword("Hippo");
        newsUrl.addKeyword("CMS");
        newsUrl.addKeyword("Entertainment");

        NewsUrlset newsUrlset = new NewsUrlset();
        newsUrlset.addUrl(newsUrl);

        System.out.println(NewsSitemapGenerator.toString(newsUrlset));
    }

}
