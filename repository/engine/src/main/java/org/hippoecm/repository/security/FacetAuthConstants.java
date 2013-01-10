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
package org.hippoecm.repository.security;


/**
 * Help class for resolving facet auth principals from jcr nodes
 */
public class FacetAuthConstants {

    /** SVN id placeholder */

    /** The wildcard to match everything */
    public static final String WILDCARD = "*";

    /** Expander for user */
    public static final String EXPANDER_USER = "__user__";

    /** Expander for group */
    public static final String EXPANDER_GROUP = "__group__";

    /** Expander for role */
    public static final String EXPANDER_ROLE = "__role__";

}
