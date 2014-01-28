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

/**
 * Gist change status class.
 */
public class GistChangeStatus implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 9189375293271905239L;

    private int additions;

    private int deletions;

    private int total;

    /**
     * @return additions
     */
    public int getAdditions() {
        return additions;
    }

    /**
     * @param additions
     * @return this gist change status
     */
    public GistChangeStatus setAdditions(int additions) {
        this.additions = additions;
        return this;
    }

    /**
     * @return deletions
     */
    public int getDeletions() {
        return deletions;
    }

    /**
     * @param deletions
     * @return this gist change status
     */
    public GistChangeStatus setDeletions(int deletions) {
        this.deletions = deletions;
        return this;
    }

    /**
     * @return total
     */
    public int getTotal() {
        return total;
    }

    /**
     * @param total
     * @return this gist change status
     */
    public GistChangeStatus setTotal(int total) {
        this.total = total;
        return this;
    }
}