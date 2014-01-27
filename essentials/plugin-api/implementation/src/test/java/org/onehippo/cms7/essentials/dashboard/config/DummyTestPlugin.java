package org.onehippo.cms7.essentials.dashboard.config;

import java.util.Collections;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.Plugin;

/**
 * @version "$Id: DummyTestPlugin.java 174785 2013-08-23 08:28:52Z mmilicevic $"
 */
public class DummyTestPlugin implements Plugin {

    private static final long serialVersionUID = 1L;

    private String parentPath;

    @Override
    public void addScreenShot(final Screenshot screenShot) {

    }

    @Override
    public void addAsset(final Asset asset) {

    }

    @Override
    public String getDescription() {
        return "test";
    }

    @Override
    public void setScreenshots(final List<Screenshot> screenshots) {

    }

    @Override
    public void setDescription(final String description) {

    }

    @Override
    public Asset getAsset(final String id) {
        return null;
    }

    @Override
    public List<Asset> getAssets() {
        return null;
    }

    @Override
    public void setAssets(final List<Asset> assets) {

    }

    @Override
    public String getVendorLink() {
        return "test vendor link";
    }

    @Override
    public void setVendorLink(final String vendorLink) {

    }

    @Override
    public String getPluginLink() {
        return "pluginLink";
    }

    @Override
    public void setPluginLink(final String pluginLink) {

    }

    @Override
    public String getVendor() {
        return "vendor";
    }

    @Override
    public void setVendor(final String vendor) {

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
    public List<Screenshot> getScreenshots() {
        return null;
    }

    @Override
    public List<String> getProperties() {
        return Collections.emptyList();
    }

    @Override
    public void setProperties(final List<String> properties) {

    }

    @Override
    public void addProperty(final String value) {

    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void setName(final String name) {

    }

    @Override
    public String getParentPath() {
        return parentPath;
    }

    @Override
    public String getPath() {
        return parentPath + '/' + getClass().getSimpleName();
    }

    @Override
    public void setParentPath(final String path) {

    }

    @Override
    public String getType() {
        return "testtype";
    }

    @Override
    public void setType(final String type) {

    }

    @Override
    public String getIcon() {
        return null;
    }

    @Override
    public void setIcon(final String icon) {

    }

    @Override
    public String getPluginClass() {
        return getClass().getName();
    }

    @Override
    public void setPluginClass(final String pluginClass) {

    }
}