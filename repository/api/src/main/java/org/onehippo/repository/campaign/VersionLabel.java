/*
 *  Copyright 2021 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onehippo.repository.campaign;

import java.util.Objects;

public class VersionLabel {

    private String uuid;
    private String versionLabel;

    // kept for deserialization
    public VersionLabel() {
    }

    public VersionLabel(final String uuid) {
        this.uuid = uuid;
    }

    public VersionLabel(final String uuid, final String versionLabel) {
        this.uuid = uuid;
        this.versionLabel = versionLabel;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(final String versionLabel) {
        this.versionLabel = versionLabel;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final VersionLabel that = (VersionLabel) o;
        return Objects.equals(uuid, that.uuid) &&
                Objects.equals(versionLabel, that.versionLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, versionLabel);
    }
}
