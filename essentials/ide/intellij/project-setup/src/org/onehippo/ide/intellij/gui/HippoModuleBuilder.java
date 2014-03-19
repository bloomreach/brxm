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

package org.onehippo.ide.intellij.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onehippo.ide.intellij.project.HippoEssentialsGeneratorPeer;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.openapi.module.WebModuleBuilder;

/**
 * @version "$Id$"
 */
public class HippoModuleBuilder extends WebModuleBuilder {

    private HippoEssentialsGeneratorPeer peer;

    public HippoModuleBuilder(@NotNull final WebProjectTemplate<?> webProjectTemplate) {
        super(webProjectTemplate);
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull final SettingsStep settingsStep) {
        final ModuleWizardStep moduleWizardStep = super.modifySettingsStep(settingsStep);
        settingsStep.addSettingsComponent(getPeer().getComponent());
        return moduleWizardStep;


    }


    public HippoEssentialsGeneratorPeer getPeer() {
        if (peer == null) {
            peer = new HippoEssentialsGeneratorPeer();
        }
        return peer;
    }

}
