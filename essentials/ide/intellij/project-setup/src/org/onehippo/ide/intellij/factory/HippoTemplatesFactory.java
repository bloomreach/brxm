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

package org.onehippo.ide.intellij.factory;

import java.io.File;
import java.util.Properties;

import javax.swing.Icon;

import org.onehippo.ide.intellij.project.SettingsData;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.lang.Language;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;

/**
 * @version "$Id$"
 */
public class HippoTemplatesFactory implements FileTemplateGroupDescriptorFactory {

    public static final String ESSENTIALS_VERSION = "1.01.02-SNAPSHOT";

    public enum HippoTemplate {
        ESSENTIALS_POM_TEMPLATE("essentials_pom"),
        ESSENTIALS_WEB_FRAGMENT_TEMPLATE("essentials_web_fragment"),
        ESSENTIALS_REST_CLASS_TEMPLATE("essentials_rest_class"),
        ESSENTIALS_PLUGIN_HTML_TEMPLATE("essentials_html_template"),
        ESSENTIALS_PLUGIN_JS_TEMPLATE("essentials_javascript_template"),
        // InstructionPackage section
        ESSENTIALS_PACKAGE_REST_CLASS_TEMPLATE("essentials_package_rest_class"),
        ESSENTIALS_PACKAGE_CLASS_TEMPLATE("essentials_package_class"),
        ESSENTIALS_PACKAGE_INSTRUCTIONS_TEMPLATE("essentials_package_instructions");

        final String name;

        HippoTemplate(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Override
    public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
        String title = "Hippo Essentials templates";
        final Icon icon = IconLoader.getIcon("/icons/logo.png");
        final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(title, icon);
        for (HippoTemplate template : HippoTemplate.values()) {
            group.addTemplate(new FileTemplateDescriptor(template.getName(), icon));
        }
        return group;
    }


    public static PsiElement createFileFromTemplate(PsiElement directory, final SettingsData data, String fileName, HippoTemplate template) {
        String myFileName = fileName;
        final FileTemplate fileTemplate = FileTemplateManager.getInstance().getInternalTemplate(template.getName());
        final Properties properties = new Properties(FileTemplateManager.getInstance().getDefaultProperties());
        final String projectName = data.getProjectName();
        final String projectNameCapitalized = Character.toUpperCase(projectName.charAt(0)) + projectName.substring(1);
        properties.setProperty("PLUGIN_NAME_CAPITALIZED", projectNameCapitalized);
        properties.setProperty("PLUGIN_NAME", projectName);
        properties.setProperty("PLUGIN_GROUP", data.getPluginGroup());
        properties.setProperty("PACKAGE", data.getProjectPackage());
        properties.setProperty("GROUP_ID", data.getGroupId());
        properties.setProperty("ARTIFACT_ID", data.getArtifactId());
        properties.setProperty("VENDOR", data.getVendor());
        properties.setProperty("VERSION", data.getVersion());
        properties.setProperty("ESSENTIALS_VERSION", ESSENTIALS_VERSION);
        String text;
        try {
            text = fileTemplate.getText(properties);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load template for " + template.getName(), e);
        }
        final PsiFileFactory factory = PsiFileFactory.getInstance(directory.getProject());
        if ((new File(myFileName)).exists()) {
            throw new RuntimeException("File already exists");
        }
        Language language = null;
        if (myFileName.endsWith("xml")) {
            language = Language.findLanguageByID("XML");
        } else if (myFileName.endsWith("js")) {
            language = Language.findLanguageByID("JavaScript");
        } else if (myFileName.endsWith("java")) {
            myFileName = Character.toUpperCase(myFileName.charAt(0)) + myFileName.substring(1);

            language = Language.findLanguageByID("JAVA");
        } else if (myFileName.endsWith("html")) {
            language = Language.findLanguageByID("HTML");
        }

        if (language == null) {
            return null;
        }
        final PsiFile file = factory.createFileFromText(myFileName, language, text);
        if (file == null) {
            return null;
        }

        return directory.add(file);
    }
}
