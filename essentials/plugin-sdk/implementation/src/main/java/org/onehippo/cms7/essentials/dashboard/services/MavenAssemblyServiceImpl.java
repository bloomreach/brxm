/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;

import javax.inject.Singleton;

import org.dom4j.Element;
import org.onehippo.cms7.essentials.dashboard.service.MavenAssemblyService;
import org.onehippo.cms7.essentials.dashboard.utils.Dom4JUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.springframework.stereotype.Service;

@Service
@Singleton
public class MavenAssemblyServiceImpl implements MavenAssemblyService {
    @Override
    public boolean addDependencySet(final String descriptorFilename, final String outputDirectory,
                                    final String outputFileNameMapping, final boolean useProjectArtifact,
                                    final String scope, final String include) {
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

            addIncludeToDependencySet(dependencySet, include);
        });
    }

    @Override
    public boolean addIncludeToFirstDependencySet(final String descriptorFilename, final String include) {
        return update(descriptorFilename, doc -> {
            Element dependencySet = (Element) doc.selectSingleNode("//*[name()='dependencySet']");
            addIncludeToDependencySet(dependencySet, include);
        });
    }

    private void addIncludeToDependencySet(Element dependencySet, String include) {
        Element includes = (Element)dependencySet.selectSingleNode("./*[name()='includes']");
        if (includes == null) {
            includes = Dom4JUtils.addIndentedElement(dependencySet, "includes");
        }
        if (includes.selectNodes("*[text()='" + include + "']").isEmpty()) {
            Dom4JUtils.addIndentedElement(includes, "include", include);
        }
    }

    private boolean update(final String descriptorFilename, final Dom4JUtils.Modifier modifier) {
        final File descriptorFile = ProjectUtils.getAssemblyFile(descriptorFilename);
        return Dom4JUtils.update(descriptorFile, modifier);
    }
}
