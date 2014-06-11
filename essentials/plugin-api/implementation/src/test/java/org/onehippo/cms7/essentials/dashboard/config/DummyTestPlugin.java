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

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.List;

import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.Plugin;
import org.onehippo.cms7.essentials.dashboard.model.Vendor;
import org.onehippo.cms7.essentials.dashboard.model.VendorRestful;

/**
 * @version "$Id: DummyTestPlugin.java 174785 2013-08-23 08:28:52Z mmilicevic $"
 */
public class DummyTestPlugin implements Plugin {

    private static final long serialVersionUID = 1L;

    private String parentPath;


    @Override
    public List<String> getRestClasses() {
        return null;
    }

    @Override
    public void setRestClasses(final List<String> restClasses) {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(final boolean enabled) {

    }

    @Override
    public String getDescription() {
        return "test";
    }


    @Override
    public void setDescription(final String description) {

    }

    @Override
    public boolean isNeedsInstallation() {
        return false;
    }

    @Override
    public void setNeedsInstallation(final boolean needsInstallation) {

    }

    @Override
    public boolean isAddRequested() { return false; }

    @Override
    public void setAddRequestedFlag(boolean addRequested) { }

    @Override
    public boolean isAdded() { return false; }

    @Override
    public void setAddedFlag(boolean added) { }

    @Override
    public boolean isInstalled() {
        return false;
    }

    @Override
    public void setInstalled(final boolean installed) {

    }

    @Override
    public String getPluginId() {
        return "pluginId";
    }

    @Override
    public void setPluginId(final String pluginId) {

    }

    @Override
    public String getPackageFile() {
        return null;
    }

    @Override
    public void setPackageFile(final String packageFile) {

    }

    @Override
    public Vendor getVendor() {
        return new VendorRestful();
    }

    @Override
    public String getPackageClass() {
        return null;
    }

    @Override
    public void setPackageClass(final String packageClass) {

    }

    @Override
    public void setVendor(final Vendor vendor) {

    }

    @Override
    public String getDocumentationLink() {
        return null;
    }

    @Override
    public void setDocumentationLink(final String documentationLink) {

    }

    @Override
    public String getIssuesLink() {
        return null;
    }

    @Override
    public void setIssuesLink(final String issuesLink) {

    }


    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void setName(final String name) {

    }

    @Override
    public String getType() {
        return "testtype";
    }

    @Override
    public void setType(final String type) {

    }

    @Override
    public List<EssentialsDependency> getDependencies() {
        return null;
    }

    @Override
    public void setDependencies(final List<EssentialsDependency> dependencies) {

    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public void setTitle(final String title) {

    }

    @Override
    public String getIntroduction() {
        return null;
    }

    @Override
    public void setIntroduction(final String introduction) {

    }


}