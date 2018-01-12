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

import javax.inject.Inject;

import org.dom4j.Element;
import org.onehippo.cms7.essentials.plugin.sdk.model.MavenDependency;
import org.onehippo.cms7.essentials.plugin.sdk.service.MavenAssemblyService;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.Dom4JUtils;
import org.springframework.stereotype.Service;

@Service
public class MavenAssemblyServiceImpl implements MavenAssemblyService {

    @Inject private ProjectService projectService;

    @Override
    public boolean addDependencySet(final String descriptorFilename, final String outputDirectory,
                                    final String outputFileNameMapping, final boolean useProjectArtifact,
                                    final String scope, final MavenDependency dependency) {
        return update(descriptorFilename, doc -> {
            Element component = (Element) doc.getRootElement().selectSingleNode("/component");
            Element dependencySets = (Element) component.selectSingleNode("*[name()='dependencySets']");
            if (dependencySets == null) {
                dependencySets = Dom4JUtils.addIndentedElement(component, "dependencySets");
            }
            Element dependencySet = Dom4JUtils.addIndentedElement(dependencySets, "dependencySet");
            Dom4JUtils.addIndentedElement(dependencySet, "useProjectArtifact", Boolean.toString(useProjectArtifact));
            Dom4JUtils.addIndentedElement(dependencySet, "outputDirectory", outputDirectory);
            Dom4JUtils.addIndentedElement(dependencySet, "outputFileNameMapping", outputFileNameMapping);
            Dom4JUtils.addIndentedElement(dependencySet, "scope", scope);

            addIncludeToDependencySet(dependencySet, dependency);
        });
    }

    @Override
    public boolean addIncludeToFirstDependencySet(final String descriptorFilename, final MavenDependency dependency) {
        return update(descriptorFilename, doc -> {
            Element dependencySet = (Element) doc.selectSingleNode("//*[name()='dependencySet']");
            addIncludeToDependencySet(dependencySet, dependency);
        });
    }

    private void addIncludeToDependencySet(final Element dependencySet, final MavenDependency dependency) {
        Element includes = (Element)dependencySet.selectSingleNode("./*[name()='includes']");
        if (includes == null) {
            includes = Dom4JUtils.addIndentedElement(dependencySet, "includes");
        }
        final String include = makeInclude(dependency);
        if (includes.selectNodes("*[text()='" + include + "']").isEmpty()) {
            Dom4JUtils.addIndentedElement(includes, "include", include);
        }
    }

    private String makeInclude(final MavenDependency dependency) {
        String include = String.format("%s:%s", dependency.getGroupId(), dependency.getArtifactId());
        if (dependency.getType() != null) {
            include  = String.format("%s:%s", include, dependency.getType());
        }
        return include;
    }

    private boolean update(final String descriptorFilename, final Dom4JUtils.Modifier modifier) {
        final File descriptorFile = projectService.getAssemblyFolderPath().resolve(descriptorFilename).toFile();
        return Dom4JUtils.update(descriptorFile, modifier);
    }
}
