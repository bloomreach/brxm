/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.ctx;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

import com.google.common.base.Function;

import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.onehippo.cms7.essentials.dashboard.service.SettingsService;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DefaultPluginContext implements PluginContext {

    private static final Logger log = LoggerFactory.getLogger(DefaultPluginContext.class);

    @Inject private ProjectService projectService;
    @Inject private SettingsService settingsService;

    private Map<String, Object> placeholderData;

    @Override
    public void addPlaceholderData(final String key, final Object value) {
        getPlaceholderData().put(key, value);
    }

    @Override
    public void addPlaceholderData(final Map<String, Object> data) {
        if (data != null) {
            getPlaceholderData().putAll(data);
        }
    }

    @Override
    public Map<String, Object> getPlaceholderData() {
        if (placeholderData == null) {
            placeholderData = new HashMap<>();
        } else {
            return placeholderData;
        }

        placeholderData.put(EssentialConst.PLACEHOLDER_NAMESPACE, settingsService.getSettings().getProjectNamespace());
        placeholderData.put(EssentialConst.PLACEHOLDER_PROJECT_ROOT, projectService.getBasePathForModule(TargetPom.PROJECT));
        //############################################
        // DATE PLACEHOLDERS
        //############################################
        // current
        final Calendar today = Calendar.getInstance();
        setFolderPlaceholders(EssentialConst.PLACEHOLDER_DATE_REPO_YYYY_MM, EssentialConst.PLACEHOLDER_DATE_FILE_YYYY_MM, today);
        setDatePlaceholder(EssentialConst.PLACEHOLDER_JCR_TODAY_DATE, today);
        placeholderData.put(EssentialConst.PLACEHOLDER_CURRENT_YEAR, today.get(Calendar.YEAR));
        placeholderData.put(EssentialConst.PLACEHOLDER_CURRENT_MONTH, today.get(Calendar.MONTH) + 1);
        // next month
        final Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.MONTH, 1);
        setFolderPlaceholders(EssentialConst.PLACEHOLDER_DATE_REPO_YYYY_MM_NEXT_MONTH, EssentialConst.PLACEHOLDER_DATE_FILE_YYYY_MM_NEXT_MONTH, nextMonth);
        setDatePlaceholder(EssentialConst.PLACEHOLDER_JCR_DATE_NEXT_MONTH, nextMonth);
        // next year
        final Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);
        setFolderPlaceholders(EssentialConst.PLACEHOLDER_DATE_REPO_YYYY_MM_NEXT_YEAR, EssentialConst.PLACEHOLDER_DATE_FILE_YYYY_MM_NEXT_YEAR, nextYear);
        setDatePlaceholder(EssentialConst.PLACEHOLDER_JCR_DATE_NEXT_YEAR, nextYear);
        //############################################
        // TRANSLATION ID (note: translationId or translationId.id can be used)
        //############################################
        placeholderData.put(EssentialConst.PLACEHOLDER_TRANSLATION_ID, new Object() {
            @Override
            public String toString() {
                return UUID.randomUUID().toString();
            }

            Function<String, String> id() {
                return new Function<String, String>() {
                    @Override
                    public String apply(final String input) {
                        return UUID.randomUUID().toString();
                    }
                };
            }
        });
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_ROOT, projectService.getBasePathForModule(TargetPom.SITE));
        final Path siteWebRoot = projectService.getWebApplicationRootPathForModule(TargetPom.SITE);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_WEB_ROOT, siteWebRoot);
        placeholderData.put(EssentialConst.PLACEHOLDER_JAVASCRIPT_ROOT, siteWebRoot.resolve("js"));
        placeholderData.put(EssentialConst.PLACEHOLDER_IMAGES_ROOT, siteWebRoot.resolve("images"));
        placeholderData.put(EssentialConst.PLACEHOLDER_CSS_ROOT, siteWebRoot.resolve("css"));
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_WEB_INF_ROOT, siteWebRoot.resolve("WEB-INF"));
        placeholderData.put(EssentialConst.PLACEHOLDER_JSP_ROOT, siteWebRoot.resolve("WEB-INF").resolve("jsp"));
        final Path siteResourcesRoot = projectService.getResourcesRootPathForModule(TargetPom.SITE);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_RESOURCES, siteResourcesRoot);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_OVERRIDE_FOLDER, siteResourcesRoot.resolve("META-INF").resolve("hst-assembly").resolve("overrides"));

        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_ROOT, projectService.getBasePathForModule(TargetPom.CMS));
        final Path cmsWebRoot = projectService.getWebApplicationRootPathForModule(TargetPom.CMS);
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_WEB_ROOT, cmsWebRoot);
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_WEB_INF_ROOT, cmsWebRoot.resolve("WEB-INF"));
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_RESOURCES, projectService.getResourcesRootPathForModule(TargetPom.CMS).toString());

        final Path webFilesResourcesRoot = projectService.getResourcesRootPathForModule(TargetPom.REPOSITORY_DATA_WEB_FILES);
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_RESOURCES, webFilesResourcesRoot);
        final Path webFilesRoot = webFilesResourcesRoot.resolve("site");
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_ROOT, webFilesRoot);
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_FREEMARKER_ROOT, webFilesRoot.resolve("freemarker"));
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_CSS_ROOT, webFilesRoot.resolve("css"));
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_JS_ROOT, webFilesRoot.resolve("js"));
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_IMAGES_ROOT, webFilesRoot.resolve("images"));
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_PREFIX, EssentialConst.WEBFILES_PREFIX);
        // packages
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_PACKAGE, settingsService.getSettings().getSelectedBeansPackage());
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_PACKAGE, settingsService.getSettings().getSelectedRestPackage());
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_PACKAGE, settingsService.getSettings().getSelectedComponentsPackage());
        placeholderData.put(EssentialConst.PLACEHOLDER_PROJECT_PACKAGE, settingsService.getSettings().getSelectedProjectPackage());
        // folders
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_FOLDER, projectService.getBeansPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_FOLDER, projectService.getRestPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_FOLDER, projectService.getComponentsPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_TMP_FOLDER, System.getProperty("java.io.tmpdir"));
        // essentials
        placeholderData.put(EssentialConst.PLACEHOLDER_ESSENTIALS_ROOT, projectService.getBasePathForModule(TargetPom.ESSENTIALS));
        return placeholderData;
    }

    private void setFolderPlaceholders(final String repoPlaceholder, final String filePlaceholder, final Calendar calendarInstance) {
        final Date today = calendarInstance.getTime();
        DateFormat formatter = new SimpleDateFormat(EssentialConst.REPO_FOLDER_FORMAT);
        placeholderData.put(repoPlaceholder, formatter.format(today));
        formatter = new SimpleDateFormat("yyyy" + File.separator + "MM");
        final String fileFolder = formatter.format(today);
        placeholderData.put(filePlaceholder, fileFolder);
    }

    private void setDatePlaceholder(final String placeholderName, final Calendar calendarInstance) {
        try {
            calendarInstance.set(Calendar.SECOND, 0);
            calendarInstance.set(Calendar.MILLISECOND, 0);
            final String jcrDate = ValueFactoryImpl.getInstance().createValue(calendarInstance).getString();
            placeholderData.put(placeholderName, jcrDate);
        } catch (RepositoryException e) {
            log.error("Error setting date instance", e);
            placeholderData.put(placeholderName, "1970-01-01T01:00:00.000+01:00");
        }
    }
}
