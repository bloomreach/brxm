/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

public final class Constants {

    private Constants() {
    }

    static final String SV_URI = "http://www.jcp.org/jcr/sv/1.0";
    static final String DELTA_URI = "http://www.onehippo.org/jcr/xmlimport";
    static final String AE_URI = "http://www.onehippo.org/jcr/autoexport/1.0";

    static final String SV_PREFIX = "sv";
    static final String DELTA_PREFIX = "h";

    static final String ID = "id";
    static final String QID = "ae:id";
    static final String NAME = "name";
    static final String QNAME = "sv:name";
    static final String NODE = "node";
    static final String QNODE = "sv:node";
    static final String PROPERTY = "property";
    static final String QPROPERTY = "sv:property";
    static final String VALUE = "value";
    static final String QVALUE = "sv:value";
    static final String MERGE = "merge";
    static final String QMERGE = "h:merge";
    static final String CDATA = "CDATA";
    static final String TYPE = "type";
    static final String QTYPE = "sv:type";
    static final String FILE = "file";
    static final String QFILE = "h:file";


    public static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/autoexport/hippo:moduleconfig";
    public static final String CONFIG_ENABLED_PROPERTY_NAME = "autoexport:enabled";
    static final String NODETYPES_PATH = "/jcr:system/jcr:nodeTypes";
    static final String CONFIG_MODULES_PROPERTY_NAME = "autoexport:modules";
    static final String CONFIG_EXCLUDED_PROPERTY_NAME = "autoexport:excluded";
    static final String CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME = "autoexport:filteruuidpaths";

    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";
    static final String OLD_EXPORT_DIR_PROPERTY = "hippoecm.export.dir";
    
    public static final String LOGGER_NAME = "org.onehippo.cms7.autoexport";

}
