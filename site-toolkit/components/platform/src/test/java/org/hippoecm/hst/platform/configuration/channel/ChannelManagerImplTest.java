/*
 * Copyright 2020-2023 Bloomreach
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
package org.hippoecm.hst.platform.configuration.channel;

import javax.jcr.Node;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_HOSTS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_VIRTUALHOST;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS;

public class ChannelManagerImplTest {

    @Test
    public void testFindSubstitutedNodeNames() throws Exception {

        /**
         * Create
         * <pre>
         *     + root
         *       + hst:hosts
         *         + production
         *              + brc
         *                + cloud
         *                  + bloomreach
         *                     + ${brc_stack}
         *                        + ${brc_environment}
         *                     + www-${brc_environment}-${brc_stack}
         *
         * </pre>
         */
        final MockNode root = new MockNode("root");
        final MockNode virtualHosts = root.addNode(NODENAME_HST_HOSTS, NODETYPE_HST_VIRTUALHOSTS);
        final MockNode hostGroup = virtualHosts.addNode("prod", NODETYPE_HST_VIRTUALHOSTGROUP);
        final MockNode brc = hostGroup.addNode("brc", NODETYPE_HST_VIRTUALHOST);
        final Node cloud = brc.addNode("cloud", NODETYPE_HST_VIRTUALHOST);
        final Node bloomreach = cloud.addNode("bloomreach", NODETYPE_HST_VIRTUALHOST);
        final Node stack = bloomreach.addNode("${brc_stack}", NODETYPE_HST_VIRTUALHOST);
        final Node environment = stack.addNode("${brc_environment}", NODETYPE_HST_VIRTUALHOST);

        final Node stack_and_env = bloomreach.addNode("www-${brc_environment}-${brc_stack}", NODETYPE_HST_VIRTUALHOST);

        try {
            System.setProperty("brc_stack", "saascicdtest");
            System.setProperty("brc_environment", "dev-green");

            assertThat(ChannelManagerImpl.getOrCreateVirtualHost(root, "bloomreach.cloud.brc", "prod"))
                    .as("Expected an existing virtual host to be returned")
                    .isSameAs(bloomreach);

            assertThat(ChannelManagerImpl.getOrCreateVirtualHost(root, "saascicdtest.bloomreach.cloud.brc", "prod"))
                    .as("Expected an existing virtual host to be returned as 'stack' should be replaced by ${brc_stack}")
                    .isSameAs(stack);

            assertThat(ChannelManagerImpl.getOrCreateVirtualHost(root, "dev-green.saascicdtest.bloomreach.cloud.brc", "prod"))
                    .as("Expected an existing virtual host to be returned as 'saascicdtest' should be replaced by ${brc_stack} " +
                            "and 'dev-green' with '${brc_environment}'")
                    .isSameAs(environment);

            final Node newHost = ChannelManagerImpl.getOrCreateVirtualHost(root, "dev-blue.saascicdtest.bloomreach.cloud.brc", "prod");

            assertThat(newHost.getPath())
                    .as("Expected a newly added host to be returned as 'saascicdtest' should be replaced by ${brc_stack} " +
                            "but 'dev-blue' cannot be replaced by a system property")
                    .isEqualTo("/hst:hosts/prod/brc/cloud/bloomreach/${brc_stack}/dev-blue");

            assertThat(newHost.getParent())
                    .as("Expected an existing parent as 'saascicdtest' should be replaced by ${brc_stack}")
                    .isSameAs(stack);

            // even a host name like www-${brc_environment}-${brc_stack} is usable and if it matches after substitution,
            // a new host should be added there
            assertThat(ChannelManagerImpl.getOrCreateVirtualHost(root, "www-dev-green-saascicdtest.bloomreach.cloud.brc", "prod"))
                    .as("Expected an existing virtual host to be returned as 'saascicdtest' should be replaced by ${brc_stack} " +
                            "and 'dev-green' with '${brc_environment}'")
                    .isSameAs(stack_and_env);


            final Node newHost2 = ChannelManagerImpl.getOrCreateVirtualHost(root, "www-dev-blue-saascicdtest.bloomreach.cloud.brc", "prod");

            assertThat(newHost2.getPath())
                    .as("Expected a newly added host to be returned as www-dev-blue-saascicdtest replaced with " +
                            "property values will result in 'www-dev-blue-${brc_stack}' for which there is no host")
                    .isEqualTo("/hst:hosts/prod/brc/cloud/bloomreach/www-dev-blue-saascicdtest");

            assertThat(newHost2.getParent())
                    .as("Expected an existing parent 'bloomreach'")
                    .isSameAs(bloomreach);

        } finally {
            System.clearProperty("brc_stack");
            System.clearProperty("brc_environment");
        }
    }


}
