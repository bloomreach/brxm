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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.sdk.api.service.PlaceholderService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.ProjectSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlaceholderServiceImpl implements PlaceholderService {

    private static final Logger LOG = LoggerFactory.getLogger(PlaceholderServiceImpl.class);

    private final SettingsService settingsService;
    private final ProjectService projectService;

    @Inject
    public PlaceholderServiceImpl(final SettingsService settingsService, final ProjectService projectService) {
        this.settingsService = settingsService;
        this.projectService = projectService;
    }

    @Override
    public Map<String, Object> makePlaceholders() {
        final Map<String, Object> data = new HashMap<>();
        final ProjectSettings settings = settingsService.getSettings();

        addDatePlaceholders(data);
        addFileSystemPlaceholders(data);

        // project namespace
        data.put(NAMESPACE, settings.getProjectNamespace());

        // packages
        data.put(BEANS_PACKAGE, settings.getSelectedBeansPackage());
        data.put(REST_PACKAGE, settings.getSelectedRestPackage());
        data.put(COMPONENTS_PACKAGE, settings.getSelectedComponentsPackage());
        data.put(PROJECT_PACKAGE, settings.getSelectedProjectPackage());

        // Generate unique translation IDs
        data.put(TRANSLATION_ID, new Object() {
            @Override
            public String toString() {
                return UUID.randomUUID().toString();
            }
        });

        return data;
    }

    private void addDatePlaceholders(final Map<String, Object> data) {
        final Calendar now = Calendar.getInstance();
        data.put(DATE_CURRENT_YEAR, now.get(Calendar.YEAR));
        data.put(DATE_CURRENT_MONTH, now.get(Calendar.MONTH) + 1);
        data.put(DATE_CURRENT_YYYY, new SimpleDateFormat("yyyy").format(now.getTime()));
        data.put(DATE_CURRENT_MM, new SimpleDateFormat("MM").format(now.getTime()));
        data.put(DATE_JCR_CURRENT, makeJcrDate(now));

        final Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.MONTH, 1);
        data.put(DATE_NEXT_MM, new SimpleDateFormat("MM").format(nextMonth.getTime()));
        data.put(DATE_JCR_NEXT_MONTH, makeJcrDate(nextMonth));

        final Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);
        data.put(DATE_NEXT_YYYY, new SimpleDateFormat("yyyy").format(nextYear.getTime()));
        data.put(DATE_JCR_NEXT_YEAR, makeJcrDate(nextYear));
    }

    private String makeJcrDate(final Calendar date) {
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        try {
            return ValueFactoryImpl.getInstance().createValue(date).getString();
        } catch (RepositoryException e) {
            LOG.error("Failed creating jcrDate for placeholder.", e);
        }
        return "1970-01-01T01:00:00.000+01:00";
    }

    private void addFileSystemPlaceholders(final Map<String, Object> data) {
        final Path siteWebRoot = projectService.getWebApplicationRootPathForModule(Module.SITE_WEBAPP);
        final Path siteResourcesRoot = projectService.getResourcesRootPathForModule(Module.SITE_COMPONENTS);
        final Path cmsWebRoot = projectService.getWebApplicationRootPathForModule(Module.CMS);
        final Path webFilesResourcesRoot = projectService.getResourcesRootPathForModule(Module.REPOSITORY_DATA_WEB_FILES);
        final Path webFilesRoot = webFilesResourcesRoot.resolve("site");

        // project
        data.put(PROJECT_ROOT, projectService.getBasePathForModule(Module.PROJECT));

        // site
        data.put(SITE_ROOT, projectService.getBasePathForModule(Module.SITE));
        data.put(SITE_WEB_ROOT, siteWebRoot);
        data.put(JAVASCRIPT_ROOT, siteWebRoot.resolve("js"));
        data.put(IMAGES_ROOT, siteWebRoot.resolve("images"));
        data.put(CSS_ROOT, siteWebRoot.resolve("css"));
        data.put(SITE_WEB_INF_ROOT, siteWebRoot.resolve("WEB-INF"));
        data.put(JSP_ROOT, siteWebRoot.resolve("WEB-INF").resolve("jsp"));
        data.put(SITE_RESOURCES, siteResourcesRoot);
        data.put(SITE_OVERRIDE_FOLDER, siteResourcesRoot.resolve("META-INF").resolve("hst-assembly").resolve("overrides"));
        data.put(BEANS_FOLDER, projectService.getBeansPackagePath());
        data.put(REST_FOLDER, projectService.getRestPackagePath());
        data.put(COMPONENTS_FOLDER, projectService.getComponentsPackagePath());

        // cms
        data.put(CMS_ROOT, projectService.getBasePathForModule(Module.CMS));
        data.put(CMS_WEB_ROOT, cmsWebRoot);
        data.put(CMS_WEB_INF_ROOT, cmsWebRoot.resolve("WEB-INF"));
        data.put(CMS_RESOURCES, projectService.getResourcesRootPathForModule(Module.CMS).toString());

        // webfiles
        data.put(WEBFILES_RESOURCES, webFilesResourcesRoot);
        data.put(WEBFILES_ROOT, webFilesRoot);
        data.put(WEBFILES_FREEMARKER_ROOT, webFilesRoot.resolve("freemarker"));
        data.put(WEBFILES_CSS_ROOT, webFilesRoot.resolve("css"));
        data.put(WEBFILES_JS_ROOT, webFilesRoot.resolve("js"));
        data.put(WEBFILES_IMAGES_ROOT, webFilesRoot.resolve("images"));
        data.put(WEBFILES_PREFIX, EssentialConst.WEBFILES_PREFIX);

        // essentials
        data.put(ESSENTIALS_ROOT, projectService.getBasePathForModule(Module.ESSENTIALS));
    }
}
