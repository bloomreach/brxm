package org.onehippo.cm.impl.model;

import java.util.List;

import org.onehippo.cm.api.model.ConfigurationItem;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.DefinitionItem;

public abstract class ConfigurationItemImpl implements ConfigurationItem {

    private String name;
    private String path;
    private ConfigurationNode parent;
    private List<DefinitionItem> definitions;
    private boolean delete;

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public ConfigurationNode getParent() {
        return parent;
    }

    public void setParent(final ConfigurationNode parent) {
        this.parent = parent;
    }

    @Override
    public List<DefinitionItem> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(final List<DefinitionItem> definitions) {
        this.definitions = definitions;
    }

    @Override
    public boolean isDeleted() {
        return delete;
    }

    public void setDelete(final boolean delete) {
        this.delete = delete;
    }
}
