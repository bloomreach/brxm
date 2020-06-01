/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import javax.jcr.observation.Event;

/**
 * A JCR {@link Event} with the revision id of the corresponding ClusterRecord
 * exposed by the {@link #getRevision()} method.
 */
public interface RevisionEvent extends Event {

    /**
     * The revision id of the cluster record corresponding to this Event.
     *
     * @return revision
     */
    public long getRevision();

}
