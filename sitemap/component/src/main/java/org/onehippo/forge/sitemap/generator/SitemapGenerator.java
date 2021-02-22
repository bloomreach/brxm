/*
 *  Copyright 2010-2021 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.forge.sitemap.generator;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

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

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.onehippo.forge.sitemap.components.beans.SiteMap;
import org.onehippo.forge.sitemap.components.beans.SiteMapItem;
import org.onehippo.forge.sitemap.components.model.ChangeFrequency;
import org.onehippo.forge.sitemap.components.model.SiteMapCharacterEscapeHandler;
import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.Urlset;

public class SitemapGenerator {

    private final HstRequestContext requestContext;

    private static final String UNUSED = "unused";

    public SitemapGenerator(HstRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @SuppressWarnings({UNUSED})
    public String createSitemap(final HstSiteMenu sitemenu, final int maxDepth) {
        Urlset urls = new Urlset();
        addSitemapItems(sitemenu, urls, maxDepth);
        return toString(urls);
    }

    public String createSitemap(final HstSiteMenus sitemenus, final int maxDepth) {
        Urlset urls = new Urlset();
        for (HstSiteMenu menu : sitemenus.getSiteMenus().values()) {
            addSitemapItems(menu, urls, maxDepth);
        }
        return toString(urls);
    }

    @SuppressWarnings({UNUSED})
    protected void addSitemapItems(final HstSiteMenus sitemenus, final Urlset urls, final int maxDepth) {
        for (HstSiteMenu menu : sitemenus.getSiteMenus().values()) {
            addSitemapItems(menu, urls, maxDepth);
        }
    }

    protected void addSitemapItems(final HstSiteMenu sitemenu, final Urlset urls, final int maxDepth) {
        Calendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        for (HstSiteMenuItem item : sitemenu.getSiteMenuItems()) {
            addMenuItem(item, urls, 1, maxDepth, now);
        }
    }

    protected void addMenuItem(final HstSiteMenuItem item, final Urlset urlSet, final int depth, final int maxDepth,
                               final Calendar defaultLastModifedDate) {
        Url url = new Url();
        if (item.getHstLink() != null) {
            url.setChangeFrequency(ChangeFrequency.DAILY);
            url.setLastmod(defaultLastModifedDate);
            url.setLoc(item.getHstLink().toUrlForm(requestContext, true));
            url.setPriority(new BigDecimal("1.0"));
            urlSet.getUrls().add(url);
        }

        if (depth < maxDepth) {
            for (HstSiteMenuItem childItem : item.getChildMenuItems()) {
                addMenuItem(childItem, urlSet, depth + 1, maxDepth, defaultLastModifedDate);
            }
        }
    }

    public static String toString(final Urlset urls) {
        String output;
        try {
            JAXBContext jc = JAXBContext.newInstance(Urlset.class, Url.class);
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
            throw new IllegalStateException("Cannot marshal the Urlset to an XML string", e);
        }
        return output;
    }

    public static SiteMap getSitemapView(final HstSiteMenus sitemenus, final int maxDepth) {
        SiteMap map = new SiteMap();
        for (HstSiteMenu menu : sitemenus.getSiteMenus().values()) {
            addSitemapItems(menu, map, maxDepth);
        }
        return map;
    }

    public static void addSitemapItems(final HstSiteMenu sitemenu, final SiteMap map, final int maxDepth) {
        for (HstSiteMenuItem item : sitemenu.getSiteMenuItems()) {
            addMenuItem(item, map, 1, maxDepth, null);
        }
    }

    public static void addMenuItem(final HstSiteMenuItem item, final SiteMap map, final int depth, final int maxDepth,
                                   final String parent) {

        SiteMapItem mapItem = new SiteMapItem(item.getName(), item.getHstLink().getPath());
        String newParent;
        if (parent != null && map.getItems().containsKey(parent)) {
            map.getItems().get(parent).add(mapItem);
            newParent = item.getName();
        } else {
            List<SiteMapItem> sitemMapItems = new ArrayList<SiteMapItem>();
            sitemMapItems.add(mapItem);
            map.getItems().put(item.getName(), sitemMapItems);
            newParent = item.getName();
        }
        if (depth < maxDepth) {
            for (HstSiteMenuItem childItem : item.getChildMenuItems()) {
                addMenuItem(childItem, map, depth + 1, maxDepth, newParent);
            }
        }
    }


    protected HstRequestContext getRequestContext() {
        return requestContext;
    }
}
