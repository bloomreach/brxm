/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest.model.tmp;

import java.io.Serializable;

/**
 * @version "$Id$"
 */
public class UserPlan implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4759542049129654659L;

    private long collaborators;

    private long privateRepos;

    private long space;

    private String name;

    /**
     * @return collaborators
     */
    public long getCollaborators() {
        return collaborators;
    }

    /**
     * @param collaborators
     * @return this user plan
     */
    public UserPlan setCollaborators(long collaborators) {
        this.collaborators = collaborators;
        return this;
    }

    /**
     * @return privateRepos
     */
    public long getPrivateRepos() {
        return privateRepos;
    }

    /**
     * @param privateRepos
     * @return this user plan
     */
    public UserPlan setPrivateRepos(long privateRepos) {
        this.privateRepos = privateRepos;
        return this;
    }

    /**
     * @return space
     */
    public long getSpace() {
        return space;
    }

    /**
     * @param space
     * @return this user plan
     */
    public UserPlan setSpace(long space) {
        this.space = space;
        return this;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     * @return this user plan
     */
    public UserPlan setName(String name) {
        this.name = name;
        return this;
    }
}
