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

import java.io.File;

import javax.swing.Icon;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onehippo.ide.intellij.factory.HippoTemplatesFactory;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;

/**
 * @version "$Id$"
 */
public class HippoEssentialsProject extends WebProjectTemplate {

    private static final Logger log = Logger.getInstance(HippoEssentialsProject.class);

    public static final String POM_XML = "pom.xml";
    public static final String WEB_FRAGMENT_XML = "web-fragment.xml";

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

    @SuppressWarnings("InstanceofInterfaces")
    @Override
    public void generateProject(@NotNull final Project project, @NotNull final VirtualFile baseDirectory, @NotNull final Object settings, @NotNull final Module module) {

        if (!(settings instanceof SettingsData)) {
            return;
        }

        if (baseDirectory.getCanonicalPath() == null) {
            return;
        }

        StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {

                        final PsiDirectory rootDirectory = PsiManager.getInstance(project).findDirectory(getVirtualFile(baseDirectory.getCanonicalPath()));

                        if (rootDirectory == null) {
                            return;
                        }

                        final SettingsData mySettings = (SettingsData) settings;
                        // create pom:
                        createFile(rootDirectory, mySettings, POM_XML, HippoTemplatesFactory.HippoTemplate.ESSENTIALS_POM_TEMPLATE);
                        // create web-fragment:
                        createFile(rootDirectory, mySettings, WEB_FRAGMENT_XML, HippoTemplatesFactory.HippoTemplate.ESSENTIALS_WEB_FRAGMENT_TEMPLATE);
                        VirtualFileManager.getInstance().syncRefresh();


                    }

                    private void createFile(final PsiDirectory directory, final SettingsData mySettings, final String fileName, final HippoTemplatesFactory.HippoTemplate template) {
                        try {
                            directory.checkCreateFile(fileName);
                            HippoTemplatesFactory.createFileFromTemplate(directory, mySettings, fileName, template);
                        } catch (IncorrectOperationException ignored) {
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                    }
                });
            }
        });


    }

    @SuppressWarnings("rawtypes")
    @NotNull
    @Override
    public GeneratorPeer createPeer() {
        return new HippoEssentialsGeneratorPeer();
    }

    private VirtualFile getVirtualFile(String path) {
        File pluginPath = new File(path);

        if (!pluginPath.exists()) {
            return null;
        }

        String url = VfsUtilCore.pathToUrl(pluginPath.getAbsolutePath());

        return VirtualFileManager.getInstance().findFileByUrl(url);
    }

}
