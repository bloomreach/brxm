/*
 * Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.seo.support;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenuItem;
import org.hippoecm.hst.utils.SimpleHtmlExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersInfo(type = SEOHelperComponentParamsInfo.class)
public class SEOHelperComponent extends BaseHstComponent {

    static final String SEO_COMPOUND_NODETYPE = "seosupport:seo";
    static final String SEO_TITLE_PROPERTY = "seosupport:seotitle";
    static final String SEO_DESCRIPTION_PROPERTY = "seosupport:seodescription";
    private static final String SEPARATOR_CHARACTERS = ", \t\r\n";
    private static Logger log = LoggerFactory.getLogger(SEOHelperComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        SEOHelperComponentParamsInfo params = getComponentParametersInfo(request);

        request.setAttribute(params.getParamsInfoAttribute(), params);

        final HippoBean document = request.getRequestContext().getContentBean();
        if (document != null) {
            request.setAttribute(params.getDocumentAttribute(), document);
        }

        final String menuName = params.getMenuName();
        final HstSiteMenu menu = request.getRequestContext().getHstSiteMenus().getSiteMenu(menuName);
        if (menu != null) {
            request.setAttribute(params.getMenuAttribute(), menu);
        }

        if (!params.isHeadersInTemplate()) {
            setSEORequestAttributes(request, params, document, menu);
        }
    }

    /**
     * Sets several SEO related request attributes
     *
     * @param request  current {@link HstRequest}
     * @param params   {@link SEOHelperComponentParamsInfo}
     * @param document {@link HippoBean} that is the source for the current page
     * @param menu     {@link HstSiteMenu}
     */
    protected void setSEORequestAttributes(HstRequest request, SEOHelperComponentParamsInfo params, HippoBean document,
                                           HstSiteMenu menu) {

        setPageTitleRequestAttribute(request, params, document, menu);
        setDublinCoreLinksRequestAttributes(request, params);
        setMetaKeywordsDescriptionRequestAttributes(request, params, document, menu);
        setDocumentDatesRequestAttributes(request, document);
        setImageAttributes(request, params, document);
    }

    /**
     * Sets the {@literal title} request attribute
     *
     * @param request  current {@link HstRequest}
     * @param params   {@link SEOHelperComponentParamsInfo}
     * @param document {@link HippoBean} that is the source for the current page
     * @param menu     {@link HstSiteMenu} to add parent items to the title (like a bread crumb)
     */
    protected void setPageTitleRequestAttribute(HstRequest request, SEOHelperComponentParamsInfo params,
                                                HippoBean document, HstSiteMenu menu) {
        if (!params.isSiteTitleInTemplate()) {
            try {
                String mergedTitle = getPageTitle(request, params, document, menu);
                if (StringUtils.isBlank(mergedTitle)) {
                    return;
                }

                request.setAttribute("title", mergedTitle);
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to add title head element", e);
                } else {
                    log.warn("Failed to add title head element. {}", e.toString());
                }
            }
        }
    }

    /**
     * Sets the {@literal image} request attribute
     *
     * @param request  current {@link HstRequest}
     * @param params   {@link SEOHelperComponentParamsInfo}
     * @param document {@link HippoBean} that is the source for the current page
     */
    protected void setImageAttributes(HstRequest request, SEOHelperComponentParamsInfo params, HippoBean document) {
        if (document != null) {
            try {
                request.setAttribute("image", getDocumentImage(document, params));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to add image head element", e);
                } else {
                    log.warn("Failed to add image head element. {}", e.toString());
                }
            }
        }
    }

    /**
     * Sets {@literal metaKeywords} and {@literal metaDescription} request attributes
     *
     * @param request  current {@link HstRequest}
     * @param params   {@link SEOHelperComponentParamsInfo}
     * @param document {@link HippoBean} that is the source for the current page
     * @param menu     {@link HstSiteMenu}
     */
    protected void setMetaKeywordsDescriptionRequestAttributes(HstRequest request, SEOHelperComponentParamsInfo params,
                                                               HippoBean document, HstSiteMenu menu) {
        try {
            String keywords = "";
            String pageKeywords = getPageKeywords(request, params, document, menu);
            String defaultKeywords = params.getDefaultMetaKeywords();

            if (StringUtils.isNotBlank(pageKeywords)) {
                keywords = pageKeywords;
            } else if (StringUtils.isNotBlank(defaultKeywords)) {
                keywords = defaultKeywords;
            }

            if (StringUtils.isNotBlank(keywords)) {
                request.setAttribute("metaKeywords", keywords);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add keywords head element", e);
            } else {
                log.warn("Failed to add keywords head element. {}", e.toString());
            }
        }

        try {
            String description = "";
            String seoDescription = getSeoDescription(document);
            String pageDescription = getPageDescription(request, params, document, menu);
            String defaultDescription = params.getDefaultMetaDescription();

            if (StringUtils.isNotBlank(seoDescription)) {
                description = seoDescription;
            } else if (StringUtils.isNotBlank(pageDescription)) {
                description = pageDescription;
            } else if (StringUtils.isNotBlank(defaultDescription)) {
                description = defaultDescription;
            }

            if (StringUtils.isNotBlank(description)) {
                request.setAttribute("metaDescription", description);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add description head element", e);
            } else {
                log.warn("Failed to add description head element. {}", e.toString());
            }
        }
    }

    /**
     * Sets {@literal dublinCoreSchemaLink}, {@literal dublinCoreTermsLink}, {@literal dublinCoreCopyrightLink}
     * and {@literal dublinCoreLanguage} request attributes
     *
     * @param request  current {@link HstRequest}
     * @param params   {@link SEOHelperComponentParamsInfo}
     */
    protected void setDublinCoreLinksRequestAttributes(HstRequest request, SEOHelperComponentParamsInfo params) {
        String dublinCoreSchemaLink = getDublinCoreSchemaLink(request, params);
        if (StringUtils.isNotBlank(dublinCoreSchemaLink)) {
            request.setAttribute("dublinCoreSchemaLink", dublinCoreSchemaLink);
        }

        String dublinCoreTermsLink = getDublinCoreTermsLink(request, params);
        if (StringUtils.isNotBlank(dublinCoreTermsLink)) {
            request.setAttribute("dublinCoreTermsLink", dublinCoreTermsLink);
        }

        String dublinCoreSiteCopyrightLink = getSiteDublinCoreCopyrightLink(request, params);
        if (StringUtils.isNotBlank(dublinCoreSiteCopyrightLink)) {
            request.setAttribute("dublinCoreCopyrightLink", dublinCoreSiteCopyrightLink);
        }

        String dublinCoreSiteLanguage = getDublinCoreLangauge(request, params);
        if (StringUtils.isNotBlank(dublinCoreSiteLanguage)) {
            request.setAttribute("dublinCoreLanguage", dublinCoreSiteLanguage);
        }
    }

    /**
     * Sets {@literal dublinCoreTermsCreated} and {@literal dublinCoreTermsModified} request
     * attributes
     *
     * @param request  current {@link HstRequest}
     * @param document {@link HippoBean} that is the source for the current page
     */
    protected void setDocumentDatesRequestAttributes(HstRequest request, HippoBean document) {
        try {
            Calendar documentCreated = getDocumentCreated(request, document);
            if (documentCreated == null) {
                return;
            }
            request.setAttribute("dublinCoreTermsCreated",
                    DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(documentCreated));

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add DCTERMS.created meta head element", e);
            } else {
                log.warn("Failed to add DCTERMS.created meta head element. {}", e.toString());
            }
        }

        try {
            Calendar documentModified = getDocumentModified(request, document);
            if (documentModified != null) {
                request.setAttribute("dublinCoreTermsModified",
                        DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(documentModified));
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add DCTERMS.modified meta head element", e);
            } else {
                log.warn("Failed to add DCTERMS.modified meta head element. {}", e.toString());
            }
        }
    }

    protected String getDublinCoreSchemaLink(HstRequest request, SEOHelperComponentParamsInfo params) {
        return params.getDublinCoreSchemaLink();
    }

    protected String getDublinCoreTermsLink(HstRequest request, SEOHelperComponentParamsInfo params) {
        return params.getDublinCoreTermsLink();
    }

    protected String getSiteDublinCoreCopyrightLink(HstRequest request, SEOHelperComponentParamsInfo params) {
        return params.getSiteDublinCoreCopyrightLink();
    }

    protected String getDublinCoreLangauge(HstRequest request, SEOHelperComponentParamsInfo params) {
        if ("true".equalsIgnoreCase(params.getEnableDublinCoreLanguage())) {
            String language = params.getDublinCoreLanguage();
            if (StringUtils.isNotEmpty(language)) {
                return language;
            }

            Locale locale = request.getLocale();
            if (locale != null) {
                if (StringUtils.isEmpty(locale.getCountry())) {
                    return locale.getLanguage();
                } else {
                    return locale.getLanguage() + "-" + locale.getCountry();
                }
            }
        }

        return null;
    }

    /**
     * Composes the page title based on
     * <ul>
     * <li>{@link org.onehippo.forge.seo.support.SEOHelperComponentParamsInfo#getSiteTitle()}</li>
     * <li>current sitemenu item</li>
     * <li>Title of the content bean for the current request</li>
     * </ul>
     *
     * @param request  current {@link HstRequest}
     * @param params   {@link SEOHelperComponentParamsInfo}
     * @param document {@link HippoBean} that is the main content of the page
     * @param menu     {@link HstSiteMenu}
     * @return A title like {@literal SiteName - MenuItem - PageTitle}
     * @throws Exception if properties cannot be retrieved
     */
    protected String getPageTitle(HstRequest request, SEOHelperComponentParamsInfo params, HippoBean document,
                                  HstSiteMenu menu) throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("siteTitle", "");
        values.put("menuItem", "");
        values.put("pageTitle", "");
        values.put("keywords", "");

        String siteTitle = getSiteTitle(request, params);
        if (StringUtils.isNotBlank(siteTitle)) {
            values.put("siteTitle", siteTitle);
        }

        HstSiteMenuItem selectedMenuItem = findSelectedSiteMenuItem(request, params, menu);
        HstSiteMenuItem expandedMenuItem = findExpandedSiteMenuItem(request, params, menu);

        if (selectedMenuItem != null && selectedMenuItem.getParentItem() != null) {
            values.put("menuItem", selectedMenuItem.getParentItem().getName());
        } else if (selectedMenuItem == null && params.isMenuItemAllowExpanded() && expandedMenuItem != null) {
            values.put("menuItem", expandedMenuItem.getName());
        }

        String documentTitle = getDocumentTitle(document, params);
        String sitemapTitle = null;
        if (request.getRequestContext().getResolvedSiteMapItem()!=null) {
            sitemapTitle = request.getRequestContext().getResolvedSiteMapItem().getPageTitle();
        }
        if (StringUtils.isNotBlank(documentTitle)) {
            values.put("pageTitle", documentTitle);
        } else if (sitemapTitle != null) {
            values.put("pageTitle", sitemapTitle);
        } else if (selectedMenuItem != null) {
            values.put("pageTitle", selectedMenuItem.getName());
        }

        if (params.isKeywordsInDocumentTitle()) {
            String keywords = getPageKeywords(request, params, document, menu);
            if (keywords != null) {
                values.put("keywords", keywords);
            }
        }

        return generatePageTitle(params.getTemplatePageTitle(), values);
    }

    /**
     * Gets the document title from a comma separated list of properties using the first property that returns a value
     *
     * @param document {@link HippoBean}
     * @param params   {@link SEOHelperComponentParamsInfo}
     * @return title based on the configured properties or {@literal null} if it can't be found
     * @throws Exception if something goes wrong
     */
    protected String getDocumentTitle(HippoBean document, SEOHelperComponentParamsInfo params) throws Exception {

        if (document != null) {
            // try title from SEO compound
            final String documentTitle = getSeoTitle(document);
            if (StringUtils.isNotBlank(documentTitle)) {
                return documentTitle;
            }

            // fall-back to property-based title
            final String propertiesString = params.getDocumentTitleBeanProperties();
            if (propertiesString != null) {
                final String[] propertyNames = StringUtils.split(propertiesString, SEPARATOR_CHARACTERS);
                for (String propertyName : propertyNames) {
                    if (PropertyUtils.isReadable(document, propertyName)) {
                        final String propertyValue = getPropertyAsEscapedString(document, propertyName);
                        if (StringUtils.isNotBlank(propertyValue)) {
                            return propertyValue;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets the document image from a comma separated list of properties using the first property that returns a value
     *
     * @param document {@link HippoBean}
     * @param params   {@link SEOHelperComponentParamsInfo}
     * @return image based on the configured properties or {@literal null} if it can't be found
     * @throws Exception if something goes wrong
     */
    protected Object getDocumentImage(HippoBean document, SEOHelperComponentParamsInfo params) throws Exception {

        if (document != null) {
            // fall-back to property-based title
            final String propertiesString = params.getDocumentImageBeanProperties();
            if (propertiesString != null) {
                final String[] propertyNames = StringUtils.split(propertiesString, SEPARATOR_CHARACTERS);
                for (String propertyName : propertyNames) {
                    if (PropertyUtils.isReadable(document, propertyName)) {
                        Object image = PropertyUtils.getProperty(document, propertyName);
                        return image;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Tries to find the sitemenu item that is an exact match (hit on "Events")
     *
     * @param request current {@link HstRequest}
     * @param params  {@link SEOHelperComponentParamsInfo}
     * @param menu    {@link HstSiteMenu} used for the SEO
     * @return {@link HstSiteMenuItem} if it can be found, otherwise {@literal null}
     */
    protected HstSiteMenuItem findSelectedSiteMenuItem(HstRequest request, SEOHelperComponentParamsInfo params,
                                                       HstSiteMenu menu) {
        if (menu == null) {
            return null;
        }
        return menu.getSelectSiteMenuItem();
    }

    /**
     * Gets the sitemenu item that is closest to the current request ("Events" if the request is for an Event detail page).
     *
     * @param request current {@link HstRequest}
     * @param params  {@link SEOHelperComponentParamsInfo}
     * @param menu    {@link HstSiteMenu} used for the SEO
     * @return {@link HstSiteMenuItem} if it can be found, otherwise {@literal null}
     */
    protected HstSiteMenuItem findExpandedSiteMenuItem(HstRequest request, SEOHelperComponentParamsInfo params,
                                                       HstSiteMenu menu) {
        if (menu == null) {
            return null;
        }
        return menu.getDeepestExpandedItem();
    }

    protected String getPageKeywords(HstRequest request, SEOHelperComponentParamsInfo params, HippoBean document,
                                     HstSiteMenu menu) throws Exception {
        if (document != null) {
            final String propertiesString = params.getDocumentKeywordsBeanProperties();
            if (propertiesString != null) {
                return getPropertiesAsValue(document, StringUtils.split(propertiesString, SEPARATOR_CHARACTERS));
            }
        }
        return null;
    }

    protected String getPageDescription(HstRequest request, SEOHelperComponentParamsInfo params, HippoBean document,
                                        HstSiteMenu menu) throws Exception {
        if (document != null) {
            final String propertiesString = params.getDocumentDescriptionBeanProperties();
            if (propertiesString != null) {
                return getPropertiesAsValue(document, StringUtils.split(propertiesString, SEPARATOR_CHARACTERS));
            }
        }
        return null;
    }

    protected String getSiteTitle(HstRequest request, SEOHelperComponentParamsInfo params) {
        return params.getSiteTitle();
    }

    protected Calendar getDocumentCreated(HstRequest request, HippoBean document) throws Exception {
        if (document == null) {
            return null;
        }

        Node node = document.getNode();

        if (node == null) {
            return null;
        }
        if (node.isNodeType("hippostdpubwf:document")) {
            return document.getProperty("hippostdpubwf:publicationDate");
        } else if (node.isNodeType("mix:created")) {
            return document.getProperty("jcr:created");
        }
        return null;
    }

    protected Calendar getDocumentModified(HstRequest request, HippoBean document) throws Exception {
        if (document == null) {
            return null;
        }

        Node node = document.getNode();

        if (node == null) {
            return null;
        }

        if (node.isNodeType("hippostdpubwf:document")) {
            return document.getProperty("hippostdpubwf:lastModificationDate");
        } else if (node.isNodeType("mix:lastModified")) {
            return document.getProperty("jcr:lastModified");
        }
        return null;
    }

    protected String getPropertiesAsValue(HippoBean document, String[] propertyNames) throws Exception {
        if (propertyNames != null) {
            for (String propertyName : propertyNames) {
                if (PropertyUtils.isReadable(document, propertyName)) {
                    String propertyValue = getPropertyAsEscapedString(document, propertyName);
                    if (StringUtils.isNotBlank(propertyValue)) {
                        return propertyValue;
                    }
                }
            }
        }

        return null;
    }

    protected String getPropertyAsEscapedString(Object bean, String propertyName) throws Exception {
        Object value = PropertyUtils.getProperty(bean, propertyName);

        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof HippoHtml) {
            return SimpleHtmlExtractor.getText(((HippoHtml) value).getContent());
        } else {
            return SimpleHtmlExtractor.getText(value.toString());
        }
    }

    /**
     * Use the provisioned template to to generate the page title
     *
     * @param template  the template for generating the page title
     * @param values    the values to substitute into the page title
     * @return          the page title
     */
    private String generatePageTitle(String template, Map<String, String> values) {
        StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");

        // Remove placeholders for missing values from template
        for (String value : values.keySet()) {
            if (StringUtils.isBlank(values.get(value))) {
                final String placeholder = "%("+value+")";
                int startPlaceholder = template.indexOf(placeholder);
                int endPlaceholder = startPlaceholder + placeholder.length();
                if (startPlaceholder >= 0) {
                    // template requests missing value, adjust template.
                    int endPreviousPlaceholder = template.substring(0, startPlaceholder).lastIndexOf(")");
                    if (endPreviousPlaceholder >= 0) {
                        // Not the first placeholder in template, remove separator before missing value, and placeholder
                        template = template.substring(0, endPreviousPlaceholder + 1)
                                + template.substring(endPlaceholder);
                    } else {
                        int startNextPlaceholder = endPlaceholder + template.substring(endPlaceholder).indexOf("%(");
                        if (startNextPlaceholder >= 0) {
                            // Not the last placeholder in template, remove placeholder and separator after missing value
                            template = template.substring(0, startPlaceholder) + template.substring(startNextPlaceholder);
                        } else {
                            // Only placeholder in the template, remove it
                            template = template.substring(0, startPlaceholder)
                                    + template.substring(endPlaceholder);
                        }
                    }
                }
            }
        }

        return sub.replace(template);
    }

    /**
     * Gets the title set in the Seo compound on the document
     *
     * @param document {@link HippoBean}
     * @return {@link String} title set in the Seo compound on the document if it is set, otherwise {@literal null}
     */
    private String getSeoTitle(HippoBean document) {
        if (document != null) {
            return getPropertyFromSeoCompound(document, SEO_TITLE_PROPERTY);
        }
        return null;
    }

    /**
     * Gets the description set in the Seo compound on the document
     *
     * @param document {@link HippoBean}
     * @return {@link String} description set in the Seo compound on the document if it is set, otherwise {@literal null}
     */
    private String getSeoDescription(HippoBean document) {
        if (document != null) {
            return getPropertyFromSeoCompound(document, SEO_DESCRIPTION_PROPERTY);
        }
        return null;
    }

    /**
     *
     * @param document  {@link HippoBean}
     * @param property  Name of the to-be-retrieved String property of the SEO compound
     * @return          Value of the String property of the SEO compound
     */
    private String getPropertyFromSeoCompound(HippoBean document, String property) {
        try {
            NodeIterator nodes = document.getNode().getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node.getPrimaryNodeType().getName().equals(SEO_COMPOUND_NODETYPE)
                        && node.hasProperty(property)) {
                    return node.getProperty(property).getString();
                }
            }
        } catch (RepositoryException e) {
            log.warn("Failure retrieving property {} from SEO compound of document {}", property, document.getName());
        }
        return null;
    }
}
