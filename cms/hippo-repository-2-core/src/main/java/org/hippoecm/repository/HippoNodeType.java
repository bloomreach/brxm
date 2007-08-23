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
package org.hippoecm.repository;

/*
 * This file has to be kept in sync with:
 * hippo-repository-2-core/src/main/resources/org/hippoecm/repository/repository.cnd
 */

public final class HippoNodeType
{
    final public static String NT_FACETRESULT           = "hippo:facetresult";

    final public static String NT_FACETSEARCH           = "hippo:facetsearch";
    final public static String FACETSEARCH_QUERYNAME    = "hippo:queryname";
    final public static String FACETSEARCH_DOCBASE      = "hippo:docbase";
    final public static String FACETSEARCH_FACETS       = "hippo:facets";
    final public static String FACETSEARCH_SEARCH       = "hippo:search";
    final public static String FACETSEARCH_COUNT        = "hippo:count";
    final public static String FACETSEARCH_RESULTSET    = "hippo:resultset";

    final public static String NT_DOCUMENT              = "hippo:document";

    final public static String NT_TYPES                 = "hippo:types";
    final public static String TYPES_NODETYPE           = "hippo:nodetype";
    final public static String TYPES_DISPLAY            = "hippo:display";
    final public static String TYPES_CLASSNAME          = "hippo:classname";

    final public static String NT_WORKFLOW              = "hippo:workflow";
    final public static String WORKFLOW_NODETYPE        = "hippo:nodetype";
    final public static String WORKFLOW_SERVICE         = "hippo:service";
    final public static String WORKFLOW_DISPLAY         = "hippo:display";
    final public static String WORKFLOW_RENDERER        = "hippo:renderer";
    final public static String WORKFLOW_TYPES           = "hippo:types";
  
    final public static String NT_WORKFLOWCATEGORY      = "hippo:workflowcategory";

    final public static String NT_WORKFLOWFOLDER        = "hippo:workflowfolder";

    final public static String NT_QUERY                 = "hippo:query";
    final public static String QUERY_QUERY              = "hippo:query";
    final public static String QUERY_LANGUAGE           = "hippo:language";
    final public static String QUERY_CLASSNAME          = "hippo:classname";
    final public static String QUERY_TYPES              = "hippo:types";

    final public static String NT_QUERYFOLDER           = "hippo:queryfolder";

    final public static String NT_INITIALIZEITEM        = "hippo:initializeitem";
    final public static String INITIALIZEITEM_NAMESPACE = "hippo:namespace";
    final public static String INITIALIZEITEM_NODETYPES = "hippo:nodetypes";
    final public static String INITIALIZEITEM_CONTENT   = "hippo:content";

    final public static String NT_INITIALIZEFOLDER      = "hippo:initializefolder";

    final public static String NT_CONFIGURATION         = "hippo:configuration";
    final public static String CONFIGURATION_WORKFLOWS  = "hippo:workflows";
    final public static String CONFIGURATION_DOCUMENTS  = "hippo:documents";
    final public static String CONFIGURATION_INITIALIZE = "hippo:initialize";
}
