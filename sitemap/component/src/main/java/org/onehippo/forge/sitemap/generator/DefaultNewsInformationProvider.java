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
package org.onehippo.forge.sitemap.generator;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.forge.sitemap.components.NewsInformationProvider;
import org.onehippo.forge.sitemap.components.model.news.info.AccessType;
import org.onehippo.forge.sitemap.components.model.news.info.Genre;
import org.onehippo.forge.sitemap.components.model.news.info.Publication;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * A default news information provider that works by returning a few standard-hippo values for the required news sitemap
 * elements
 */
public class DefaultNewsInformationProvider extends DefaultUrlInformationProvider implements NewsInformationProvider {

    private Publication publication;
    private String publicationDateProperty;

    @SuppressWarnings({"unused"})
    private DefaultNewsInformationProvider() {} // Hide default constructor

    public DefaultNewsInformationProvider(Publication publication, String publicationDateProperty) {
        this.publication = publication;
        this.publicationDateProperty = publicationDateProperty;
    }

    /**
     * Returns null for this optional field
     */
    public String getGeoLocations(HippoBean hippoBean) {
        return null;
    }

    /**
     * Returns null for this optional field
     */
    public String getTitle(HippoBean hippoBean) {
        return null;
    }

    /**
     * Returns null for this optional field
     */
    public AccessType getAccess(HippoBean hippoBean) {
        return null;
    }

    /**
     * Returns the publication as specified in the constructor
     */
    public Publication getPublication(HippoBean hippoBean) {
        return publication;
    }

    /**
     * Returns the value of the publicationDateProperty as specified in the constructor
     */
    public Calendar getPublicationDate(HippoBean hippoBean) {
        return hippoBean.getProperty(publicationDateProperty);
    }

    /**
     * Returns an empty {@link List} for this optional field
     */
    public List<Genre> getGenres(HippoBean hippoBean) {
        return Collections.emptyList();
    }

    /**
     * Returns an empty {@link List} for this optional field
     */
    public List<String> getKeywords(HippoBean hippoBean) {
        return Collections.emptyList();
    }

    /**
     * Returns the publicationDateProperty as specified in the constructor
     */
    public String getPublicationDateProperty() {
        return publicationDateProperty;
    }
}
