/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.onehippo.cms7.essentials.dashboard.config.FilePluginService;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.model.ProjectSettings;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @version "$Id$"
 */
public class DefaultPluginContext implements PluginContext {

    public static final String MAIN_JAVA_PART = File.separator + "src" + File.separator + "main" + File.separator + "java"
            + File.separator;
    public static final String MAIN_RESOURCE_PART = File.separator + "src" + File.separator + "main" + File.separator + "resources";
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginContext.class);
    private static final long serialVersionUID = 1L;

    private final Multimap<String, Object> contextData = ArrayListMultimap.create();
    private transient File siteFile;
    private String componentsPackage;
    private String beansPackage;
    private String projectPackage;
    private String restPackage;
    private String projectNamespace;
    private Map<String, Object> placeholderData;
    private ProjectSettings projectSettings;

    public DefaultPluginContext() {
        final ProjectSettings document = getProjectSettings();
        if (document != null) {
            setBeansPackageName(document.getSelectedBeansPackage());
            setComponentsPackageName(document.getSelectedComponentsPackage());
            setRestPackageName(document.getSelectedRestPackage());
            setProjectNamespacePrefix(document.getProjectNamespace());
            setProjectPackageName(document.getSelectedProjectPackage());
        }
    }


    @Override
    public ProjectSettings getProjectSettings() {
        if (projectSettings != null) {
            return projectSettings;
        }

        try (PluginConfigService service = getConfigService()) {
            projectSettings =  service.read(ProjectSettingsBean.DEFAULT_NAME, ProjectSettingsBean.class);
        } catch (Exception e) {
            log.error("Error reading project settings", e);
        }
        return projectSettings;
    }

    @Override
    public void setProjectSettings(final ProjectSettings projectSettings) {
        this.projectSettings = projectSettings;
    }

    @Override
    public Multimap<String, Object> getPluginContextData() {
        return contextData;
    }

    @Override
    public Collection<Object> getPluginContextData(final String key) {
        return contextData.get(key);
    }

    @Override
    public void addPluginContextData(final String key, final Object value) {
        contextData.put(key, value);

    }

    @Override
    public Session createSession() {
        return GlobalUtils.createSession();
    }

    @Override
    public File getSiteDirectory() {
        if (siteFile == null) {
            siteFile = ProjectUtils.getSite(this);
        }
        return siteFile;
    }

    public void setSiteFile(final File siteFile) {
        this.siteFile = siteFile;
    }

    @Override
    public File getCmsDirectory() {
        return ProjectUtils.getCms(this);
    }

    @Override
    public File getEssentialsDirectory() {
        return ProjectUtils.getEssentialsFolderName(this);
    }

    @Override
    public String getEssentialsResourcePath() {
        return ProjectUtils.getEssentialsFolderName(this).getAbsolutePath() + MAIN_RESOURCE_PART;
    }

    @Override
    public String beansPackageName() {
        return beansPackage;
    }

    @Override
    public String getProjectPackageName() {
        return projectPackage;
    }

    @Override
    public String restPackageName() {
        return restPackage;
    }

    @Override
    public String getComponentsPackageName() {

        return componentsPackage;
    }

    @Override
    public void setComponentsPackageName(final String componentsPackage) {
        this.componentsPackage = componentsPackage;
    }

    @Override
    public void setRestPackageName(final String restPackage) {
        this.restPackage = restPackage;
    }

    @Override
    public Path getComponentsPackagePath() {
        return createPath(componentsPackage);

    }

    @Override
    public Path getBeansPackagePath() {
        return createBeansPath(beansPackage);
    }


    @Override
    public Path getBeansRootPath() {
        if (projectSettings != null && projectSettings.getBeansFolder() != null) {
            final String beansFolder = projectSettings.getBeansFolder();
            final File rootFolder = ProjectUtils.getProjectRootFolder();
            return new File(rootFolder.getAbsolutePath() + File.separator + beansFolder).toPath();
        }

        return null;
    }

    @Override
    public Path getRestPackagePath() {
        return createPath(restPackage);
    }

    private Path createBeansPath(final String packageName) {

        final Iterator<String> iterator = Splitter.on('.').split(packageName).iterator();
        final Joiner joiner = Joiner.on(File.separator).skipNulls();
        final Path beansRootPath = getBeansRootPath();
        if (beansRootPath !=null) {
            return new File(beansRootPath.toString() + File.separator + joiner.join(iterator)).toPath();
        }
        // default to site folder
        final File  siteDirectory = ProjectUtils.getSite(this);


        if (Strings.isNullOrEmpty(packageName) || siteDirectory == null) {
            log.error("Package: {}, or  project site directory: {}, were not defined", packageName, siteDirectory);
            return null;
        }

        return new File(siteDirectory.getAbsolutePath() + MAIN_JAVA_PART + joiner.join(iterator)).toPath();
    }

    private Path createPath(final String packageName) {
        final File siteDirectory = ProjectUtils.getSite(this);
        if (Strings.isNullOrEmpty(packageName) || siteDirectory == null) {
            log.error("Package: {}, or  project site directory: {}, were not defined", packageName, siteDirectory);
            return null;
        }
        final Iterator<String> iterator = Splitter.on('.').split(packageName).iterator();
        final Joiner joiner = Joiner.on(File.separator).skipNulls();
        return new File(siteDirectory.getAbsolutePath() + MAIN_JAVA_PART + joiner.join(iterator)).toPath();
    }

    @Override
    public void setBeansPackageName(final String beansPackage) {
        this.beansPackage = beansPackage;
    }

    @Override
    public void setProjectPackageName(final String projectPackage) {
        this.projectPackage = projectPackage;
    }

    @Override
    public String getProjectNamespacePrefix() {
        return projectNamespace;
    }

    @Override
    public void setProjectNamespacePrefix(final String namespace) {
        this.projectNamespace = namespace;
    }

    @Override
    public boolean hasProjectSettings() {
        return projectNamespace != null && componentsPackage != null && beansPackage != null;
    }

    @Override
    public PluginConfigService getConfigService() {
        return new FilePluginService(this);
    }

    @Override
    public String getSiteJavaRoot() {
        return getSiteDirectory().getAbsolutePath() + MAIN_JAVA_PART;
    }

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

    /**
     * @inherit
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_NAMESPACE
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_PROJECT_ROOT
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_CMS_ROOT
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_SITE_ROOT
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_JSP_ROOT
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_TARGET
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_SOURCE
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_BEANS_FOLDER
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_BEANS_PACKAGE
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_COMPONENTS_FOLDER
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_COMPONENTS_PACKAGE
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_REST_PACKAGE
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_REST_FOLDER
     * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PLACEHOLDER_TMP_FOLDER
     */
    @Override
    public Map<String, Object> getPlaceholderData() {
        if (placeholderData == null) {
            placeholderData = new HashMap<>();
        } else {
            return placeholderData;
        }


        placeholderData.put(EssentialConst.PLACEHOLDER_NAMESPACE, getProjectNamespacePrefix());
        placeholderData.put(EssentialConst.PLACEHOLDER_PROJECT_ROOT, ProjectUtils.getBaseProjectDirectory());
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
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_ROOT, ProjectUtils.getSite(this).getAbsolutePath());
        final String siteWebRoot = ProjectUtils.getSite(this).getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_WEB_ROOT;
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_WEB_ROOT, siteWebRoot);

        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_RESOURCES, getSiteDirectory() + MAIN_RESOURCE_PART);

        final String cmsWebRoot = ProjectUtils.getCms(this).getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_WEB_ROOT;
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_WEB_ROOT, cmsWebRoot);
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_RESOURCES, getCmsDirectory() + MAIN_RESOURCE_PART);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_OVERRIDE_FOLDER, ProjectUtils.getSite(this).getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_RESOURCES
                + File.separator + EssentialConst.PATH_REL_OVERRIDE);

        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_WEB_INF_ROOT, ProjectUtils.getSite(this).getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_WEB_INF);
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_WEB_INF_ROOT, ProjectUtils.getCms(this).getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_WEB_INF);
        final File webfilesResource = ProjectUtils.getWebfilesResources(this);
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_RESOURCES, webfilesResource.getAbsolutePath());
        final File webfilesFolder = ProjectUtils.getWebfiles(this);
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_ROOT, webfilesFolder.getAbsolutePath());
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_FREEMARKER_ROOT, webfilesFolder.getAbsolutePath() + File.separator + "freemarker");
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_CSS_ROOT, webfilesFolder.getAbsolutePath() + File.separator + "css");
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_JS_ROOT, webfilesFolder.getAbsolutePath() + File.separator + "js");
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_IMAGES_ROOT, webfilesFolder.getAbsolutePath() + File.separator + "images");
        placeholderData.put(EssentialConst.PLACEHOLDER_WEBFILES_PREFIX, EssentialConst.WEBFILES_PREFIX);
        placeholderData.put(EssentialConst.PLACEHOLDER_JSP_ROOT, ProjectUtils.getSiteJspFolder(this));
        placeholderData.put(EssentialConst.PLACEHOLDER_JAVASCRIPT_ROOT, siteWebRoot + File.separator + "js");
        placeholderData.put(EssentialConst.PLACEHOLDER_IMAGES_ROOT, siteWebRoot + File.separator + "images");
        placeholderData.put(EssentialConst.PLACEHOLDER_CSS_ROOT, siteWebRoot + File.separator + "css");
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_ROOT, ProjectUtils.getCms(this).getAbsolutePath());
        // packages
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_PACKAGE, beansPackage);
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_PACKAGE, restPackage);
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_PACKAGE, componentsPackage);
        placeholderData.put(EssentialConst.PLACEHOLDER_PROJECT_PACKAGE, projectPackage);
        // folders
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_FOLDER, getBeansPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_FOLDER, getRestPackagePath().toString());
        final Path componentsPackagePath = getComponentsPackagePath();
        if (componentsPackagePath != null) {
            placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_FOLDER, componentsPackagePath.toString());
        }
        placeholderData.put(EssentialConst.PLACEHOLDER_TMP_FOLDER, System.getProperty("java.io.tmpdir"));
        // essentials
        placeholderData.put(EssentialConst.PLACEHOLDER_ESSENTIALS_ROOT, getEssentialsDirectory().getAbsolutePath());
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
