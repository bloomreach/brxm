/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
