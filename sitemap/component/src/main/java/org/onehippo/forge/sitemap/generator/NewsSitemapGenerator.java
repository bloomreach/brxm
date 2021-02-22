/*
 * Copyright 2012-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.sitemap.components.NewsInformationProvider;
import org.onehippo.forge.sitemap.components.model.SiteMapCharacterEscapeHandler;
import org.onehippo.forge.sitemap.components.model.news.NewsUrl;
import org.onehippo.forge.sitemap.components.model.news.NewsUrlset;
import org.onehippo.forge.sitemap.components.model.news.info.Genre;
import org.onehippo.forge.sitemap.components.model.news.info.Publication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@SuppressWarnings({"unused"})
public class NewsSitemapGenerator extends SitemapGenerator {

    private final NewsInformationProvider newsInformationProvider;

    private static final Logger LOG = LoggerFactory.getLogger(NewsSitemapGenerator.class);

    public NewsSitemapGenerator(final HstRequestContext requestContext, final String publicationName,
                                final String publicationLanguage,
                                final String publicationDateProperty, final ObjectConverter objectConverter) {
        super(requestContext);
        newsInformationProvider = new DefaultNewsInformationProvider(
                new Publication(publicationName, publicationLanguage),
                publicationDateProperty
        );
    }

    public NewsSitemapGenerator(final HstRequestContext requestContext,
                                final NewsInformationProvider newsInformationProvider,
                                final ObjectConverter objectConverter) {
        super(requestContext);
        this.newsInformationProvider = newsInformationProvider;
    }

    public NewsUrlset createNewsSitemap(final HstQuery query) {
        //TODO: Cap returned amt of docs based on max possible in news site map
        HstQueryResult result;
        try {
            result = query.execute();
        } catch (QueryException e) {
            LOG.error("News sitemap cannot be created que to a problem in the query", e);
            throw new IllegalArgumentException("passed query resulted in an Exception", e);
        }

        NewsUrlset urlset = new NewsUrlset();
        HippoBeanIterator resultsIterator = result.getHippoBeans();

        while (resultsIterator.hasNext()) {
            HippoBean bean = resultsIterator.nextHippoBean();
            if (bean == null) {
                // If this node cannot be mapped to a HippoBean, then jump to the next bean
                LOG.debug("Skipping node, because it cannot be mapped to a hippo bean");
                continue;
            }
            NewsUrl url = createNewsUrlForHippoBean(bean);
            urlset.addUrl(url);
        }

        return urlset;
    }

    private NewsUrl createNewsUrlForHippoBean(HippoBean bean) {
        NewsUrl url = new NewsUrl();
        url.setLoc(newsInformationProvider.getLoc(bean, getRequestContext()));
        url.setGeoLocations(newsInformationProvider.getGeoLocations(bean));
        url.setChangeFrequency(newsInformationProvider.getChangeFrequency(bean));
        url.setLastmod(newsInformationProvider.getLastModified(bean));
        url.setPriority(newsInformationProvider.getPriority(bean));
        url.setAccess(newsInformationProvider.getAccess(bean));
        Publication publication = newsInformationProvider.getPublication(bean);
        url.setPublication(publication.getName(), publication.getLanguage());
        url.setPublicationDate(newsInformationProvider.getPublicationDate(bean));
        url.setTitle(newsInformationProvider.getTitle(bean));
        List<Genre> genres = newsInformationProvider.getGenres(bean);
        if (genres != null) {
            for (Genre genre : genres) {
                url.addGenre(genre);
            }
        }
        List<String> keywords = newsInformationProvider.getKeywords(bean);
        if (keywords != null) {
            for (String keyword : keywords) {
                url.addKeyword(keyword);
            }
        }
        return url;
    }

    public static String toString(final NewsUrlset urls) {
        String output;
        try {
            JAXBContext jc = JAXBContext.newInstance(NewsUrlset.class);
            Marshaller m = jc.createMarshaller();

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            TransformerHandler handler = factory.newTransformerHandler();
            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
            handler.setResult(new StreamResult(out));

            m.marshal(urls, new SiteMapCharacterEscapeHandler(handler));
            output = out.toString();
        } catch (JAXBException | TransformerConfigurationException e) {
            throw new IllegalStateException("Cannot marshal the NewsUrlset to an XML string", e);
        }
        return output;
    }
}
