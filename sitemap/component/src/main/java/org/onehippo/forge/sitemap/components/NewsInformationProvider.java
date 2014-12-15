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
package org.onehippo.forge.sitemap.components;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.onehippo.forge.sitemap.components.model.news.info.AccessType;
import org.onehippo.forge.sitemap.components.model.news.info.Genre;
import org.onehippo.forge.sitemap.components.model.news.info.Publication;

import java.util.Calendar;
import java.util.List;

/**
 * An interface implemented by classes that can resolve news sitemap item properties for HippoBeans
 * Classes that implement this interface can be passed to the SitemapGenerator to be used in creating a news site map.
 *
 * @author Wouter Danes
 */
public interface NewsInformationProvider extends UrlInformationProvider{

    /**
     * Returns a comma seperated list of geo locations for this sitemap item, from more to less specific, f.ex:
     * Amsterdam, Netherlands, Europe
     * @param hippoBean the document to generate the geo locations value for
     * @return {@link String} containing a comma seperated list of geo locations
     */
    String getGeoLocations(HippoBean hippoBean);

    /**
     * Returns the title of the document for the google news sitemap
     * @param hippoBean the document to generate the title value for
     * @return {@link String} containing the Title of this news document
     */
    String getTitle(HippoBean hippoBean);

    /**
     * Returns the access type of this news document. For a list of supported values, check the {@link AccessType} enum.
     * @param hippoBean the document to return the {@link AccessType} for
     * @return the {@link AccessType} for this news document
     */
    AccessType getAccess(HippoBean hippoBean);

    /**
     * Returns the {@link Publication} for this news document
     * @param hippoBean the document to return the {@link Publication} for
     * @return the {@link Publication} for this news document
     */
    Publication getPublication(HippoBean hippoBean);

    /**
     * Returns the publication date for the passed {@link HippoBean}.
     * @param hippoBean the document to return the publication date for
     * @return {@link Calendar} representing the publication date of this news document
     */
    Calendar getPublicationDate(HippoBean hippoBean);

    /**
     * Returns the {@link Genre}s for the passed {@link HippoBean}
     * @param hippoBean the document to return the {@link Genre}s for. For possible values check the enum.
     * @return {@link List} containing the applicable {@link Genre}s.
     */
    List<Genre> getGenres(HippoBean hippoBean);

    /**
     * Returns a comma seperated list of keywords that are applicable to the passed {@link HippoBean}
     * @param hippoBean the document to return the keywords for.
     * @return a {@link String} containing a comma seperated list of keywords
     */
    List<String> getKeywords(HippoBean hippoBean);

    /**
     * Returns the publication date property that will be used to resolve the age of documents. documents old than 48
     * hours should not be included in the news sitemap
     * @return the name of the property of type jcr:date that contains the publication date of a news document
     */
    String getPublicationDateProperty();

}
