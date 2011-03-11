/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.configs;

import java.util.Calendar;
import java.util.Locale;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;

public class ConfigBackup implements Comparable<ConfigBackup>, IClusterable {

    private final String name;
    private final String createdBy;
    private final Calendar created;
    
    public ConfigBackup(String name, String createdBy, Calendar created) {
        this.name = name;
        this.createdBy = createdBy;
        this.created = created;
    }
    
    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Calendar getCreated() {
        return created;
    }

    public String getCreatedAsString() {
        return simpleFormattedCalendar(created);
    }

    private String simpleFormattedCalendar(Calendar cal) {
        if (cal != null) {
            Locale locale = Session.get().getLocale();
            return DateTimeFormat.forPattern("d-MMM-yyyy").withLocale(locale).print(cal.getTimeInMillis());
        }
        return "";
    }

    @Override
    public int compareTo(final ConfigBackup configBackup) {
        if (configBackup == null || configBackup.created == null) {
            return -1;
        }
        return configBackup.created.compareTo(created);
    }

}
