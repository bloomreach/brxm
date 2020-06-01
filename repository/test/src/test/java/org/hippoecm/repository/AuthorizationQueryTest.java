/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hippoecm.repository.impl.SessionDecorator;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class AuthorizationQueryTest extends RepositoryTestCase {

    @Test
    public void admin_results_in_match_all_query() throws Exception {
        Session admin = null;
        try {
            admin = server.login(new SimpleCredentials("admin", "admin".toCharArray()));
            InternalHippoSession unwrap = (InternalHippoSession) SessionDecorator.unwrap(admin);
            final AuthorizationQuery authorizationQuery = unwrap.getAuthorizationQuery();

            assertEquals(1, authorizationQuery.getQuery().getClauses().length);
            assertEquals(new MatchAllDocsQuery(), authorizationQuery.getQuery().getClauses()[0].getQuery());

        } finally {
            admin.logout();
        }
    }

    @Test
    public void author_results_in_non_match_all_query() throws Exception {
        Session author = null;
        try {
            author = server.login(new SimpleCredentials("author", "author".toCharArray()));
            InternalHippoSession unwrap = (InternalHippoSession) SessionDecorator.unwrap(author);
            final AuthorizationQuery authorizationQuery = unwrap.getAuthorizationQuery();

            assertNotEquals(new MatchAllDocsQuery(), authorizationQuery.getQuery().getClauses()[0].getQuery());
        } finally {
            author.logout();
        }
    }

    /**
     * when marking a user as 'system', it does *NOT* mean that it becomes a jcr system session with jcr:all, but just
     * that the running repository requires this user to function properly. Marking a user to be system thus should not
     * result in the authorizationQuery to become a match all query
     */

    @Test
    public void user_marked_as_system_does_not_result_in_match_all_query() throws Exception {
        Session author = null;
        try {
            author = server.login(new SimpleCredentials("author", "author".toCharArray()));

            InternalHippoSession unwrap = (InternalHippoSession) SessionDecorator.unwrap(author);

            assertFalse("author not expected to be a JCR System Session", unwrap.isSystemSession());
            assertFalse("author not expected to be marked as system user ",unwrap.getUser().isSystemUser());

            final AuthorizationQuery authorizationQuery = unwrap.getAuthorizationQuery();

            assertNotEquals(new MatchAllDocsQuery(), authorizationQuery.getQuery().getClauses()[0].getQuery());
        } finally {
            author.logout();
        }

        session.getNode("/hippo:configuration/hippo:users/author").setProperty("hipposys:system", true);
        session.save();

        try {
            author = server.login(new SimpleCredentials("author", "author".toCharArray()));

            InternalHippoSession unwrap = (InternalHippoSession) SessionDecorator.unwrap(author);

            assertFalse("author not expected to be a JCR System Session ALTHOUGH marked as system", unwrap.isSystemSession());
            assertTrue("author EXPECTED to be marked as system user",unwrap.getUser().isSystemUser());

            final AuthorizationQuery authorizationQuery = unwrap.getAuthorizationQuery();

            assertNotEquals("Although MARKED as system, the author user still shouldn't be a JCR System Session",
                    new MatchAllDocsQuery(), authorizationQuery.getQuery().getClauses()[0].getQuery());
        } finally {
            author.logout();
        }
        session.getNode("/hippo:configuration/hippo:users/author").getProperty("hipposys:system").remove();
        session.save();
    }
}
