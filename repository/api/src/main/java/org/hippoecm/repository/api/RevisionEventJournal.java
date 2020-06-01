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

import javax.jcr.observation.EventJournal;

/**
 * An extension of {@link EventJournal} which also allows skipping
 * based on Event revision.
 */
public interface RevisionEventJournal extends EventJournal {

    /**
     * Skip all Events with revision less than or equal to <code>revision</code>.
     *
     * @param revision the revision after which this iterator should be positioned
     */
    public void skipToRevision(long revision);

    /**
     * {@inheritDoc}
     */
    @Override
    public RevisionEvent nextEvent();

}
