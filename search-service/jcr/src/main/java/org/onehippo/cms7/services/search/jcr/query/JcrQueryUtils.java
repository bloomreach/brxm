/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.jcr.query;

import org.hippoecm.repository.util.DateTools;
import org.onehippo.cms7.services.search.query.constraint.DateConstraint;

public final class JcrQueryUtils {

    static String toXPathProperty(String path, boolean childAxisAllowed, String methodName) throws JcrQueryException {
        return toXPathProperty(path, childAxisAllowed, methodName, null);
    }

    static String toXPathProperty(final String path, final boolean childAxisAllowed, String methodName, String[] skips)  throws JcrQueryException {
        if(path == null) {
            throw new JcrQueryException("Scope is not allowed to be null for '"+methodName+"'");
        }
        if(skips != null) {
            for(String skip : skips) {
                if(skip.equals(path)) {
                    return path;
                }
            }
        }
        if(childAxisAllowed) {
            if(path.indexOf("/") > -1) {
                String[] parts = path.split("/");
                StringBuilder newPath = new StringBuilder();
                int i = 0;
                for(String part : parts) {
                    i++;
                    if(i == parts.length) {
                        if(i > 1) {
                            newPath.append("/");
                        }
                        if(!part.startsWith("@")) {
                            newPath.append("@");
                        }
                        newPath.append(part);
                    } else {
                        if(part.startsWith("@")) {
                            throw new JcrQueryException("'@' in path only allowed for a property: invalid path: '"+path+"'");
                        }
                        if(i > 1) {
                            newPath.append("/");
                        }
                        newPath.append(part);
                    }
                }
                return newPath.toString();
            } else {
                if(path.startsWith("@")) {
                    return path;
                } else {
                    return "@"+path;
                }
            }
        } else {
            if(path.indexOf("/") > -1) {
                throw new JcrQueryException("Not allowed to use a child path for '"+methodName+"'. Invalid: '"+path+"'");
            }
            if(path.startsWith("@")) {
                return path;
            } else {
                return "@"+path;
            }
        }
    }

    static DateTools.Resolution getDateToolsResolution(final DateConstraint.Resolution resolution) {
        if (resolution == null) {
            Filter.log.warn("Resolution is null, return default exact expensive resolution on milliseconds");
            return DateTools.Resolution.DAY;
        }
        switch (resolution) {
            case YEAR: return DateTools.Resolution.YEAR;
            case MONTH: return DateTools.Resolution.MONTH;
            case DAY: return DateTools.Resolution.DAY;
            case HOUR: return DateTools.Resolution.HOUR;
            case EXACT: return DateTools.Resolution.MILLISECOND;
        }
        throw new IllegalStateException("Resolution must be of supported type");
    }
}
