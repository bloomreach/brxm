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

package org.onehippo.ide.intellij.project;

import java.io.IOException;

import org.onehippo.ide.intellij.gui.HippoModuleBuilder;
import org.onehippo.ide.intellij.gui.SettingsData;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @version "$Id$"
 */
public final class ProjectUtils {


    private ProjectUtils() {
    }

    public static void replaceVariables(final VirtualFile virtualFile, final Module module, final HippoModuleBuilder hippoModuleBuilder) {
        final Project project = module.getProject();
        final VirtualFile rootFile = project.getBaseDir();
        final SettingsData data = new SettingsData();
        final VirtualFile root = rootFile.findFileByRelativePath("pom.xml");
        replacePomPlaceholders(data, root);
        /*final FileTemplate template = FileTemplateManager.getInstance().getCodeTemplate(module.getModuleFilePath());*/


    }

    private static void replacePomPlaceholders(final SettingsData data, final VirtualFile cmsFile) {
        if (cmsFile != null) {

            try {
                String templateText = VfsUtilCore.loadText(cmsFile);
                templateText = StringUtil.replace(templateText, "${GROUP_ID}", data.getGroupId());
                templateText = StringUtil.replace(templateText, "${VERSION}", data.getVersion());
                templateText = StringUtil.replace(templateText, "${GROUP_ID}", data.getGroupId());
                templateText = StringUtil.replace(templateText, "${ARTIFACT_ID}", data.getVendor());
                VfsUtil.saveText(cmsFile, templateText);
            } catch (IOException e) {

            }

        }
    }
}
