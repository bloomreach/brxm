package org.onehippo.cms7.essentials.dashboard.config;

import java.util.List;

import org.onehippo.cms7.essentials.dashboard.model.Dependency;
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
    public Vendor getVendor() {
        return new VendorRestful();
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
    public List<Dependency> getDependencies() {
        return null;
    }

    @Override
    public void setDependencies(final List<Dependency> dependencies) {

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

    @Override
    public String getPluginClass() {
        return getClass().getName();
    }

    @Override
    public void setPluginClass(final String pluginClass) {

    }
}