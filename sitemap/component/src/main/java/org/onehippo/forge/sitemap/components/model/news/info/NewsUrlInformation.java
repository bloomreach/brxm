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

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 */
@XmlRootElement(name = "news")
@XmlAccessorType(value = XmlAccessType.PUBLIC_MEMBER)
@XmlType(propOrder = {
        "publication",
        "accessAsString",
        "genres",
        "publicationDateInW3CFormat",
        "title",
        "geoLocations",
        "keywords"
})
@SuppressWarnings({"unused"})
public class NewsUrlInformation {
    private Publication publication;
    private AccessType access;
    private final List<Genre> genres;
    private Calendar publicationDate;
    private String title;
    private String geoLocations;
    private final List<String> keywords;

    public NewsUrlInformation() {
        publication = null;
        access = null;
        genres = new ArrayList<Genre>();
        publicationDate = null;
        title = null;
        geoLocations = null;
        keywords = new ArrayList<String>();
    }

    @XmlElement(name = "geo_locations")
    public String getGeoLocations() {
        return geoLocations;
    }

    public void setGeoLocations(String geoLocations) {
        this.geoLocations = geoLocations;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @XmlElement(required = true)
    public Publication getPublication() {
        try {
            return (Publication) publication.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone publication", e);
        }
    }

    public void setPublication(Publication publication) {
        try {
            this.publication = (Publication) publication.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Cannot clone publication", e);
        }
    }

    @XmlTransient
    public AccessType getAccess() {
        return access;
    }

    @XmlElement(name = "access")
    public String getAccessAsString() {
        if (access == null) {
            return null;
        }
        return access.toString();
    }

    public void setAccess(AccessType access) {
        this.access = access;
    }

    public void addGenre(Genre genre) {
        genres.add(genre);
    }

    @XmlElement(name = "genres")
    public String getGenres() {
        if (genres.isEmpty()) {
            return null; // Should not render an empty tag
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Genre genre : genres) {
            sb.append(genre.toString());
            if (i < genres.size()) {
                sb.append(", ");
            }
            i++;
        }
        return sb.toString();
    }

    @XmlTransient
    public Calendar getPublicationDate() {
        return (Calendar) publicationDate.clone();
    }

    public void setPublicationDate(Calendar publicationDate) {
        this.publicationDate = (Calendar) publicationDate.clone();
    }

    @XmlElement(name = "publication_date", required = true)
    public String getPublicationDateInW3CFormat() {
        if (publicationDate == null) {
            return null;
        }
        FastDateFormat dateFormatter = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
        return dateFormatter.format(publicationDate);
    }

    public void addKeyword(String keyword) {
        keywords.add(keyword);
    }

    @XmlElement(name = "keywords")
    public String getKeywords() {
        if (keywords.isEmpty()) {
            return null; // Should not render an empty tag
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (String keyword : keywords) {
            sb.append(keyword);
            if (i < keywords.size()) {
                sb.append(", ");
            }
            i++;
        }
        return sb.toString();
    }

}
