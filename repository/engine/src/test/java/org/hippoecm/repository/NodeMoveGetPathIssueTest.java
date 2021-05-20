/*
 *  Copyright 2019-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.stream.Collectors;

import org.hippoecm.repository.jackrabbit.QFacetRuleStateManager;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeMoveGetPathIssueTest extends RepositoryTestCase {

    @Test
    public void assert_moved_node_results_in_get_path_issue() throws Exception {

        for (int i = 0; i < 100; i++) {

            try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(QFacetRuleStateManager.class).build()) {
                session.move("/hippo:configuration/hippo:domains/defaultread/test-domain/test-and-descendants", "/tmp");
                session.save();

                try {
                    assertThat(interceptor.messages().collect(Collectors.toSet()))
                            .containsExactlyInAnyOrder("Skipping facet rule in not-supported location",
                                    "Exception while fetching path for facet rule. Most likely the result of a node move which " +
                                            "has not been processed by other (session) listeners. Ignore this exception");
                } catch (AssertionError e) {
                    // sometimes the #getPath succeeds (concurrency?). Hence the test also passes if only the warning below
                    // is logged
                    assertThat(interceptor.messages().collect(Collectors.toSet()))
                            .containsExactlyInAnyOrder("Skipping facet rule in not-supported location: /tmp");
                }

            } finally {
                session.move("/tmp", "/hippo:configuration/hippo:domains/defaultread/test-domain/test-and-descendants");
                session.save();
            }
        }

    }

}
