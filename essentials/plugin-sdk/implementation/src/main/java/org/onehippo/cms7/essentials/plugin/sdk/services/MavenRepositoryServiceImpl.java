/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.onehippo.cms7.essentials.plugin.sdk.model.MavenRepository;
import org.onehippo.cms7.essentials.plugin.sdk.model.TargetPom;
import org.onehippo.cms7.essentials.plugin.sdk.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MavenRepositoryServiceImpl implements MavenRepositoryService {

    private static final Logger LOG = LoggerFactory.getLogger(MavenRepositoryServiceImpl.class);

    @Inject private ProjectService projectService;

    @Override
    public boolean addRepository(final TargetPom module, final MavenRepository repository) {
        if (StringUtils.isBlank(repository.getUrl())) {
            LOG.error("Failed to add Maven repository '{}' to module '{}', no repository URL specified.", repository, module.getName());
            return false;
        }

        return updatePomModel(module, model -> {
            if (hasRepository(model, repository)) {
                return true;
            }
            model.addRepository(forMaven(repository));
            return MavenModelUtils.writePom(model, projectService.getPomPathForModule(module).toFile());
        });
    }

    private Repository forMaven(final MavenRepository repository) {
        final Repository repo = new Repository();

        repo.setId(repository.getId());
        repo.setName(repository.getName());
        repo.setUrl(repository.getUrl());

        if (repository.getReleasePolicy() != null) {
            repo.setReleases(forMaven(repository.getReleasePolicy()));
        }
        if (repository.getSnapshotPolicy() != null) {
            repo.setSnapshots(forMaven(repository.getSnapshotPolicy()));
        }

        return repo;
    }

    private RepositoryPolicy forMaven(final MavenRepository.Policy policy) {
        final RepositoryPolicy p = new RepositoryPolicy();

        p.setEnabled(policy.getEnabled());
        p.setUpdatePolicy(policy.getUpdatePolicy());
        p.setChecksumPolicy(policy.getChecksumPolicy());

        return p;
    }

    private boolean hasRepository(final Model model, final MavenRepository repository) {
        return model.getRepositories()
                .stream()
                .anyMatch(r -> repository.getUrl().equals(r.getUrl()));
    }

    private boolean updatePomModel(final TargetPom module, final Predicate<Model> updater) {
        final File pom = projectService.getPomPathForModule(module).toFile();
        final Model model = MavenModelUtils.readPom(pom);
        if (model == null) {
            LOG.error("Unable to load model for pom.xml of module '{}'.", module.getName());
            return false;
        }
        return updater.test(model);
    }
}
