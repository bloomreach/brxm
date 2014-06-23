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

package org.onehippo.cms7.essentials.dashboard.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.maven.model.RepositoryPolicy;

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
    private String type;

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

    @Override
    public Snapshot getSnapshots() {
        return snapshots;
    }

    @Override
    public void setSnapshots(final Snapshot snapshots) {
        this.snapshots = snapshots;
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
    public DependencyType getDependencyType() {
        return DependencyType.typeForName(type);
    }

    @Override
    public org.apache.maven.model.Repository createMavenRepository() {
        org.apache.maven.model.Repository repository = new org.apache.maven.model.Repository();
        repository.setName(getName());
        repository.setId(getId());
        repository.setUrl(getUrl());
        final RepositoryPolicy policy = new RepositoryPolicy();
        if (snapshots == null || snapshots.getEnabled() == null) {
            policy.setEnabled(false);
        } else {
            policy.setEnabled(snapshots.getEnabled());
        }
        repository.setSnapshots(policy);
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
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
