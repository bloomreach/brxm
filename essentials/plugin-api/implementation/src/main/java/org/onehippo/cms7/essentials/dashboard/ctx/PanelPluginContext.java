/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Iterator;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.JcrPluginConfigService;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
public class PanelPluginContext implements PluginContext {

    private static final Logger log = LoggerFactory.getLogger(PanelPluginContext.class);

    private static final long serialVersionUID = 1L;
    public static final String MAIN_JAVA_PART = File.separator + "src" + File.separator + "main" + File.separator + "java"
            + File.separator;
    private final transient Session session;
    private transient File siteFile;
    private transient EventBus eventBus;
    private String componentsPackage;
    private String beansPackage;
    private String restPackage;
    private String projectNamespace;

    public PanelPluginContext(final Session session, final EventBus eventBus) {
        this.session = session;
        this.eventBus = eventBus;
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
        throw new UnsupportedOperationException("Panel plugin doesn't support plugin descriptor");
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
    public void close() throws Exception {
        log.info("Closing hippo session");
        if (session != null) {
            session.logout();
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
