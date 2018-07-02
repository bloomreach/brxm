/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.service;

import java.util.Map;

/**
 * Service for retrieving a fresh map of placeholders, typically used for template interpolation when installing /
 * importing some resource into the project.
 *
 * Can be @Inject'ed into REST Resources and Instructions.
 */
public interface PlaceholderService {

    String NAMESPACE = "namespace";

    String BEANS_PACKAGE = "beansPackage";
    String PROJECT_PACKAGE = "projectPackage";
    String COMPONENTS_PACKAGE = "componentsPackage";
    String REST_PACKAGE = "restPackage";

    String TRANSLATION_ID = "translationId";

    String DATE_CURRENT_YEAR = "currentYear";
    String DATE_CURRENT_MONTH = "currentMonth";
    String DATE_CURRENT_YYYY = "dateCurrentYear";
    String DATE_CURRENT_MM = "dateCurrentMonth";
    String DATE_NEXT_YYYY = "dateNextYear";
    String DATE_NEXT_MM = "dateNextMonth";
    String DATE_JCR_CURRENT = "jcrDate";
    String DATE_JCR_NEXT_MONTH = "jcrDateNextMonth";
    String DATE_JCR_NEXT_YEAR = "jcrDateNextYear";

    String PROJECT_ROOT = "projectRoot";
    String SITE_ROOT = "siteRoot";
    String SITE_WEB_ROOT = "siteWebRoot";
    String SITE_WEB_INF_ROOT = "siteWebInfRoot";
    String CMS_WEB_INF_ROOT = "cmsWebInfRoot";
    String SITE_RESOURCES = "siteResources";
    String JSP_ROOT = "jspRoot";
    String JAVASCRIPT_ROOT = "javascriptRoot";
    String IMAGES_ROOT = "imagesRoot";
    String BEANS_FOLDER = "beansFolder";
    String COMPONENTS_FOLDER = "componentsFolder";
    String REST_FOLDER = "restFolder";
    String SITE_OVERRIDE_FOLDER = "siteOverrideFolder";
    String CSS_ROOT = "cssRoot";
    String CMS_ROOT = "cmsRoot";
    String CMS_RESOURCES = "cmsResources";
    String CMS_WEB_ROOT = "cmsWebRoot";
    String WEBFILES_RESOURCES = "webfilesResources";
    String WEBFILES_ROOT = "webfilesRoot";
    String WEBFILES_FREEMARKER_ROOT = "freemarkerRoot";
    String WEBFILES_CSS_ROOT = "webfilesCssRoot";
    String WEBFILES_JS_ROOT = "webfilesJsRoot";
    String WEBFILES_IMAGES_ROOT = "webfilesImagesRoot";
    String WEBFILES_PREFIX = "webfilesPrefix";
    String ESSENTIALS_ROOT = "essentialsRoot";
    String HST_ROOT = "hstRoot";

    /**
     * Make and return a fresh set of placeholders.
     *
     * @return fresh set of placeholders.
     */
    Map<String, Object> makePlaceholders();
}
