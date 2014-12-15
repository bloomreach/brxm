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

import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.news.info.AccessType;
import org.onehippo.forge.sitemap.components.model.news.info.Genre;
import org.onehippo.forge.sitemap.components.model.news.info.NewsUrlInformation;
import org.onehippo.forge.sitemap.components.model.news.info.Publication;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Calendar;

/**
 */
@XmlRootElement(name = "url")
@XmlAccessorType(value = XmlAccessType.NONE)
@SuppressWarnings({"unused"})
public class NewsUrl extends Url {

    private NewsUrlInformation news;

    @XmlElement(namespace = "http://www.google.com/schemas/sitemap-news/0.9")
    public NewsUrlInformation getNews() {
        return news;
    }

    public void setPublication(String name, String language) {
        news.setPublication(new Publication(name, language));
    }

    public Publication getPublication() {
        return news.getPublication();
    }

    public AccessType getAccess() {
        return news.getAccess();
    }

    public void setAccess(AccessType access) {
        news.setAccess(access);
    }

    public void addGenre(Genre genre) {
        news.addGenre(genre);
    }

    public void setPublicationDate(Calendar date) {
        news.setPublicationDate(date);
    }

    public Calendar getPublicationDate() {
        return news.getPublicationDate();
    }

    public NewsUrl() {
        super();
        news = new NewsUrlInformation();
    }

    public void setTitle(String title) {
        news.setTitle(title);
    }

    public String getTitle() {
        return news.getTitle();
    }

    public String getGeoLocations() {
        return news.getGeoLocations();
    }

    public void setGeoLocations(String geoLocations) {
        news.setGeoLocations(geoLocations);
    }

    public void addKeyword(String keyword) {
        news.addKeyword(keyword);
    }
}
