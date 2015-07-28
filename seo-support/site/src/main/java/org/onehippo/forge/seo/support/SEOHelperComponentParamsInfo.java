/*
 * Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
        @FieldGroup(
                titleKey = "group.titlesettings",
                value = {
                        "site-title-in-template",
                        "site-title",
                        "document-title-bean-props",
                        "menu-name",
                        "menu-item-allow-expanded",
                        "template-page-title"
                }
        ),
        @FieldGroup(
                titleKey = "group.descriptionsettings",
                value = {
                        "document-description-bean-props",
                        "default-meta-description"
                }
        ),
        @FieldGroup(
                titleKey = "group.dcsettings",
                value = {
                        "site-dc-copyright-link",
                        "dc-schema-link",
                        "dc-terms-link",
                        "enable-dc-language",
                        "dc-language"
                }
        ),
        @FieldGroup(
                // These attributes are still supported for backwards compatibility,
                // but no longer exposed through the component parameters dialog.
                titleKey = "group.remove",
                value = {
                        "document-attribute", // controlling the name of the attribute is confusing to the user,
                        "menu-attribute",     // the template should use the attributes as named by the component.
                        "params-info-attribute",
                        "document-keywords-bean-props", // keywords are no longer used by the relevant search engines
                        "keywords-in-document-title",
                        "default-meta-keywords",
                        "headers-in-template"
                }
        )
})

public interface SEOHelperComponentParamsInfo {

    @Parameter(name="document-attribute", defaultValue="document", description="Request scope attribute name for the current request content bean", hideInChannelManager = true)
    String getDocumentAttribute();

    @Parameter(name="menu-name", defaultValue="main", description="Name with which the current site menu for the request is retrieved")
    String getMenuName();

    @Parameter(name="menu-attribute", defaultValue="menu", description="Request scope attribute name for the current request site menu", hideInChannelManager = true)
    String getMenuAttribute();

    @Parameter(name="menu-item-allow-expanded", defaultValue = "false",
            description = "Flag whether the sitemenu item is allowed to be an expanded ancestor. If false, only exact matches are allowed.")
    boolean isMenuItemAllowExpanded();

    @Parameter(name="params-info-attribute", defaultValue="paramsInfo", description="Request scope attribute name for the parameters info of the current HST component", hideInChannelManager = true)
    String getParamsInfoAttribute();

    @Parameter(name="document-title-bean-props", defaultValue="title", description="Candidate bean property names of the current request content bean from which document title can be retrieved")
    String getDocumentTitleBeanProperties();

    @Parameter(name="document-keywords-bean-props", defaultValue="keywords", description="Candidate bean property names of the current request content bean from which comma separated keywords can be retrieved", hideInChannelManager = true)
    String getDocumentKeywordsBeanProperties();

    @Parameter(name="document-description-bean-props", defaultValue="description", description="Candidate bean property names of the current request content bean from which document description can be retrieved")
    String getDocumentDescriptionBeanProperties();

    @Parameter(name="site-title", description="Title of this website")
    String getSiteTitle();

    @Parameter(name="dc-schema-link", defaultValue="http://purl.org/dc/elements/1.1/", description="Dublin Core Schema Link")
    String getDublinCoreSchemaLink();

    @Parameter(name="dc-terms-link", defaultValue="http://purl.org/dc/terms/", description="Dublin Core Terms Link")
    String getDublinCoreTermsLink();

    @Parameter(name="dc-language", description="Dublin Core Language")
    String getDublinCoreLanguage();

    @Parameter(name="enable-dc-language", description="Enable Dublin Core Language")
    String getEnableDublinCoreLanguage();

    @Parameter(name="site-dc-copyright-link", description="SITE's Dublin Core Copyright Link")
    String getSiteDublinCoreCopyrightLink();

    @Parameter(name="headers-in-template", defaultValue="false", description="Flag whether a render template is used to contribute head elements.", hideInChannelManager = true)
    boolean isHeadersInTemplate();

    @Parameter(name="keywords-in-document-title", defaultValue="true", description="Flag whether keywords are appended to document title.", hideInChannelManager = true)
    boolean isKeywordsInDocumentTitle();

    @Parameter(name="default-meta-keywords", defaultValue="", description="If there are no meta-keywords in the document (because the field is empty or not available) these values are used", hideInChannelManager = true)
    String getDefaultMetaKeywords();

    @Parameter(name="default-meta-description", defaultValue="", description="If there are no meta-description in the document (because the field is empty or not available) this value is used")
    String getDefaultMetaDescription();

    @Parameter(name="site-title-in-template", defaultValue="false", description="Flag whether the page title is provided by the template.")
    boolean isSiteTitleInTemplate();

    @Parameter(name="template-page-title", defaultValue="%(siteTitle) - %(menuItem) - %(pageTitle) : %(keywords)", description="Template used to generate the page title")
    String getTemplatePageTitle();
}
