/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "repository")
public class RepositoryRestful implements Repository, Restful {

    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String layout;
    private String url;
    private Snapshot snapshots;
/*
    // To extend the repository snapshots with updatePolicy and checksumPolicy will break backwards compatibility
    private RepositoryPolicy snapshots;
*/
    private String targetPom;
    private RepositoryPolicy releases;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getLayout() {
        return layout;
    }

    @Override
    public void setLayout(final String layout) {
        this.layout = layout;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(final String url) {
        this.url = url;
    }
/*
    // To extend the repository snapshots with updatePolicy and checksumPolicy will break backwards compatibility

    @XmlElementRef(type = RepositoryPolicyRestful.class, name = "snapshots")
    @JsonSubTypes({@JsonSubTypes.Type(value = RepositoryPolicyRestful.class, name = "snapshots")})
    @JsonTypeInfo(defaultImpl = RepositoryPolicyRestful.class, use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @Override
    public RepositoryPolicy getSnapshots() {
        return snapshots;
    }

    @Override
    public void setSnapshots(final RepositoryPolicy snapshots) {
        this.snapshots = snapshots;
    }
*/

    @XmlElementRef(type = SnapshotRestful.class, name = "snapshots")
    @JsonSubTypes({@JsonSubTypes.Type(value = SnapshotRestful.class, name = "snapshots")})
    @JsonTypeInfo(defaultImpl = SnapshotRestful.class, use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @Override
    public Snapshot getSnapshots() {
        return snapshots;
    }

    @Override
    public void setSnapshots(final Snapshot snapshots) {
        this.snapshots = snapshots;
    }

    @XmlElementRef(type = RepositoryPolicyRestful.class, name = "releases")
    @JsonSubTypes({@JsonSubTypes.Type(value = RepositoryPolicyRestful.class, name = "releases")})
    @JsonTypeInfo(defaultImpl = RepositoryPolicyRestful.class, use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @Override
    public RepositoryPolicy getReleases() {
        return releases;
    }

    @Override
    public void setReleases(final RepositoryPolicy releases) {
        this.releases = releases;
    }

    @Override
    public String getTargetPom() {
        return targetPom;
    }

    @Override
    public void setTargetPom(final String targetPom) {
        this.targetPom = targetPom;
    }

    @XmlTransient
    @JsonIgnore
    @Override
    public TargetPom getDependencyTargetPom() {
        return TargetPom.pomForName(targetPom);
    }

    @Override
    public org.apache.maven.model.Repository createMavenRepository() {
        org.apache.maven.model.Repository repository = new org.apache.maven.model.Repository();
        repository.setName(getName());
        repository.setId(getId());
        repository.setUrl(getUrl());
        final org.apache.maven.model.RepositoryPolicy policy = new org.apache.maven.model.RepositoryPolicy();
        if (snapshots == null || snapshots.getEnabled() == null) {
            policy.setEnabled(false);
        } else {
            policy.setEnabled(snapshots.getEnabled());
        }
        if(getReleases() != null) {
            repository.setReleases(getReleases().createMavenRepositoryPolicy());
        }
        return repository;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RepositoryRestful{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", layout='").append(layout).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", snapshots=").append(snapshots);
        sb.append(", releases=").append(releases);
        sb.append(", targetPom='").append(targetPom).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
