package org.onehippo.cms7.essentials.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.Restful;

/**
 * @version "$Id: DependencyRestful.java 174870 2013-08-23 13:56:24Z mmilicevic $"
 */
@XmlRootElement(name = "dependency")
public class DependencyRestful implements Restful {

    private static final long serialVersionUID = 1L;
    private String groupId;
    private String artifactId;
    private String repositoryId;
    private String repositoryUrl;
    private String version;
    private String scope;
    private String type;


    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(final String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(final String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setScope(final String scope) {
        this.scope = scope;
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
