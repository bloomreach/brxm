/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.model.rest;

public class MavenRepository {

    private String id;
    private String name;
    private String url;
    private Policy releasePolicy;
    private Policy snapshotPolicy;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public Policy getReleasePolicy() {
        return releasePolicy;
    }

    public void setReleasePolicy(final Policy releasePolicy) {
        this.releasePolicy = releasePolicy;
    }

    public Policy getSnapshotPolicy() {
        return snapshotPolicy;
    }

    public void setSnapshotPolicy(final Policy snapshotPolicy) {
        this.snapshotPolicy = snapshotPolicy;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MavenRepository{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class Policy {

        private String enabled;
        private String updatePolicy;
        private String checksumPolicy;

        public String getEnabled() {
            return enabled;
        }

        public void setEnabled(final String enabled) {
            this.enabled = enabled;
        }

        public String getUpdatePolicy() {
            return updatePolicy;
        }

        public void setUpdatePolicy(final String updatePolicy) {
            this.updatePolicy = updatePolicy;
        }

        public String getChecksumPolicy() {
            return checksumPolicy;
        }

        public void setChecksumPolicy(final String checksumPolicy) {
            this.checksumPolicy = checksumPolicy;
        }
    }

    public static class WithModule extends MavenRepository {
        private String targetPom;

        public String getTargetPom() {
            return targetPom;
        }

        public void setTargetPom(final String targetPom) {
            this.targetPom = targetPom;
        }
    }
}
