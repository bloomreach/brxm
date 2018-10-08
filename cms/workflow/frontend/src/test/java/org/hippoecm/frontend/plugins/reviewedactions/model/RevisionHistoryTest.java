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
        Assert.assertTrue(isLatestRevisionOfBranch(labels));
    }


    @Test
    public void whenLabelsContainPublishedPreReintegrate() {
        labels = new String[]{PRE_REINTEGRATE + DASH + MASTER + DASH + UNPUBLISHED + DASH + COUNTER,
                MASTER + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels));
    }

    @Test
    public void whenLabelsContainUnPublishedPreReintegrateNonMaster() {
        labels = new String[]{PRE_REINTEGRATE + DASH + "qwert-" + UNPUBLISHED + DASH + COUNTER,
                MASTER + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels));
    }

    @Test
    public void whenLabelsContainPublishedPreReintegrateNonMaster() {
        labels = new String[]{PRE_REINTEGRATE + DASH + BRANCH_ID + DASH + UNPUBLISHED + DASH + COUNTER,
                MASTER + DASH + PUBLISHED};
        Assert.assertFalse(isLatestRevisionOfBranch(labels));
    }

    @Test
    public void whenLabelsContainPreReintegrateNonMaster() {
        labels = new String[]{PRE_REINTEGRATE + DASH + BRANCH_ID + UNPUBLISHED + DASH + COUNTER};
        Assert.assertFalse(isLatestRevisionOfBranch(labels));
    }

    @Test
    public void whenLabelsContainPublishedUnpublished() {
        labels = new String[]{MASTER + DASH + PUBLISHED,
                MASTER + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels));
    }

    @Test
    public void whenLabelsContainBranchUnpublished() {
        labels = new String[]{BRANCH_ID + DASH + UNPUBLISHED};
        Assert.assertTrue(isLatestRevisionOfBranch(labels));
    }

    private boolean isLatestRevisionOfBranch(final String[] labels) {
        RevisionHistory revisionHistory = new RevisionHistory(null);
        return revisionHistory.isLatestRevisionOfBranch(Stream.of(labels).collect(Collectors.toSet()));
    }
}