package org.onehippo.cms7.essentials.dashboard.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @version "$Id: DependencyRestful.java 174870 2013-08-23 13:56:24Z mmilicevic $"
 */
@XmlRootElement(name = "dependency")
public class DependencyRestful implements EssentialsDependency, Restful {

    private static final long serialVersionUID = 1L;
    private String groupId;
    private String artifactId;
    private String repositoryId;
    private String repositoryUrl;
    private String version;
    private String scope;
    private String type;


    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public void setRepositoryId(final String repositoryId) {
        this.repositoryId = repositoryId;
    }

    @Override
    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    @Override
    public void setRepositoryUrl(final String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(final String version) {
        this.version = version;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public void setScope(final String scope) {
        this.scope = scope;
    }

    @Override
    public DependencyType getDependencyType() {
        return DependencyType.typeForName(type);

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DependencyRestful{");
        sb.append("groupId='").append(groupId).append('\'');
        sb.append(", artifactId='").append(artifactId).append('\'');
        sb.append(", repositoryId='").append(repositoryId).append('\'');
        sb.append(", repositoryUrl='").append(repositoryUrl).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
