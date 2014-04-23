/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.onehippo.cms7.essentials.dashboard.config.JcrPluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.model.Plugin;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Plugin plugin;
    private final Multimap<String, Object> contextData = ArrayListMultimap.create();
    private transient File siteFile;
    private String componentsPackage;
    private String beansPackage;
    private String restPackage;
    private String projectNamespace;
    private Map<String, Object> placeholderData;

    public DefaultPluginContext(final Plugin plugin) {
        this.plugin = plugin;
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
            siteFile = ProjectUtils.getSite();
        }
        return siteFile;
    }

    public void setSiteFile(final File siteFile) {
        this.siteFile = siteFile;
    }

    @Override
    public File getCmsDirectory() {
        return ProjectUtils.getCms();
    }

    @Override
    public boolean isEnterpriseProject() {
        // TODO implement
        return false;
    }

    @Override
    public String beansPackageName() {
        return beansPackage;
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
        return createPath(beansPackage);
    }

    @Override
    public Path getRestPackagePath() {
        return createPath(restPackage);
    }

    private Path createPath(final String packageName) {
        final File siteDirectory = ProjectUtils.getSite();
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
    public Plugin getDescriptor() {
        return plugin;
    }

    @Override
    public PluginConfigService getConfigService() {
        return new JcrPluginConfigService(this);
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
        //
        //############################################


        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_ROOT, ProjectUtils.getSite().getAbsolutePath());
        final String siteWebRoot = ProjectUtils.getSite().getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_WEB_ROOT;
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_WEB_ROOT, siteWebRoot);

        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_RESOURCES, getSiteDirectory() + MAIN_RESOURCE_PART);

        final String cmsWebRoot = ProjectUtils.getCms().getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_WEB_ROOT;
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_WEB_ROOT, cmsWebRoot);
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_RESOURCES, getCmsDirectory() + MAIN_RESOURCE_PART);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_OVERRIDE_FOLDER, ProjectUtils.getSite().getAbsolutePath()
                + File.separator + EssentialConst.PATH_REL_OVERRIDE);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_FREEMARKER_ROOT, ProjectUtils.getSite().getAbsolutePath() + File.separator + EssentialConst.FREEMARKER_RELATIVE_FOLDER);
        placeholderData.put(EssentialConst.PLACEHOLDER_JSP_ROOT, ProjectUtils.getSiteJspFolder());
        placeholderData.put(EssentialConst.PLACEHOLDER_JAVASCRIPT_ROOT, siteWebRoot + File.separator + "js");
        placeholderData.put(EssentialConst.PLACEHOLDER_CSS_ROOT, siteWebRoot + File.separator + "css");
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_ROOT, ProjectUtils.getCms().getAbsolutePath());
        // packages
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_PACKAGE, beansPackage);
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_PACKAGE, restPackage);
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_PACKAGE, componentsPackage);
        // folders
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_FOLDER, getBeansPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_FOLDER, getRestPackagePath().toString());
        final Path componentsPackagePath = getComponentsPackagePath();
        if (componentsPackagePath != null) {
            placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_FOLDER, componentsPackagePath.toString());
        }
        placeholderData.put(EssentialConst.PLACEHOLDER_TMP_FOLDER, System.getProperty("java.io.tmpdir"));


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
            final String jcrDate = ValueFactoryImpl.getInstance().createValue(calendarInstance).getString();
            placeholderData.put(placeholderName, jcrDate);
        } catch (RepositoryException e) {
            log.error("Error setting date instance", e);
            placeholderData.put(placeholderName, "1970-01-01T01:00:00.000+01:00");
        }
    }


}
