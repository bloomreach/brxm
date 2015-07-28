/*
 * Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
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

    public static final String DCTERMS_SCHEME = "DCTERMS.RFC3066";
    private static Logger log = LoggerFactory.getLogger(SEOHelperComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        SEOHelperComponentParamsInfo params = getComponentParametersInfo(request);

        request.setAttribute(params.getParamsInfoAttribute(), params);

        final String documentAttrName = params.getDocumentAttribute();
        final String menuName = params.getMenuName();
        final String menuAttrName = params.getMenuAttribute();

        final HippoBean document = request.getRequestContext().getContentBean();

        if (document != null) {
            request.setAttribute(documentAttrName, document);
        }

        final HstSiteMenu menu = request.getRequestContext().getHstSiteMenus().getSiteMenu(menuName);

        if (menu != null) {
            request.setAttribute(menuAttrName, menu);
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
            String pageDescription = getPageDescription(request, params, document, menu);
            String defaultDescription = params.getDefaultMetaDescription();

            if (StringUtils.isNotBlank(pageDescription)) {
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
        try {
            String dublinCoreSchemaLink = getDublinCoreSchemaLink(request, params);

            if (StringUtils.isNotBlank(dublinCoreSchemaLink)) {
                request.setAttribute("dublinCoreSchemaLink", dublinCoreSchemaLink);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add schema.DC link head element", e);
            } else {
                log.warn("Failed to add schema.DC link head element. {}", e.toString());
            }
        }

        try {
            String dublinCoreTermsLink = getDublinCoreTermsLink(request, params);

            if (StringUtils.isNotBlank(dublinCoreTermsLink)) {
                request.setAttribute("dublinCoreTermsLink", dublinCoreTermsLink);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add schema.DCTERMS link head element", e);
            } else {
                log.warn("Failed to add schema.DCTERMS link head element. {}", e.toString());
            }
        }

        try {
            String dublinCoreSiteCopyrightLink = getSiteDublinCoreCopyrightLink(request, params);

            if (StringUtils.isNotBlank(dublinCoreSiteCopyrightLink)) {
                request.setAttribute("dublinCoreCopyrightLink", dublinCoreSiteCopyrightLink);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add DC.rights link head element", e);
            } else {
                log.warn("Failed to add DC.rights link head element. {}", e.toString());
            }
        }

        try {
            String dublinCoreSiteLanguage = getDublinCoreLangauge(request, params);

            if (StringUtils.isNotBlank(dublinCoreSiteLanguage)) {
                request.setAttribute("dublinCoreLanguage", dublinCoreSiteLanguage);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to add DC.language link head element", e);
            } else {
                log.warn("Failed to add DC.language link head element. {}", e.toString());
            }
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
        String enable = params.getEnableDublinCoreLanguage();

        if ("true".equalsIgnoreCase(enable)) {
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
     * Composes the page titlebased on
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
        List<Object> titleParts = new ArrayList<Object>();

        String siteTitle = getSiteTitle(request, params);
        if (StringUtils.isNotBlank(siteTitle)) {
            titleParts.add(siteTitle);
        }

        HstSiteMenuItem selectedMenuItem = findSelectedSiteMenuItem(request, params, menu);
        HstSiteMenuItem expandedMenuItem = findExpandedSiteMenuItem(request, params, menu);

        String documentTitle = getDocumentTitle(document, params);

        if (selectedMenuItem != null && selectedMenuItem.getParentItem() != null) {
            titleParts.add(selectedMenuItem.getParentItem().getName());
        } else if (selectedMenuItem == null && params.isMenuItemAllowExpanded() && expandedMenuItem != null) {
            titleParts.add(expandedMenuItem.getName());
        }

        if (StringUtils.isNotBlank(documentTitle)) {
            titleParts.add(documentTitle);
        } else if (selectedMenuItem != null) {
            titleParts.add(selectedMenuItem.getName());
        }

        String keywords = null;

        if (params.isKeywordsInDocumentTitle()) {
            keywords = getPageKeywords(request, params, document, menu);
        }

        if (StringUtils.isBlank(keywords)) {
            return StringUtils.join(titleParts, " - ");
        } else {
            return new StringBuilder(80).append(StringUtils.join(titleParts, " - ")).append(" : ").append(keywords)
                    .toString();
        }
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
        String documentTitle = null;

        if (document == null) {
            return documentTitle;
        }
        String[] documentTitlePropNames = StringUtils.split(params.getDocumentTitleBeanProperties(), ", \t\r\n");

        if (documentTitlePropNames == null || documentTitlePropNames.length <= 0) {
            return documentTitle;
        }
        for (String documentTitlePropName : documentTitlePropNames) {
            if (PropertyUtils.isReadable(document, documentTitlePropName)) {
                String candidateTitle = getPropertyAsEscapedString(document, documentTitlePropName);

                if (StringUtils.isNotBlank(candidateTitle)) {
                    documentTitle = candidateTitle;
                    break;
                }
            }
        }
        return documentTitle;
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
        if (document == null) {
            return null;
        }
        String[] documentKeywordsPropNames = StringUtils.split(params.getDocumentKeywordsBeanProperties(), ", \t\r\n");

        return getPropertiesAsValue(document, documentKeywordsPropNames);
    }

    protected String getPageDescription(HstRequest request, SEOHelperComponentParamsInfo params, HippoBean document,
            HstSiteMenu menu) throws Exception {
        if (document == null) {
            return null;
        }
        String[] documentDescriptionPropNames = StringUtils.split(params.getDocumentDescriptionBeanProperties(),
                ", \t\r\n");

        return getPropertiesAsValue(document, documentDescriptionPropNames);
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
        if (propertyNames == null || propertyNames.length <= 0) {
            return null;
        }
        for (String documentKeywordsPropName : propertyNames) {
            if (PropertyUtils.isReadable(document, documentKeywordsPropName)) {
                String keywords = getPropertyAsEscapedString(document, documentKeywordsPropName);

                if (StringUtils.isNotBlank(keywords)) {
                    return keywords;
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
}
