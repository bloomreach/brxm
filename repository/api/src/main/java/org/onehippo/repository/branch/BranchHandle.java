/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.onehippo.repository.branch;

import javax.jcr.Node;

/**
 * Contains nodes of document variants from either nodes under handle or from version history
 * based on the branchId and the document handle.
 */
public interface BranchHandle {

    /**
     * Returns the branch id for which this handle should return variants.
     * Note that if it is a non-existent branchId the variant getter methods will all return {@code null} except possibly
     * for {@link #getPublishedMaster()}
     *
     * @return branch id of this handle.
     */
    String getBranchId();

    /**
     * <p>
     *     Returns the published variant for the branch returned by {@link #getBranchId()}
     *     or {@code null} if there is no published node for the branch
     * </p>
     * <p>
     *     The returned node can be either a variant below the handle but can also be a versioned history node
     *     corresponding to the branch for {@link #getBranchId()}
     * </p>
     *
     * @return published variant
     */
    Node getPublished();

    /**
     * <p>
     *     Returns the unpublished variant for the branch returned by {@link #getBranchId()}
     *     or {@code null} if there is no unpublished node for the branch
     * </p>
     * <p>
     *     The returned node can be either a variant below the handle but can also be a versioned history node
     *     corresponding to the branch for {@link #getBranchId()}
     * </p>
     * @return unpublished variant
     */
    Node getUnpublished();

    /**
     * <p>
     *     Returns the draft variant for the branch returned by {@link #getBranchId()}
     *     or {@code null} if there is no draft node for the branch
     * </p>
     * <p>
     *     The returned node can be either a variant below the handle but can also be a versioned history node
     *     corresponding to the branch for {@link #getBranchId()}
     * </p>
     *
     * @return draft variant
     */
    Node getDraft();

    /**
     * <p>
     *     Tries to return the published master node for this {@link BranchHandle} or {@code null} if there is no
     *     master published. The returned node can be a node from version history
     * </p>
     * @return the published master node for this {@link BranchHandle} or {@code null} if there is no such node
     */
    Node getPublishedMaster();

    /**
     * Returns {@code true} if
     * <ul>
     * <li>the lastModifiedDate of {@link #getPublished()} is not equal that of {@link #getUnpublished()}</li>
     * <li>{@link #getPublished()} availability is not live or null and {@link #getUnpublished()} is not null</li>
     * </ul>
     * and {@code false} otherwise.
     *
     * @return whether the document is modified or not
     */
    boolean isModified();

    /**
     * Returns true if this branch handle's branchId is master
     *
     * @return if the handle is for master
     */
    boolean isMaster();

    /**
     * Returns {@code true} if the published variant for {@link #getBranchId()} exists and is live, {@code false}
     * otherwise.
     *
     * @return if the published variant exists and is live
     */
    boolean isLive();
}
