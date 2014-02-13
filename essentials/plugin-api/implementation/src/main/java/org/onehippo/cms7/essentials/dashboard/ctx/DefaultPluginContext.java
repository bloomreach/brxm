/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.ctx;


import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.JcrPluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
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
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginContext.class);
    private static final long serialVersionUID = 1L;
    private final transient Session session;
    private final Plugin plugin;
    private final Multimap<String, Object> contextData = ArrayListMultimap.create();
    private transient File siteFile;
    private String componentsPackage;
    private String beansPackage;
    private String restPackage;
    private String projectNamespace;

    public DefaultPluginContext(final Session session, final Plugin plugin) {
        this.session = session;
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

    public Session getSession() {
        return session;
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
        final Map<String, Object> placeholderData = new HashMap<>();
        placeholderData.put(EssentialConst.PLACEHOLDER_NAMESPACE, getProjectNamespacePrefix());
        placeholderData.put(EssentialConst.PLACEHOLDER_PROJECT_ROOT, ProjectUtils.getBaseProjectDirectory());
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_ROOT, ProjectUtils.getSite().getAbsolutePath());
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_WEB_ROOT, ProjectUtils.getSite().getAbsolutePath() + File.separator + EssentialConst.PATH_REL_WEB_ROOT);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_FREEMARKER_ROOT, ProjectUtils.getSite().getAbsolutePath() + File.separator + EssentialConst.FREEMARKER_RELATIVE_FOLDER);
        placeholderData.put(EssentialConst.PLACEHOLDER_JSP_ROOT, ProjectUtils.getSiteJspFolder());
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_ROOT, ProjectUtils.getCms().getAbsolutePath());
        // packages
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_PACKAGE, beansPackage);
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_PACKAGE, restPackage);
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_PACKAGE, componentsPackage);
        // folders
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_FOLDER, getBeansPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_FOLDER, getRestPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_FOLDER, getComponentsPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_TMP_FOLDER, System.getProperty("java.io.tmpdir"));

        return placeholderData;
    }

    @Override
    public void close() throws Exception {
        log.info("Closing hippo session");
        if (session != null) {
            session.logout();
        }
    }


}
