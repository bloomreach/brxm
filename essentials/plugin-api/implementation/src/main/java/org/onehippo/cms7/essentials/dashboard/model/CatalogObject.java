package org.onehippo.cms7.essentials.dashboard.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for HST catalog items
 * TODO: move to API?
 * @version "$Id: CatalogObject.java 174059 2013-08-16 13:51:28Z mmilicevic $"
 */
public class CatalogObject implements Serializable {

    public static final String PRIMARY_TYPE = "hst:containeritemcomponent";
    private static final long serialVersionUID = 1L;

    private String siteName;
    private String name;
    private String componentClassName;
    private String iconPath;
    private String label;
    private String template;
    private String xType;
    private boolean detail;
    private Map<String, String> parameters = new HashMap<>();


    public CatalogObject() {
    }

    public CatalogObject(final String name, final String label) {
        this.name = name;
        this.label = label;
    }

    public CatalogObject addParameter(final String name, final String value) {
        parameters.put(name, value);
        return this;

    }

    public boolean isDetail() {
        return detail;
    }

    public CatalogObject setDetail(final boolean detail) {
        this.detail = detail;
        return this;
    }

    public String getSiteName() {
        return siteName;
    }

    public CatalogObject setSiteName(final String siteName) {
        this.siteName = siteName;
        return this;
    }

    public String getName() {
        return name;
    }

    public CatalogObject setName(final String name) {
        this.name = name;
        return this;
    }

    public String getComponentClassName() {
        return componentClassName;
    }

    public CatalogObject setComponentClassName(final String componentClassName) {
        this.componentClassName = componentClassName;
        return this;
    }

    public String getIconPath() {
        return iconPath;
    }

    public CatalogObject setIconPath(final String iconPath) {
        this.iconPath = iconPath;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public CatalogObject setLabel(final String label) {
        this.label = label;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public CatalogObject setTemplate(final String template) {
        this.template = template;
        return this;
    }

    public String getType() {
        return xType;

    }

    public CatalogObject setType(final String xType) {
        this.xType = xType;
        return this;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public CatalogObject setParameters(final Map<String, String> parameters) {
        this.parameters = parameters;
        return this;
    }
}
