/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import static org.hippoecm.frontend.plugins.reviewedactions.model.RevisionHistory.DASH;
import static org.hippoecm.frontend.plugins.reviewedactions.model.RevisionHistory.UNPUBLISHED;

public class RevisionHistoryTest {

    private static final String BRANCH_ID = "qIdcu";
    private static final String PRE_REINTEGRATE = "pre" + DASH + "reintegrate";
    private static final String MASTER = "master";
    private static final String PUBLISHED = "published";
    private static final String COUNTER = "3";
    private String[] labels;

    @Test
    public void whenLabelsContainUnPublishedPublishedPreReintegrate() {
        labels = new String[]{PRE_REINTEGRATE + DASH + MASTER + DASH + UNPUBLISHED + DASH + COUNTER,
                MASTER + DASH + PUBLISHED,
                MASTER + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels,MASTER));
    }


    @Test
    public void whenLabelsContainPublishedPreReintegrate()  {
        labels = new String[]{PRE_REINTEGRATE + DASH + MASTER + DASH + UNPUBLISHED + DASH + COUNTER,
                MASTER + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels, MASTER));
    }

    @Test
    public void whenLabelsContainUnPublishedPreReintegrateNonMaster()  {
        labels = new String[]{PRE_REINTEGRATE + DASH + "qwert-" + UNPUBLISHED + DASH + COUNTER,
                MASTER + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels, MASTER));
    }

    @Test
    public void whenLabelsContainPublishedPreReintegrateNonMaster()  {
        labels = new String[]{PRE_REINTEGRATE + DASH + BRANCH_ID + DASH + UNPUBLISHED + DASH + COUNTER,
                MASTER + DASH + PUBLISHED};
        Assert.assertFalse(isLatestRevisionOfBranch(labels, BRANCH_ID));
    }

    @Test
    public void whenLabelsContainPreReintegrateNonMaster()  {
        labels = new String[]{PRE_REINTEGRATE + DASH + BRANCH_ID + UNPUBLISHED + DASH + COUNTER};
        Assert.assertFalse(isLatestRevisionOfBranch(labels, BRANCH_ID));
    }

    @Test
    public void whenLabelsContainPublishedUnpublished()  {
        labels = new String[]{MASTER + DASH + PUBLISHED,
                MASTER + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels, MASTER));
    }

    @Test
    public void whenLabelsContainBranchUnpublished() {
        labels = new String[]{BRANCH_ID + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels, BRANCH_ID));
    }

    private boolean isLatestRevisionOfBranch(final String[] labels, String branchId) {
        RevisionHistory revisionHistory = new RevisionHistory(null, branchId);
        return revisionHistory.isLatestRevisionOfBranch(Stream.of(labels).collect(Collectors.toSet()));
    }
}