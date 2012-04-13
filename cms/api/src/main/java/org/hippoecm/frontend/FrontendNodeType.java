/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend;

/*
 * This file has to be kept in sync with:
 * src/main/resources/org/hippoecm/repository/frontend.cnd
 */

/**
 * This interface defines the node types and item names that are in use by
 * the Hippo CMS frontend build on top of the Hippo Repository.
 */

public interface FrontendNodeType {
    final static String SVN_ID = "$Id";

    //--- Frontend NodeTypes ---//
    String NT_PLUGINCONFIG = "frontend:pluginconfig";
    String NT_PLUGIN = "frontend:plugin";
    String NT_PLUGINCLUSTER = "frontend:plugincluster";
    String NT_CLUSTERFOLDER = "frontend:clusterfolder";
    String NT_APPLICATION = "frontend:application";
    String NT_WORKFLOW = "frontend:workflow";
    String NT_USER = "frontend:user";

    //--- Frontend Item Names ---//
    String FRONTEND_SERVICES = "frontend:services";
    String FRONTEND_REFERENCES = "frontend:references";
    String FRONTEND_PROPERTIES = "frontend:properties";
    String FRONTEND_SAVEONEXIT = "frontend:saveonexit";
    String FRONTEND_RENDERER = "frontend:renderer";
    String FRONTEND_FIRSTNAME = "frontend:firstname";
    String FRONTEND_LASTNAME = "frontend:lastname";
    String FRONTEND_EMAIL = "frontend:email";
    String FRONTEND_PATH = "frontend:path";
    String FRONTEND_EVENTS = "frontend:events";
    String FRONTEND_DEEP = "frontend:deep";
    String FRONTEND_UUIDS = "frontend:uuids";
    String FRONTEND_NODETYPES = "frontend:nodetypes";

}
