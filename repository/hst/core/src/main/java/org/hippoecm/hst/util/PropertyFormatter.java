/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTConfiguration;

public class PropertyFormatter {
    
    private static final String KEY_DATE_FORMAT = "tags.format.date";
    private static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";
    
    private DateFormat dateFormat;
    private final HttpServletRequest request;

    /**
     * Constructor with request.
     */
    public PropertyFormatter(HttpServletRequest request) {
        super();
        this.request = request;
    }

    /**
     * Format a property as string. 
     */
    public String format(Object property) {
        
        if (property == null) {
            throw new IllegalArgumentException("Property may not be null");
        }
        
        // date
        if (property instanceof Date) {
            return getDateFormat().format((Date) property);
        }
        
        // string
        return property.toString();
    }

    private DateFormat getDateFormat() {
        
        // lazy
        if (this.dateFormat == null) {
        
            // pattern by general configuration or default
            String formatPattern = HSTConfiguration.get(request.getSession().getServletContext(), 
                        KEY_DATE_FORMAT, false/*not required*/);
            if (formatPattern == null) {
                formatPattern = DEFAULT_DATE_FORMAT;
            }    
            
            this.dateFormat = new SimpleDateFormat(formatPattern);
        }
    
        return this.dateFormat;
     }
}
