/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.migration.webdav;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.webdav.lib.Property;


/**
 * Helper method for reading, parsing and converting WebDAV nodes and properties
 */
public class WebdavHelper {

    /**
     * Convert a webdav date property to a calendar date
     * @param webdavProperty
     * @param dateFormat
     * @return calender
     */
    public static Calendar getCalendarFromProperty(Property webdavProperty, SimpleDateFormat dateFormat) {
        Date d;
        try {
            d = dateFormat.parse(webdavProperty.getPropertyAsString());
        } catch (java.text.ParseException e) {
            // use now if the date can't be parsed
            d = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }
    

    /**
     * Find the nodeName from a specific uri
     * @param uri
     * @return the nodeName
     */
    public static String nodeName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }
    
    /**
     * Get the parent path of a uri
     * @param uri
     * @return the parent path
     */
    public static String parentPath(String uri) {
        int i = uri.lastIndexOf("/");
        uri = i == -1 ? uri : uri.substring(0, i);

        return uri;
    }
}
