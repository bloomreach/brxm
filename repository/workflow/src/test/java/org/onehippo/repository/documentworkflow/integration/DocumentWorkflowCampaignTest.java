/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.onehippo.repository.documentworkflow.integration;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.version.Version;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.campaign.Campaign;
import org.onehippo.repository.campaign.VersionLabel;
import org.onehippo.repository.campaign.VersionsMeta;
import org.onehippo.repository.documentworkflow.campaign.JcrVersionsMetaUtils;
import org.onehippo.repository.scxml.SCXMLWorkflowExecutor;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class DocumentWorkflowCampaignTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void assert_campaign_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        Map<String, Serializable> hints = workflow.hints();

        assertThat(hints.get("campaign")).isEqualTo(true);
        assertThat(hints.get("removeCampaign")).isEqualTo(true);

        workflow.branch("foo", "Foo");

        Map<String, Serializable> hints2 = workflow.hints("foo");

        assertThat(hints2.get("campaign")).isEqualTo(true);
        assertThat(hints2.get("removeCampaign")).isEqualTo(true);

    }

    @Test
    public void campaign_workflow_failure_assertions() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        // create a version
        final Document version = workflow.version();
        final Version versionNode = (Version) version.getNode(session);

        Calendar from = new Calendar.Builder().setDate(2019, 1, 2).build();
        Calendar to = new Calendar.Builder().setDate(2021, 1, 2).build();

        try (Log4jInterceptor ignore = Log4jInterceptor.onWarn().trap(SCXMLWorkflowExecutor.class).build()) {
            assertThatThrownBy(() -> workflow.campaign("not-a-uuid", MASTER_BRANCH_ID, from, to))
                    .hasMessage("invalid identifier: not-a-uuid")
                    .isInstanceOf(WorkflowException.class);

            assertThatThrownBy(() -> workflow.campaign(handle.getIdentifier(), MASTER_BRANCH_ID, from, to))
                    .hasMessage(format("Node for '%s' is not a node of type '%s'", handle.getIdentifier(), NT_FROZEN_NODE))
                    .isInstanceOf(WorkflowException.class);

            assertThatThrownBy(() -> workflow.campaign(versionNode.getFrozenNode().getIdentifier(), "foo", from, to))
                    .hasMessage(format("Node for '%s' is not for branch '%s' hence not allowed to be a " +
                            "campaign for that branch", versionNode.getFrozenNode().getIdentifier(), "foo"))
                    .isInstanceOf(WorkflowException.class);

            Calendar fromAfterTo = new Calendar.Builder().setDate(2022, 1, 2).build();

            assertThatThrownBy(() -> workflow.campaign(versionNode.getFrozenNode().getIdentifier(), MASTER_BRANCH_ID, fromAfterTo, to))
                    .hasMessage(format("Not allowed to have a 'to' date '%s' being before the 'from' date '%s'",
                            to, fromAfterTo))
                    .isInstanceOf(WorkflowException.class);
        }
    }

    @Test
    public void campaign_workflow_assertions() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // create a version
        final Document version = workflow.version();
        final Version versionNode = (Version) version.getNode(session);

        final Calendar from = new Calendar.Builder().setDate(2019, 1, 2).build();
        final Calendar to = new Calendar.Builder().setDate(2021, 1, 2).build();

        final String frozenNodeId = versionNode.getFrozenNode().getIdentifier();
        workflow.campaign(frozenNodeId, MASTER_BRANCH_ID, from, to);

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            final List<Campaign> campaigns = versionsMeta.getCampaigns().stream().filter(campaign -> frozenNodeId.equals(campaign.getUuid()))
                    .collect(Collectors.toList());
            assertThat(campaigns.size()).isEqualTo(1);
            final Campaign campaign = campaigns.get(0);
            assertThat(campaign.getFrom().getTimeInMillis()).isEqualTo(from.getTimeInMillis());
            assertThat(campaign.getTo().getTimeInMillis()).isEqualTo(to.getTimeInMillis());
        }

        // update the from value
        final Calendar fromNew = new Calendar.Builder().setDate(2018, 1, 2).build();

        workflow.campaign(frozenNodeId, MASTER_BRANCH_ID, fromNew, to);

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getCampaigns().size()).isEqualTo(1);
            Campaign campaign = versionsMeta.getCampaign(frozenNodeId).get();
            assertThat(campaign.getFrom().getTimeInMillis()).isEqualTo(fromNew.getTimeInMillis());
            assertThat(campaign.getTo().getTimeInMillis()).isEqualTo(to.getTimeInMillis());
        }

        // add a second campaign for a new version

        // create new version
        final Document version2 = workflow.version();
        final Version versionNode2 = (Version) version2.getNode(session);

        String frozenNodeId2 = versionNode2.getFrozenNode().getIdentifier();
        workflow.campaign(frozenNodeId2, MASTER_BRANCH_ID, fromNew, to);

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getCampaigns().size()).isEqualTo(2);
            final Campaign campaign = versionsMeta.getCampaign(frozenNodeId2).get();
            assertThat(campaign.getFrom().getTimeInMillis()).isEqualTo(fromNew.getTimeInMillis());
            assertThat(campaign.getTo().getTimeInMillis()).isEqualTo(to.getTimeInMillis());
        }

        workflow.removeCampaign(frozenNodeId);
        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getCampaigns().size()).isEqualTo(1);
            assertThat(versionsMeta.getCampaign(frozenNodeId).isPresent()).isFalse();
            assertThat(versionsMeta.getCampaign(frozenNodeId2).isPresent()).isTrue();
        }

        workflow.removeCampaign(frozenNodeId2);
        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getCampaigns().size()).isEqualTo(0);
            assertThat(versionsMeta.getCampaign(frozenNodeId2).isPresent()).isFalse();
        }

        // add open-ended campaign, aka, no end-date
        workflow.campaign(frozenNodeId2, MASTER_BRANCH_ID,  fromNew, null);
        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getCampaigns().size()).isEqualTo(1);
            final Campaign campaign = versionsMeta.getCampaign(frozenNodeId2).get();
            assertThat(campaign.getFrom().getTimeInMillis()).isEqualTo(fromNew.getTimeInMillis());
            assertThat(campaign.getTo()).isNull();
        }

    }

    @Test
    public void invalid_stored_versionsMeta_logs_error_and_gets_replaced() throws Exception {
        // add invalid json which does not map to versionsMeta
        handle.addMixin(NT_HIPPO_VERSION_INFO);
        handle.setProperty(HippoNodeType.HIPPO_VERSIONS_META, "no-json");
        session.save();

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // create a version
        final Document version = workflow.version();
        final Version versionNode = (Version) version.getNode(session);

        final Calendar from = new Calendar.Builder().setDate(2019, 1, 2).build();
        final Calendar to = new Calendar.Builder().setDate(2021, 1, 2).build();

        final String frozenNodeId = versionNode.getFrozenNode().getIdentifier();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrVersionsMetaUtils.class).build()) {
            workflow.campaign(frozenNodeId, MASTER_BRANCH_ID, from, to);
            assertThat(interceptor.messages().allMatch(msg -> msg.startsWith("Invalid stored versionsMeta at '/test/document'")))
                    .isTrue();
        }

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            final List<Campaign> campaigns = versionsMeta.getCampaigns().stream().filter(campaign -> frozenNodeId.equals(campaign.getUuid()))
                    .collect(Collectors.toList());
            assertThat(campaigns.size()).isEqualTo(1);
            final Campaign campaign = campaigns.get(0);
            assertThat(campaign.getFrom().getTimeInMillis()).isEqualTo(from.getTimeInMillis());
            assertThat(campaign.getTo().getTimeInMillis()).isEqualTo(to.getTimeInMillis());
        }
    }

}
