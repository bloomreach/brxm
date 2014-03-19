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

package org.onehippo.ide.intellij.project;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Strings;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.WebProjectGenerator;

/**
 * @version "$Id$"
 */
public class HippoEssentialsGeneratorPeer implements WebProjectGenerator.GeneratorPeer<SettingsData> {

    private final SettingsData data = new SettingsData();

    private JPanel myMainPanel;
    private JTextField pluginName;
    private JTextField groupId;
    private JTextField artifactId;
    private JTextField version;
    private JTextField vendorName;
    private JCheckBox createRESTClassCheckBox;
    private JTextField packageName;
    private JComboBox<String> pluginGroup;


    public HippoEssentialsGeneratorPeer() {
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return myMainPanel;
    }

    @Override
    public void buildUI(@NotNull final SettingsStep settingsStep) {
        settingsStep.addSettingsField("Vendor", vendorName);
        settingsStep.addSettingsField("Plugin id", pluginName);
        settingsStep.addSettingsField("Group id", groupId);
        settingsStep.addSettingsField("Artifact id", artifactId);
        settingsStep.addSettingsField("Version", version);
        settingsStep.addSettingsField("Create REST skeleton", createRESTClassCheckBox);
        settingsStep.addSettingsField("REST package", packageName);
        settingsStep.addSettingsField("Plugin group", pluginGroup);
    }

    @NotNull
    @Override
    public SettingsData getSettings() {

        getData(data);
        return data;
    }

    @Nullable
    @Override
    public ValidationInfo validate() {
        if (createRESTClassCheckBox.isSelected() && Strings.isNullOrEmpty(packageName.getText())) {
            return new ValidationInfo("Rest package name not provided");
        }

        if (isEmpty(version)
                || isEmpty(artifactId)
                || isEmpty(groupId)
                || isEmpty(vendorName)
                ) {
            return new ValidationInfo("All fields needs to be filled in");
        }
        if (Strings.isNullOrEmpty((String) pluginGroup.getSelectedItem())) {
            return new ValidationInfo("Please select plugin group");
        }


        return null;
    }

    private boolean isEmpty(final JTextField field) {
        return Strings.isNullOrEmpty(field.getText());
    }

    @Override
    public boolean isBackgroundJobRunning() {
        return false;
    }

    @Override
    public void addSettingsStateListener(@NotNull final WebProjectGenerator.SettingsStateListener settingsStateListener) {

    }

    private void createUIComponents() {
    }


    public void setData(SettingsData data) {
        pluginGroup.setSelectedItem(data.getPluginGroup());
        pluginName.setText(data.getProjectName());
        groupId.setText(data.getGroupId());
        version.setText(data.getVersion());
        artifactId.setText(data.getArtifactId());
        vendorName.setText(data.getVendor());
        createRESTClassCheckBox.setSelected(data.isCreateRestSkeleton());
        packageName.setText(data.getProjectPackage());
    }

    public void getData(SettingsData data) {
        data.setPluginGroup((String) pluginGroup.getSelectedItem());
        data.setProjectName(pluginName.getText());
        data.setGroupId(groupId.getText());
        data.setVersion(version.getText());
        data.setArtifactId(artifactId.getText());
        data.setVendor(vendorName.getText());
        data.setCreateRestSkeleton(createRESTClassCheckBox.isSelected());
        data.setProjectPackage(packageName.getText());
    }

    public boolean isModified(SettingsData data) {
        if (pluginName.getText() != null ? !pluginName.getText().equals(data.getProjectName()) : data.getProjectName() != null) {
            return true;
        }
        if (groupId.getText() != null ? !groupId.getText().equals(data.getGroupId()) : data.getGroupId() != null) {
            return true;
        }
        if (version.getText() != null ? !version.getText().equals(data.getVersion()) : data.getVersion() != null) {
            return true;
        }
        if (artifactId.getText() != null ? !artifactId.getText().equals(data.getArtifactId()) : data.getArtifactId() != null) {
            return true;
        }
        if (vendorName.getText() != null ? !vendorName.getText().equals(data.getVendor()) : data.getVendor() != null) {
            return true;
        }
        if (createRESTClassCheckBox.isSelected() != data.isCreateRestSkeleton()) {
            return true;
        }
        if (packageName.getText() != null ? !packageName.getText().equals(data.getProjectPackage()) : data.getProjectPackage() != null) {
            return true;
        }
        return false;
    }



}
