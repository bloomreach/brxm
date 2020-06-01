/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reporting;

public interface ReportingNodeTypes {

    // NodeTypes
    public static final String NT_REPORT = "reporting:report";

    // Paths
    public static final String QUERY = "reporting:query";
    public static final String QUERY_STATEMENT = "hippo:query/jcr:statement";
    public static final String QUERY_LANGUAGE = "hippo:query/jcr:language";
    public static final String PARAMETER_NAMES = "reporting:parameternames";
    public static final String PARAMETER_VALUES = "reporting:parametervalues";
    public static final String LIMIT = "reporting:limit";
    public static final String OFFSET = "reporting:offset";
    public static final String LISTENER = "reporting:listener";
    public static final String PLUGIN = "reporting:plugin";
}
