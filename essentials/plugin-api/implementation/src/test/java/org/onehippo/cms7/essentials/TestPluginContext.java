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

package org.onehippo.cms7.essentials;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.model.Plugin;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class TestPluginContext extends DefaultPluginContext {

    private static final Logger log = LoggerFactory.getLogger(TestPluginContext.class);
    private static final long serialVersionUID = 1L;


    private final MemoryRepository repository;

    public TestPluginContext(final MemoryRepository repository, final Plugin plugin) {
        super(plugin);
        this.repository = repository;
    }


    public Map<String, Object> getTestContextPlaceholders() {

        final Map<String, Object> placeholderData = new HashMap<>();
        final String tmpDir = System.getProperty("java.io.tmpdir");
        placeholderData.put(EssentialConst.PLACEHOLDER_TMP_FOLDER, tmpDir);
        placeholderData.put(EssentialConst.PLACEHOLDER_NAMESPACE, getProjectNamespacePrefix());
        placeholderData.put(EssentialConst.PLACEHOLDER_PROJECT_ROOT, ProjectUtils.getBaseProjectDirectory());
        final File site = new File(tmpDir + File.separator + "site");
        if (!site.exists()) {
            site.mkdir();
        }
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_ROOT, site.getAbsolutePath());
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_WEB_ROOT, site.getAbsolutePath() + File.separator + EssentialConst.PATH_REL_WEB_ROOT);
        placeholderData.put(EssentialConst.PLACEHOLDER_SITE_FREEMARKER_ROOT, site.getAbsolutePath() + File.separator + EssentialConst.FREEMARKER_RELATIVE_FOLDER);
        final File siteJspFolder = new File(site.getAbsolutePath() + File.separator + "jsp");
        if (!siteJspFolder.exists()) {
            siteJspFolder.mkdir();
        }
        final File cmsFolder = new File(tmpDir + File.separator + "cms");
        if (!cmsFolder.exists()) {
            cmsFolder.mkdir();
        }
        placeholderData.put(EssentialConst.PLACEHOLDER_JSP_ROOT, siteJspFolder);
        placeholderData.put(EssentialConst.PLACEHOLDER_CMS_ROOT, cmsFolder.getAbsolutePath());
        // packages
        placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_PACKAGE, "org.onehippo.cms7.essentials.dashboard.test.beans");
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_PACKAGE, "org.onehippo.cms7.essentials.dashboard.test.rest");
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_PACKAGE, "org.onehippo.cms7.essentials.dashboard.test.components");
        // folders
        /*placeholderData.put(EssentialConst.PLACEHOLDER_BEANS_FOLDER, getBeansPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_REST_FOLDER, getRestPackagePath().toString());
        placeholderData.put(EssentialConst.PLACEHOLDER_COMPONENTS_FOLDER, getComponentsPackagePath().toString());*/
        final File essentials = new File(tmpDir + File.separator + "essentials");
        if (!essentials.exists()) {
            essentials.mkdir();
        }
        placeholderData.put(EssentialConst.PLACEHOLDER_ESSENTIALS_ROOT, essentials.getAbsolutePath());
        return placeholderData;
    }

    @Override
    public String getProjectNamespacePrefix() {
        final String projectNamespacePrefix = super.getProjectNamespacePrefix();
        if (Strings.isNullOrEmpty(projectNamespacePrefix)) {
            return BaseTest.PROJECT_NAMESPACE_TEST;
        }
        return projectNamespacePrefix;
    }

    @Override
    public Session createSession() {
        try {
            return repository.getSession();
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return null;
    }




}
