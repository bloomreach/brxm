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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MavenAssemblyUtils adds or removes items to be part of the distribution
 */
public class MavenAssemblyUtils {
    private MavenAssemblyUtils() {
        throw new IllegalStateException("Utility class");
    }
    private static Logger log = LoggerFactory.getLogger(MavenAssemblyUtils.class);

    /**
     *  Add dependency set. This will add dependencySets if not existing, resulting in
     *  <dependencySets>
     *    <dependencySet>
     *      <outputDirectory>webapps</outputDirectory>
     *      <outputFileNameMapping>hippo-awesome-webapp.war</outputFileNameMapping>
     *      <includes>
     *        <include>org.onehippo.cms:hippo-awesome-webapp:war</include>
     *      </includes>
     *    </dependencySet>
     *  </dependencySets>
     *
     * If the dependencySets parent does not yet exist it will be created
     * The dependencySet does not have an identifier and no check is done for duplicates
     *
     *  @param include include must be in the format group:artifact:type
     */
    static Document addDependencySet(Document doc, String outputDirectory, String outputFileNameMapping,
                                     boolean useProjectArtifact, String scope, String include) {
        Element component = doc.getRootElement();
        Element dependencySets = (Element) component.selectSingleNode("*[name() = 'dependencySets']");
        if (dependencySets == null) {
            dependencySets = Dom4JUtils.addIndentedElement(component, "dependencySets");
        }

        Element dependencySet = Dom4JUtils.addIndentedElement(dependencySets, "dependencySet");
        Dom4JUtils.addIndentedElement(dependencySet, "useProjectArtifact", Boolean.toString(useProjectArtifact));
        Dom4JUtils.addIndentedElement(dependencySet, "outputDirectory", outputDirectory);
        Dom4JUtils.addIndentedElement(dependencySet, "outputFileNameMapping", outputFileNameMapping);
        Dom4JUtils.addIndentedElement(dependencySet, "scope", scope);

        addIncludeToDependencySet(dependencySet, include);

        return doc;
    }

    /*
     * Add include to dependencySet/includes. Skipped if the include already exists
     */
    static Element addIncludeToDependencySet(Element dependencySet, String include) {
        Element includes = (Element)dependencySet.selectSingleNode("//*[name()='includes']");
        if (includes == null) {
            includes = Dom4JUtils.addIndentedElement(dependencySet, "includes");
        }
        if (includes.selectNodes("*[text()='" + include + "']").isEmpty()) {
            Dom4JUtils.addIndentedElement(includes, "include", include);
        }
        return dependencySet;
    }

    /*
     * Add new dependencySet to the assembly descriptor file
     */
    public static void addDependencySet(File assemblyDescriptor, String outputDirectory, String outputFileNameMapping,
                                        boolean useProjectArtifact, String scope, String include) {
        try {
            Document doc = new SAXReader().read(assemblyDescriptor);
            doc = addDependencySet(doc, outputDirectory, outputFileNameMapping, useProjectArtifact, scope, include);
            writeResource(doc, assemblyDescriptor);
        } catch (DocumentException | IOException e) {
            log.error("Error adding dependencySet to {}", assemblyDescriptor.getAbsolutePath(), e);
        }
    }

    /**
     * Add include to the first dependencySet in the assembly descriptor file.
     * At least one dependencySets/DependencySet must exist
     *
     * @param assemblyDescriptor Maven assembly file
     * @param include the coordinates of the dependency to include
     */
    public static void addIncludeToFirstDependencySet(File assemblyDescriptor, String include) {
        try {
            Document doc = new SAXReader().read(assemblyDescriptor);
            Element ds = (Element) doc.selectSingleNode("//*[name()='dependencySet']");
            addIncludeToDependencySet(ds, include);
            writeResource(doc, assemblyDescriptor);
        } catch (DocumentException | IOException e) {
            log.error("Error adding include to first dependencySet in {}" + assemblyDescriptor.getAbsolutePath(), e);
        }
    }

    private static void writeResource(Document doc, File target) throws IOException {
        FileWriter writer = new FileWriter(target);
        doc.write(writer);
        writer.close();
    }
}
