/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.cms7.utilities.security.jcr;

/**
 * A constants class from which all queries related to users and groups information retrieval can be accessed easily
 */
public final class Queries {

    public static final class SQL {

        public static final String SELECT_ALL_USERS = "SELECT * FROM hipposys:user";

        public static final String SELECT_ALL_GROUPS = "SELECT * FROM hipposys:group";

        public static final String SELECT_ALL_USER_GROUPS = "SELECT * FROM hipposys:group WHERE jcr:primaryType='hipposys:group' AND hipposys:members='{}'";

        public static final String SELECT_GROUP_BY_NAME = "SELECT * FROM hipposys:group WHERE jcr:primaryType='hipposys:group' AND fn:name()='{}'";

    }

}
