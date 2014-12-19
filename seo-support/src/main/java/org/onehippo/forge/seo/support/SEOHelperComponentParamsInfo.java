/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.parameters.Parameter;

public interface SEOHelperComponentParamsInfo {

    @Parameter(name="document-attribute", defaultValue="document", description="Request scope attribute name for the current request content bean")
    public String getDocumentAttribute();

    @Parameter(name="menu-name", defaultValue="main", description="Name with which the current site menu for the request is retrieved")
    public String getMenuName();

    @Parameter(name="menu-attribute", defaultValue="menu", description="Request scope attribute name for the current request site menu")
    public String getMenuAttribute();

    @Parameter(name="menu-item-allow-expanded", defaultValue = "false",
            description = "Flag whether the sitemenu item is allowed to be an expanded ancestor. If false, only exact matches are allowed.")
    public boolean isMenuItemAllowExpanded();

    @Parameter(name="params-info-attribute", defaultValue="paramsInfo", description="Request scope attribute name for the parameters info of the current HST component")
    public String getParamsInfoAttribute();

    @Parameter(name="document-title-bean-props", defaultValue="title", description="Candidate bean property names of the current request content bean from which document title can be retrieved")
    public String getDocumentTitleBeanProperties();

    @Parameter(name="document-keywords-bean-props", defaultValue="keywords", description="Candidate bean property names of the current request content bean from which comma separated keywords can be retrieved")
    public String getDocumentKeywordsBeanProperties();

    @Parameter(name="document-description-bean-props", defaultValue="description", description="Candidate bean property names of the current request content bean from which document description can be retrieved")
    public String getDocumentDescriptionBeanProperties();

    @Parameter(name="site-title", description="Title of this website")
    public String getSiteTitle();

    @Parameter(name="dc-schema-link", defaultValue="http://purl.org/dc/elements/1.1/", description="Dublin Core Schema Link")
    public String getDublinCoreSchemaLink();

    @Parameter(name="dc-terms-link", defaultValue="http://purl.org/dc/terms/", description="Dublin Core Terms Link")
    public String getDublinCoreTermsLink();

    @Parameter(name="dc-language", description="Dublin Core Language")
    public String getDublinCoreLanguage();

    @Parameter(name="enable-dc-language", description="Enable Dublin Core Language")
    public String getEnableDublinCoreLanguage();

    @Parameter(name="site-dc-copyright-link", description="SITE's Dublin Core Copyright Link")
    public String getSiteDublinCoreCopyrightLink();

    @Parameter(name="headers-in-template", defaultValue="false", description="Flag whether a render template is used to contribute head elements.")
    public boolean isHeadersInTemplate();

    @Parameter(name="keywords-in-document-title", defaultValue="true", description="Flag whether keywords are appended to document title.")
    public boolean isKeywordsInDocumentTitle();

    @Parameter(name="default-meta-keywords", defaultValue="", description="If there are no meta-keywords in the document (because the field is empty or not available) these values are used")
    public String getDefaultMetaKeywords();

    @Parameter(name="default-meta-description", defaultValue="", description="If there are no meta-description in the document (because the field is empty or not available) this value is used")
    public String getDefaultMetaDescription();

    @Parameter(name="site-title-in-template", defaultValue="false", description="Flag whether the title is provided by the template.")
    public boolean isSiteTitleInTemplate();

}
