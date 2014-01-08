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

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class Constants {

    private Constants() {
    }

    static final String SV_URI = "http://www.jcp.org/jcr/sv/1.0";
    static final String DELTA_URI = "http://www.onehippo.org/jcr/xmlimport";

    static final String SV_PREFIX = "sv";
    static final String DELTA_PREFIX = "h";

    static final Namespace SV_NAMESPACE = new Namespace(SV_PREFIX, SV_URI);
    static final Namespace H_NAMESPACE = new Namespace(DELTA_PREFIX, DELTA_URI);
    static final Namespace AE_NAMESPACE = new Namespace("ae", "http://www.onehippo.org/jcr/autoexport/1.0");

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

    static final QName NAME_QNAME = new QName(NAME, SV_NAMESPACE);
    static final QName TYPE_QNAME = new QName(TYPE, SV_NAMESPACE);
    static final QName NODE_QNAME = new QName(NODE, SV_NAMESPACE);
    static final QName PROPERTY_QNAME = new QName(PROPERTY, SV_NAMESPACE);
    static final QName VALUE_QNAME = new QName(VALUE, SV_NAMESPACE);
    static final QName MERGE_QNAME = new QName(MERGE, H_NAMESPACE);
    static final QName FILE_QNAME = new QName(FILE, H_NAMESPACE);
    static final QName AE_ID_QNAME = new QName("id", AE_NAMESPACE);
    
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
