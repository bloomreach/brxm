package org.onehippo.cms7.essentials.plugins.contentblocks.model;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

public class ProviderRestful implements Restful {
    private String name;
    private String translatedName;
    private String repositoryPath;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getTranslatedName() {
        return translatedName;
    }

    public void setTranslatedName(final String translatedName) {
        this.translatedName = translatedName;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(final String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }
}
