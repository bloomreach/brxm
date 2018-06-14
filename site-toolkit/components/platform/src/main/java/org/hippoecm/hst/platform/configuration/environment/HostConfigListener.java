/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.environment;

import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.GenericEventListener;

import static org.hippoecm.hst.platform.configuration.environment.EnvironmentUtils.getActiveHostGroupPath;

public class HostConfigListener extends GenericEventListener {

    private HostConfigPublisher hostConfigPublisher;

    @Override
    public void onEvent(EventIterator events) {
        // since publish hosts might take some milliseconds, we do this async to this event listener early finishes
        hostConfigPublisher.asyncPublishHosts(getActiveHostGroupPath());
    }

    public void setHostConfigPublisher(HostConfigPublisher hostConfigPublisher) {
        this.hostConfigPublisher = hostConfigPublisher;
    }
}
