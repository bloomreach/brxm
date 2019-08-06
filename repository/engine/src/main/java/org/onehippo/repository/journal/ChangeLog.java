/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.journal;

import java.util.Collection;

import javax.jcr.observation.Event;

public interface ChangeLog {

    interface Record {

        /**
         * @return the identifier of the backing {@link javax.jcr.Node} for which this {@link Record} is.
         * The {@link javax.jcr.Node} can have been removed
         */
        String getIdentifier();

        /**
         * @return the path of the backing {@link javax.jcr.Node} for which this {@link Record} is.
         * The {@link javax.jcr.Node} can have been removed
         */
        String getPath();

        /**
         * @return One of the types from {@link Event#getType()} <strong>OR</strong> the extra event type
         * {@link ExternalRepositorySyncRevisionService#NODE_MODIFIED}
         */
        int getType();

        /**
         * @return the revision for this record
         */
        long getRevision();

        /**
         * @return the date for this record
         */
        long getDate();
    }

    /**
     * @return All the {@link Record}s for this {@link ChangeLog}
     */
    Collection<? extends Record> getRecords();

    /**
     * @return the revision of the first record in this {@link ChangeLog}
     */
    long getStartRevision();

    /**
     * @return the revision of the last record in this {@link ChangeLog}
     */
    long getEndRevision();

}
