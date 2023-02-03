/*
 * Copyright 2014-2023 Bloomreach
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

package org.hippoecm.hst.pagecomposer.jaxrs.model;

import org.hippoecm.hst.configuration.hosting.Mount;

public class MountRepresentation {

    private String hostName;
    private String mountPath;

    public MountRepresentation represent(final Mount mount) {
        hostName = mount.getVirtualHost().getHostName();
        mountPath = mount.getMountPath();
        return this;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(final String mountPath) {
        this.mountPath = mountPath;
    }
}
