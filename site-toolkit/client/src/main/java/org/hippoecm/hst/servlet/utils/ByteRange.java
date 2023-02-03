/*
 *  Copyright 2017-2023 Bloomreach
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
package org.hippoecm.hst.servlet.utils;

/**
 * Byte Range Header Information class.
 * <P>
 * Inspired by <code>org.apache.catalina.servlets.DefaultServlet.java#parseRange(HttpServletRequest, HttpServletResponse, WebResource)</code>
 * of Apache Tomcat project.
 * </P>
 */
public class ByteRange {

    public long start;
    public long end;
    public long length;

    public boolean validate() {
        if (end >= length)
            end = length - 1;
        return (start >= 0) && (end >= 0) && (start <= end) && (length > 0);
    }

}
