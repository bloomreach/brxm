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
                        "default-meta-description",
                }
        ),
        @FieldGroup(
                titleKey = "group.imagesettings",
                value = {
                        "document-image-bean-props"
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

    @Parameter(name="document-attribute", defaultValue="document", hideInChannelManager = true)
    String getDocumentAttribute();

    @Parameter(name="menu-name", defaultValue="main")
    String getMenuName();

    @Parameter(name="menu-attribute", defaultValue="menu", hideInChannelManager = true)
    String getMenuAttribute();

    @Parameter(name="menu-item-allow-expanded", defaultValue = "false")
    boolean isMenuItemAllowExpanded();

    @Parameter(name="params-info-attribute", defaultValue="paramsInfo", hideInChannelManager = true)
    String getParamsInfoAttribute();

    @Parameter(name="document-title-bean-props", defaultValue="title")
    String getDocumentTitleBeanProperties();

    @Parameter(name="document-keywords-bean-props", defaultValue="keywords", hideInChannelManager = true)
    String getDocumentKeywordsBeanProperties();

    @Parameter(name="document-description-bean-props", defaultValue="description")
    String getDocumentDescriptionBeanProperties();

    @Parameter(name="document-image-bean-props", defaultValue="image")
    String getDocumentImageBeanProperties();

    @Parameter(name="site-title")
    String getSiteTitle();

    @Parameter(name="dc-schema-link", defaultValue="http://purl.org/dc/elements/1.1/")
    String getDublinCoreSchemaLink();

    @Parameter(name="dc-terms-link", defaultValue="http://purl.org/dc/terms/")
    String getDublinCoreTermsLink();

    @Parameter(name="dc-language")
    String getDublinCoreLanguage();

    @Parameter(name="enable-dc-language")
    String getEnableDublinCoreLanguage();

    @Parameter(name="site-dc-copyright-link")
    String getSiteDublinCoreCopyrightLink();

    @Parameter(name="headers-in-template", defaultValue="false", hideInChannelManager = true)
    boolean isHeadersInTemplate();

    @Parameter(name="keywords-in-document-title", defaultValue="true", hideInChannelManager = true)
    boolean isKeywordsInDocumentTitle();

    @Parameter(name="default-meta-keywords", defaultValue="", hideInChannelManager = true)
    String getDefaultMetaKeywords();

    @Parameter(name="default-meta-description", defaultValue="")
    String getDefaultMetaDescription();

    @Parameter(name="site-title-in-template", defaultValue="false")
    boolean isSiteTitleInTemplate();

    @Parameter(name="template-page-title", defaultValue="%(siteTitle) - %(menuItem) - %(pageTitle) : %(keywords)")
    String getTemplatePageTitle();
}
