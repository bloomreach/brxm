/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.MavenRepository;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.dashboard.services.MavenRepositoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_GROUP_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_RELEASE_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_REPO_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_REPO_NAME;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_REPO_URL;

/**
 * @version "$Id$"
 */
public final class EnterpriseUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EnterpriseUtils.class);
    private static MavenRepository ENTERPRISE_REPOSITORY = new MavenRepository();
    private static final MavenRepositoryService repositoryService = new MavenRepositoryServiceImpl();

    static {
        ENTERPRISE_REPOSITORY.setId(ENT_REPO_ID);
        ENTERPRISE_REPOSITORY.setName(ENT_REPO_NAME);
        ENTERPRISE_REPOSITORY.setUrl(ENT_REPO_URL);
        final MavenRepository.Policy releasePolicy = new MavenRepository.Policy();
        releasePolicy.setUpdatePolicy("never");
        releasePolicy.setChecksumPolicy("fail");
        ENTERPRISE_REPOSITORY.setReleasePolicy(releasePolicy);
    }

    private EnterpriseUtils() {
    }

    public static boolean upgradeToEnterpriseProject(final PluginContext context) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);
        if (pomModel == null) {
            return false;
        }

        final Parent parent = pomModel.getParent();
        if (parent == null) {
            LOG.error("No parent element found in project root POM, cannot upgrade to Enterprise version.");
            return false;
        }

        if (!ENT_GROUP_ID.equals(parent.getGroupId()) || !ENT_RELEASE_ID.equals(parent.getArtifactId())) {
            parent.setArtifactId(ENT_RELEASE_ID);
            parent.setGroupId(ENT_GROUP_ID);
            if (!MavenModelUtils.writePom(pomModel, ProjectUtils.getPomFile(context, TargetPom.PROJECT))) {
                return false;
            }
        }

        return repositoryService.addRepository(context, TargetPom.PROJECT, ENTERPRISE_REPOSITORY)
                && addEnterpriseCmsDependencies(context);
    }

    private static boolean addEnterpriseCmsDependencies(final PluginContext context) {
        final Model cmsModel = ProjectUtils.getPomModel(context, TargetPom.CMS);
        if (cmsModel != null) {
            final Dependency indicator = new Dependency();
            indicator.setArtifactId("hippo-addon-edition-indicator");
            indicator.setGroupId(ENT_GROUP_ID);
            cmsModel.addDependency(indicator);

            // add enterprise package of app dependencies
            final Dependency enterpriseApp = new Dependency();
            enterpriseApp.setGroupId(ENT_GROUP_ID);
            enterpriseApp.setArtifactId("hippo-enterprise-package-app-dependencies");
            enterpriseApp.setType("pom");
            cmsModel.addDependency(enterpriseApp);

            return MavenModelUtils.writePom(cmsModel, ProjectUtils.getPomFile(context, TargetPom.CMS));
        }
        return false;
    }
}
