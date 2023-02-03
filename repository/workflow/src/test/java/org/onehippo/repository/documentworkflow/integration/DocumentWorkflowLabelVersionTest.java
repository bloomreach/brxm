/*
 * Copyright 2021-2023 Bloomreach
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
import org.onehippo.repository.documentworkflow.campaign.JcrVersionsMetaUtils;
import org.onehippo.repository.campaign.VersionLabel;
import org.onehippo.repository.campaign.VersionsMeta;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class DocumentWorkflowLabelVersionTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void assert_label_versions_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        Map<String, Serializable> hints = workflow.hints();

        assertThat(hints.get("labelVersion")).isEqualTo(true);
        assertThat(hints.get("removeLabelVersion")).isEqualTo(true);


        workflow.branch("foo", "Foo");

        Map<String, Serializable> hints2 = workflow.hints("foo");

        assertThat(hints2.get("labelVersion")).isEqualTo(true);
        assertThat(hints2.get("removeLabelVersion")).isEqualTo(true);

    }

    @Test
    public void label_version_workflow_failure_assertions() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        assertThatThrownBy(() -> workflow.labelVersion("not-a-uuid", "My Label"))
                .hasMessage("invalid identifier: not-a-uuid")
                .isInstanceOf(WorkflowException.class);

        assertThatThrownBy(() -> workflow.labelVersion(handle.getIdentifier(), "My Label"))
                .hasMessage(format("Node for '%s' is not a node of type '%s'", handle.getIdentifier(), NT_FROZEN_NODE))
                .isInstanceOf(WorkflowException.class);
    }

    @Test
    public void label_version_workflow_assertions() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // create a version
        final Document version = workflow.version();
        final Version versionNode = (Version)version.getNode(session);

        final String frozenNodeId = versionNode.getFrozenNode().getIdentifier();
        workflow.labelVersion(frozenNodeId, "My Label");

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            final List<VersionLabel> labels = versionsMeta.getVersionLabels().stream().filter(label -> frozenNodeId.equals(label.getUuid()))
                    .collect(Collectors.toList());
            assertThat(labels.size()).isEqualTo(1);
            final VersionLabel label = labels.get(0);
            assertThat(label.getVersionLabel()).isEqualTo("My Label");
        }

        // update the label

        workflow.labelVersion(frozenNodeId, "My New Label");

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            final List<VersionLabel> labels = versionsMeta.getVersionLabels().stream().filter(label -> frozenNodeId.equals(label.getUuid()))
                    .collect(Collectors.toList());
            assertThat(labels.size()).isEqualTo(1);
            final VersionLabel label = labels.get(0);
            assertThat(label.getVersionLabel()).isEqualTo("My New Label");
        }

        // add a second label for a new version

        // create new version
        final Document version2 = workflow.version();
        final Version versionNode2 = (Version)version2.getNode(session);

        String frozenNodeId2 = versionNode2.getFrozenNode().getIdentifier();
        workflow.labelVersion(frozenNodeId2, "My Second Label");

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getVersionLabels().size()).isEqualTo(2);
            final VersionLabel label = versionsMeta.getVersionLabel(frozenNodeId2).get();
            assertThat(label.getVersionLabel()).isEqualTo("My Second Label");
        }

        workflow.removeLabelVersion(frozenNodeId);
        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getVersionLabels().size()).isEqualTo(1);
            assertThat(versionsMeta.getVersionLabel(frozenNodeId).isPresent()).isFalse();
            assertThat(versionsMeta.getVersionLabel(frozenNodeId2).isPresent()).isTrue();
        }

        workflow.removeLabelVersion(frozenNodeId2);
        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getVersionLabels().size()).isEqualTo(0);
            assertThat(versionsMeta.getVersionLabel(frozenNodeId2).isPresent()).isFalse();
        }

    }

    /**
     * Since both campaign info and version label info end up in the hippo:versionsMeta on the handle, create a test
     * which asserts both work independently
     */
    @Test
    public void campaign_and_label_version_workflow_assertions() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        // create a version
        final Document version = workflow.version();
        final Version versionNode = (Version)version.getNode(session);

        final String frozenNodeId = versionNode.getFrozenNode().getIdentifier();
        workflow.labelVersion(frozenNodeId, "My Label");

        final Calendar from = new Calendar.Builder().setDate(2019, 1, 2).build();
        final Calendar to = new Calendar.Builder().setDate(2021, 1, 2).build();

        workflow.campaign(frozenNodeId, MASTER_BRANCH_ID,  from, to);

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            final List<VersionLabel> labels = versionsMeta.getVersionLabels().stream().filter(label -> frozenNodeId.equals(label.getUuid()))
                    .collect(Collectors.toList());
            assertThat(labels.size()).isEqualTo(1);
            final VersionLabel label = labels.get(0);
            assertThat(label.getVersionLabel()).isEqualTo("My Label");
            final List<Campaign> campaigns = versionsMeta.getCampaigns().stream().filter(campaign -> frozenNodeId.equals(campaign.getUuid()))
                    .collect(Collectors.toList());
            assertThat(campaigns.size()).isEqualTo(1);
            final Campaign campaign = campaigns.get(0);
            assertThat(campaign.getFrom().getTimeInMillis()).isEqualTo(from.getTimeInMillis());
            assertThat(campaign.getTo().getTimeInMillis()).isEqualTo(to.getTimeInMillis());
        }

        workflow.removeCampaign(frozenNodeId);
        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            final List<VersionLabel> labels = versionsMeta.getVersionLabels().stream().filter(label -> frozenNodeId.equals(label.getUuid()))
                    .collect(Collectors.toList());
            assertThat(labels.size()).isEqualTo(1);
            final VersionLabel label = labels.get(0);
            assertThat(label.getVersionLabel()).isEqualTo("My Label");
            assertThat(versionsMeta.getCampaigns().size()).isEqualTo(0);
            assertThat(versionsMeta.getCampaign(frozenNodeId).isPresent()).isFalse();
        }
        workflow.removeLabelVersion(frozenNodeId);
        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            assertThat(versionsMeta.getVersionLabels().size()).isEqualTo(0);
            assertThat(versionsMeta.getCampaigns().size()).isEqualTo(0);
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
        final Version versionNode = (Version)version.getNode(session);

        final String frozenNodeId = versionNode.getFrozenNode().getIdentifier();

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(JcrVersionsMetaUtils.class).build()) {
            workflow.labelVersion(frozenNodeId, "My Label");
            assertThat(interceptor.messages().allMatch(msg -> msg.startsWith("Invalid stored versionsMeta at '/test/document'")))
                    .isTrue();
        }

        {
            final VersionsMeta versionsMeta = JcrVersionsMetaUtils.getVersionsMeta(handle);
            final List<VersionLabel> labels = versionsMeta.getVersionLabels().stream().filter(campaign -> frozenNodeId.equals(campaign.getUuid()))
                    .collect(Collectors.toList());
            assertThat(labels.size()).isEqualTo(1);
            final VersionLabel label = labels.get(0);
            assertThat(label.getVersionLabel()).isEqualTo("My Label");
        }
    }

}
