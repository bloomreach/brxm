/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.ctx;


import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;

/**
 * Plugin context is passed to all HippoEssentials plugins.
 *
 * @version "$Id$"
 */
public interface PluginContext extends Serializable, AutoCloseable {


    /**
     * Plugin can store and retrieve data during it's lifecycle
     *
     * @return data stored (if any)
     */
    Map<String, Object> getPluginContextData();

    /**
     * Get context data for given key
     *
     * @param key data key
     * @return object stored (if any)
     */
    Object getPluginContextData(String key);

    /**
     * Adds some data to context storage
     *
     * @param key   string key
     * @param value any object value
     */
    void addPluginContextData(String key, Object value);

    /**
     * Returns JCR session for logged in user.
     * <p>NOTE: session is managed by plugin itself, so logout etc. must be done by plugin</p>
     * <p>Sessions will be logout after plugin is unloaded by plugin framework</p>
     *
     * @return instance of JCR session, with admin rights
     * @see org.onehippo.cms7.essentials.dashboard.DashboardPlugin#onRemove()
     */
    Session getSession();

    /**
     * Returns root of the site directory
     *
     * @return directory of site module
     */
    File getSiteDirectory();

    /**
     * Returns root of the cms directory
     *
     * @return directory of cms module
     */
    File getCmsDirectory();

    /**
     * Check if Hippo  Enterprise project
     *
     * @return true if enterprise project
     */
    boolean isEnterpriseProject();

    /**
     * Package name used for HippoBeans
     *
     * @return beans package name, might be null if not configured
     */
    String beansPackageName();

    /**
     * Package name used for REST components
     *
     * @return REST package name, might be null if not configured
     */
    String restPackageName();

    /**
     * Package name used for HstComponents
     *
     * @return HSt components package name, might be null if not configured
     */
    String getComponentsPackageName();

    /**
     * Sets HST components package name
     *
     * @param componentsPackage name of the components package e.g. {@code org.onehippo.myproject.components}
     */
    void setComponentsPackageName(String componentsPackage);

    /**
     * Path to component package (if all properties are defined like site dir and components package name)
     *
     * @return path to components package or null
     */
    Path getComponentsPackagePath();

    /**
     * Path to HST beans package (if all properties are defined, like site dir and beans package name)
     *
     * @return path to HST beans  package or null
     */
    Path getBeansPackagePath();

    /**
     * Path to HST REST package (if all properties are defined, like site dir and REST package name)
     *
     * @return path to HST REST  package or null
     */
    Path getRestPackagePath();

    /**
     * Sets HST beans package name
     *
     * @param beansPackage name of the beans package e.g. {@code org.onehippo.myproject.beans}
     */
    void setBeansPackageName(String beansPackage);

    /**
     * Sets HST rest package name
     *
     * @param restPackage name of the beans package e.g. {@code org.onehippo.myproject.rest}
     */
    void setRestPackageName(String restPackage);

    /**
     * Namespace prefix of current project
     *
     * @return namespace prefix of current project
     */
    String getProjectNamespacePrefix();

    /**
     * Sets project namespace e.g. {@code myproject}
     *
     * @param namespace namespace prefix
     */
    void setProjectNamespacePrefix(String namespace);

    /**
     * Indicates that all project settings are set
     *
     * @return true if user did project setup, false otherwise.
     */
    boolean hasProjectSettings();

    /**
     * Returns plugin descriptor of currently selected plugin
     *
     * @return Plugin descriptor instance
     */
    Plugin getDescriptor();

    /**
     * Returns service instance for given Plugin descriptor
     *
     * @return instance of PluginConfigService tide to Plugin descriptor
     * @see Plugin
     */
    PluginConfigService getConfigService();

    /**
     * Returns root of java files e.g. {@code /home/user/project/site/src/main/java}
     *
     * @return root of java file folder like {@code /home/user/project/site/src/main/java}
     */
    String getSiteJavaRoot();

    /**
     * returns pre-filled nr of key value pairs for replacement injection in templates etc.
     *
     * @return object containing a number of items which can be used to inject into templates etc.
     */
    Map<String, Object> getPlaceholderData();
}
