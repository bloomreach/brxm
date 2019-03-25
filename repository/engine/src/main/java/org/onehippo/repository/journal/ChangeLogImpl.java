/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 */
package org.onehippo.repository.journal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.core.observation.EventState;
import org.hippoecm.repository.api.RevisionEvent;

import static org.onehippo.repository.journal.ExternalRepositorySyncRevisionService.NODE_MODIFIED;

public class ChangeLogImpl implements ChangeLog {

    private static class RecordImpl implements Record {

        // TODO should we use UUID for less memory consumption instead of identifier?
        private final String identifier;
        private String path;
        private int type;
        private long revision;
        private long date;

        private RecordImpl(final String identifier, final int type) {
            if (identifier == null) {
                throw new IllegalArgumentException("Identifier is not allowed to be null");
            }
            this.identifier = identifier;
            this.type = type;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        public String getPath() {
            return path;
        }

        public int getType() {
            return type;
        }

        public long getRevision() {
            return revision;
        }

        public long getDate() {
            return date;
        }

        public void setPath(final String path) {
            this.path = path;
        }

        public void setRevision(final long revision) {
            this.revision = revision;
        }

        public void setDate(final long date) {
            this.date = date;
        }

        public String toString() {
            return "Record: " + identifier;
        }

        @Override
        public int hashCode() {
            return identifier.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof RecordImpl)) {
                return false;
            }
            RecordImpl other = (RecordImpl) obj;
            return identifier.equals(other.identifier);
        }
    }

    private static class Key {
        private String identifier;
        private int type;

        private Key(final String identifier, final int type) {
            this.identifier = identifier;
            this.type = type;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Key key = (Key) o;

            if (type != key.type) return false;
            return identifier.equals(key.identifier);
        }

        @Override
        public int hashCode() {
            int result = identifier.hashCode();
            result = 31 * result + type;
            return result;
        }
    }

    private final Map<Key,RecordImpl> records = new HashMap<>();
    private long endRevision;
    private long startRevision;

    @Override
    public Collection<? extends Record> getRecords() {
        return records.values();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    /**
     *
     * @param event
     * @param squashEvents
     * @return {@code true} if this resulted in a new record
     * @throws RepositoryException
     */
    public boolean recordChange(final RevisionEvent event, final boolean squashEvents) throws RepositoryException {
        final String identifier = event.getIdentifier();

        final int type;
        String path;
        if (squashEvents) {
             type = NODE_MODIFIED;
             switch (event.getType()) {
                 case Event.PROPERTY_ADDED:
                 case Event.PROPERTY_CHANGED:
                 case Event.PROPERTY_REMOVED:
                     path = StringUtils.substringBeforeLast(event.getPath(), "/");
                     if (path.length() == 0) {
                         // ROOT NODE prop event
                         path = event.getPath();
                     }
                     break;
                 default:
                     path = event.getPath();
                     break;
             }
        } else {
            type = event.getType();
            path = event.getPath();
        }

        final Key key = new Key(identifier, type);
        RecordImpl record = records.get(key);
        if (record == null) {
            record = new RecordImpl(identifier, type);
            record.setPath(path);
            record.setRevision(event.getRevision());
            record.setDate(event.getDate());
            records.put(key, record);
            return true;
        } else {
            // update revision to later one
            record.setRevision(record.getRevision());
        }
        return false;
    }

    public long getStartRevision() {
        return startRevision;
    }

    public void setStartRevision(final long startRevision) {
        this.startRevision = startRevision;
    }

    public long getEndRevision() {
        return endRevision;
    }

    public void setEndRevision(long revision) {
        this.endRevision = revision;
    }


}
