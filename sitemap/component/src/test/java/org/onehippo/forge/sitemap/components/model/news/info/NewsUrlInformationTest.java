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
package org.onehippo.forge.sitemap.components.model.news.info;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Wouter Danes
 */
public class NewsUrlInformationTest {
    @Test
    public void testGetGenres() throws Exception {
        NewsUrlInformation newsUrlInformation = new NewsUrlInformation();

        // Should return null if the list is empty
        Assert.assertEquals(null, newsUrlInformation.getGenres());

        newsUrlInformation.addGenre(Genre.BLOG);

        // Should not add a comma if there is only one item
        Assert.assertEquals("Blog", newsUrlInformation.getGenres());

        newsUrlInformation.addGenre(Genre.PRESS_RELEASE);

        // Should return a comma seperated list and order genres from first to last added
        Assert.assertEquals("Blog, PressRelease", newsUrlInformation.getGenres());
    }

    @Test
    public void testGetKeywords() throws Exception {
        NewsUrlInformation newsUrlInformation = new NewsUrlInformation();

        // Should return null if the list is empty
        Assert.assertEquals(null, newsUrlInformation.getKeywords());

        newsUrlInformation.addKeyword("CMS");
        // Should not add a comma if there is only one item
        Assert.assertEquals("CMS", newsUrlInformation.getKeywords());

        newsUrlInformation.addKeyword("Entertainment");
        // Should return a comma seperated list and order keywords from first to last added
        Assert.assertEquals("CMS, Entertainment", newsUrlInformation.getKeywords());
    }
}
