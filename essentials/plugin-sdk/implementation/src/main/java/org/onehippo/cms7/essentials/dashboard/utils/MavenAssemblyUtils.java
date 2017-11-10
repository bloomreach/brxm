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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.CharBuffer;

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
            dependencySets = component.addText("  ").addElement("dependencySets");
        }

        Element dependencySet = indent(dependencySets, 4).addElement("dependencySet");
        indent(dependencySet, 6).addElement("useProjectArtifact").addText(Boolean.toString(useProjectArtifact));
        indent(dependencySet, 6).addElement("outputDirectory").addText(outputDirectory);
        indent(dependencySet, 6).addElement("outputFileNameMapping").addText(outputFileNameMapping);
        indent(dependencySet, 6).addElement("scope").addText(scope);
        addIncludeToDependencySet(dependencySet, include);
        indent(dependencySet, 4);
        indent(dependencySets, 2);
        indent(component, 0);

        return doc;
    }

    // indent adds a newline and specified number of spaces to the new line
    private static Element indent(Element e, int spaces) {
        e.addText("\n" + CharBuffer.allocate( spaces ).toString().replace( '\0', ' ' ));
        return e;
    }

    /*
     * Add include to dependencySet/includes. Skipped if the include already exists
     */
    static Element addIncludeToDependencySet(Element dependencySet, String include) {
        Element includes = (Element)dependencySet.selectSingleNode("//*[name()='includes']");
        if(includes == null) {
            includes = indent(dependencySet, 6).addElement("includes");
        }
        if(includes.selectNodes("*[text()='" + include + "']").isEmpty()) {
            indent(includes, 8).addElement("include").addText(include);
            indent(includes, 6);
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
