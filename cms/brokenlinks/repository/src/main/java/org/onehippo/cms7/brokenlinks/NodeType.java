/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.brokenlinks;

public interface NodeType {

    String BROKENLINKS_MIXIN = "brokenlinks:brokenlinks";
    
    String BROKENLINKS_NODE = "brokenlinks:link";
    String PROPERTY_URL = "brokenlinks:url";
    String PROPERTY_ERROR_CODE = "brokenlinks:errorCode";
    String PROPERTY_ERROR_MESSAGE = "brokenlinks:errorMessage";
    String PROPERTY_BROKEN_SINCE = "brokenlinks:brokenSince";
    String PROPERTY_LAST_TIME_CHECKED = "brokenlinks:lastTimeChecked";
    String PROPERTY_EXCERPT = "brokenlinks:excerpt";

}
