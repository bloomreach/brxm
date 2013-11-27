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
package org.onehippo.repository.update;

import javax.jcr.Node;

import org.onehippo.cms7.services.SingletonService;

/**
 * The node updater service runs registered &amp; not-net-completed updater visitors.
 * It is intended to be used by code that assumes the content to have been updated.
 * <p>
 * Since updater visitors run asynchronously, potentially taking a long time when
 * there is a lot of content, a node may not yet have been updated by a background
 * process.
 */
@SingletonService
public interface NodeUpdaterService {

    enum NodeUpdaterResult {
        NO_UPDATE_NEEDED,
        UPDATE_SUCCEEDED,
        UPDATE_FAILED
    }

    NodeUpdaterResult updateNode(final Node node);

}
