/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.onehippo.repository.branch.BranchConstants;

public final class BranchInfoBuilder {

    /**
     * Translation key.
     */
    public static final String UNPUBLISHED_CHANGES_KEY = "unpublished-changes";
    /**
     * Translation key.
     */
    public static final String DRAFT_CHANGES_KEY = "draft-changes";
    /**
     * Translation key.
     */
    public static final String LIVE_KEY = "live";
    /**
     * Translation key.
     */
    public static final String OFFLINE_KEY = "offline";
    /**
     * Translation key.
     */
    public static final String CORE_DOCUMENT_KEY = "core-document";
    /**
     * Translation key.
     */
    public static final String BRANCH_INFO_KEY = "branch-info";
    /**
     * Returns the translations associated with the translations keys.
     */
    private final UnaryOperator<String> propertyResolver;
    /**
     * {@code true} is document is live, otherwise {@code false}.
     */
    private boolean live;
    /**
     * {@code true} if the document is modified
     * ( changes between live and unpublished variant ),
     * otherwise {@code false}.
     */
    private boolean unpublishedChanges;
    /**
     * {@code true} if the document has draft changes
     * ( changes between the unpublished and draft variant ),
     * otherwise {@code false}.
     */
    private boolean draftChanges;
    /**
     * The name of the branch currently associated with this document.
     */
    private String branchName;

    /**
     * <p>Creates a new instance. It has the following default values:
     * <ul>
     *     <li>branchName: {@link BranchConstants#MASTER_BRANCH_ID}</li>
     *     <li>live, draftChanges, unpublishedChanges : {@code false}</li>
     * </ul>
     * </p>
     *
     * @param resolver {@link UnaryOperator} that looks up the
     *                 translations by key.
     */
    public BranchInfoBuilder(final UnaryOperator<String> resolver, Supplier<String> branchNameSupplier) {
        propertyResolver = resolver;
        branchName = branchNameSupplier.get();
    }

    /**
     * <p>Sets the publication state to "live".</p>
     * @param live {@code true} is document is live
     * @return {@link BranchInfoBuilder}
     */
    public BranchInfoBuilder live(boolean live) {
        this.live = live;
        return this;
    }

    /**
     * <p>Set the unpublished state to "unpublished changes".</p>
     *
     * @param changes {@code true} if document has unpublished changes
     *                            ( differences between unpublished and
     *                            published variant )
     * @return {@link BranchInfoBuilder}
     */
    public BranchInfoBuilder unpublishedChanges(boolean changes) {
        this.unpublishedChanges = changes;
        return this;
    }

    /**
     * <p>Sets the draft state to "draft changes".</p>
     *
     * @param changes {@link true} is document has draft changes
     *                            (differences between draft and unpublished
     *                            variant )
     * @return {@link BranchInfoBuilder}
     */
    public BranchInfoBuilder draftChanges(boolean changes) {
        this.draftChanges = changes;
        return this;
    }

    /**
     * <p>Builds a string with the translate document info:
     * <ul>
     *     <li>branch info ( with which branch the document is currently
     *     associated )</li>
     *     <li>if the document is "live" or "offline"</li>
     *     <li>if the document has draft changes ( changes that aren't saved
     *     to the unpublished variant )</li>
     *     <li>if the document has unpublished changes ( changes that aren't
     *     published ) </li>
     * </ul>
     * <p>If a document both has draft changes and unpublished changes only
     * draft changes is added.</p>
     * <p>If a document has unpublished changes and the document is not live
     * the info is omitted.</p>
     * <p>The rationale behind this is that is info guides the user to the next
     * action.</p>
     * </p>
     *
     * @return Translated info string.
     */
    public String build() {
        String documentState = Stream.of(getPublicationStateKey(),
                getDraftChangesKey(),
                live ? getUnpublishedChangesKey() : StringUtils.EMPTY)
                .filter(StringUtils::isNotEmpty)
                .map(this::getValue)
                .limit(2)
                .collect(Collectors.joining(", "));
        return String.format("%s (%s)", getBranchInfo(), documentState);
    }

    private String getBranchInfo() {
        return BranchConstants.MASTER_BRANCH_ID.equals(branchName)
                ? getValue(CORE_DOCUMENT_KEY)
                : String.format("%s '%s'", getValue(BRANCH_INFO_KEY),
                branchName);
    }

    private String getUnpublishedChangesKey() {
        return unpublishedChanges ? UNPUBLISHED_CHANGES_KEY : StringUtils.EMPTY;
    }

    private String getDraftChangesKey() {
        return draftChanges ? DRAFT_CHANGES_KEY : StringUtils.EMPTY;
    }

    private String getPublicationStateKey() {
        return live ? LIVE_KEY : OFFLINE_KEY;
    }

    private String getValue(final String key) {
        return key == null ? null : this.propertyResolver.apply(key);
    }
}
