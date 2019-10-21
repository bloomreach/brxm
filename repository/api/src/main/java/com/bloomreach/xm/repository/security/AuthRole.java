/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package com.bloomreach.xm.repository.security;

import java.util.SortedSet;

/**
 * A model object interface for a hipposys:authrole node which is {@link Comparable} on its node path
 */
public interface AuthRole extends Comparable<AuthRole> {

    /**
     * @return the (node) name of the authrole
     */
    String getName();

    /**
     * @return the node path of the authrole
     */
    String getPath();

    /**
     * @return the parent (domain) node path of the authrole
     */
    String getDomainPath();

    /**
     * @return the role of the authrole
     */
    String getRole();

    /**
     * @return the description of the authrole
     */
    String getDescription();

    /**
     * @return the userrole of the authrole
     */
    String getUserRole();

    /**
     * @return the groups of the authrole
     */
    SortedSet<String> getGroups();

    /**
     * @return the users of the authrole
     */
    SortedSet<String> getUsers();
}
