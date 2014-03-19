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

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.lang.javascript.boilerplate.AbstractGithubTagDownloadedProjectGenerator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.WebProjectGenerator;
import com.intellij.platform.templates.github.GithubTagInfo;

/**
 * @version "$Id$"
 */
public class HippoEmptyProject extends WebProjectTemplate {

    public static final String HIPPO_PROJECT_TEMPLATE = "hippo-project-template";

    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/logo.png");
    }

    @NotNull
    @Override
    public ModuleBuilder createModuleBuilder() {
        return super.createModuleBuilder();
    }


    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Essentials Plugin Setup";
    }

    @Override
    @NotNull
    public String getDescription() {
        return "<html>Hippo Essentials plugin project setup  <a href='http://www.onehippo.org'>http://www.onehippo.org</a></html>";
    }

    @Override
    @Nullable
    public Integer getPreferredDescriptionWidth() {
        return 390;
    }

    @Override
    public void generateProject(@NotNull final Project project, @NotNull final VirtualFile virtualFile, @NotNull final Object o, @NotNull final Module module) {

    }

    @SuppressWarnings("rawtypes")
    @NotNull
    @Override
    public GeneratorPeer createPeer() {
        return new HippoEssentialsGeneratorPeer();
    }


}
