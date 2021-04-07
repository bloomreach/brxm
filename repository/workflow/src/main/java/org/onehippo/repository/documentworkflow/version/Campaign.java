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
package org.onehippo.repository.documentworkflow.version;

import java.util.Calendar;
import java.util.Objects;
import java.util.UUID;

public class Campaign {

    private String uuid;
    private Calendar from;
    private Calendar to;

    // kept for deserialization
    public Campaign() {
    }

    public Campaign(final String uuid) {
        this.uuid = uuid;
    }

    public Campaign(final String uuid, final Calendar from, final Calendar to) {
        this.uuid = uuid;
        this.from = from;
        this.to = to;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public Calendar getFrom() {
        return from;
    }

    public void setFrom(final Calendar from) {
        this.from = from;
    }

    public Calendar getTo() {
        return to;
    }

    public void setTo(final Calendar to) {
        this.to = to;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Campaign campaign = (Campaign) o;
        return Objects.equals(uuid, campaign.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
