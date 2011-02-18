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
package org.hippoecm.repository;

/*
 * This file has to be kept in sync with:
 * src/main/resources/hippostd.cnd
 */

/**
 * This interface defines the node types and item names that are in use by
 * standard extensions of the Hippo repository.
 */

public interface HippoStdNodeType {
    final static String SVN_ID = "$Id$";

    //--- Hippo standard NodeTypes ---//
    String NT_DIRECTORY = "hippostd:directory";
    String NT_FOLDER = "hippostd:folder";
    String NT_HTML = "hippostd:html";
    String NT_DATE = "hippostd:date";
    String NT_LANGUAGEABLE = "hippostd:languageable";
    String NT_PUBLISHABLE = "hippostd:publishable";
    String NT_PUBLISHABLESUMMARY = "hippostd:publishableSummary";
    String NT_TRANSLATIONS = "hippostd:translations";
    String NT_CONTAINER = "hippostd:container";
    String NT_RELAXED = "hippostd:relaxed";

    //--- Hippo Item Names ---//
    String HIPPOSTD_CONTENT = "hippostd:content";
    String HIPPOSTD_HOLDER = "hippostd:holder";
    String HIPPOSTD_LANGUAGE = "hippostd:language";
    String HIPPOSTD_SECOND = "hippostd:second";
    String HIPPOSTD_STATE = "hippostd:state";
    String HIPPOSTD_STATESUMMARY = "hippostd:stateSummary";
    String HIPPOSTD_TRANSLATIONS = "hippostd:translations";

    String HIPPOSTD_DATE = "hippostd:date";
    String HIPPOSTD_MONTH = "hippostd:month";
    String HIPPOSTD_YEAR = "hippostd:year";
    String HIPPOSTD_DAYOFYEAR = "hippostd:dayofyear";
    String HIPPOSTD_WEEKOFYEAR = "hippostd:weekofyear";
    String HIPPOSTD_DAYOFWEEK = "hippostd:dayofweek";

    //--- Hippo Item Values ---//
    String PUBLISHED = "published";
    String UNPUBLISHED = "unpublished";
    String DRAFT = "draft";
    String NEW = "new";

}
