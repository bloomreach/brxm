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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.Utilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.security.DomainInfoPrivilege;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MEMBERS;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.security.SecurityConstants.USERROLE_REPOSITORY_BROWSER_USER;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_MODIFY_PROPERTIES;
import static org.onehippo.repository.security.StandardPermissionNames.JCR_READ;

@Ignore
public class DefaultAuthorizationSetupTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Node testdirectory = session.getNode("/content").addNode("testdirectory", NT_DIRECTORY);
        testdirectory.addNode("testfolder", NT_FOLDER);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        session.getNode("/content/testdirectory").remove();
        session.save();
        super.tearDown();
    }

    @Test
    public void user_admin_is_in_role_repository_browser_user() throws Exception {
        Session user = null;
        try {
            user = server.login(CREDENTIALS);
            assertTrue("admin should be allowed to browse the repository servlet",
                    ((HippoSession)user).isUserInRole(USERROLE_REPOSITORY_BROWSER_USER));
        } finally {
            if (user != null) {
                user.logout();
            }
        }

        for (String userName : new String[]{"author", "editor"}) {
            try {
                user = server.login(userName, userName.toCharArray());
                assertFalse(String.format("%s should NOT be allowed to browse the repository servlet", userName),
                        ((HippoSession)user).isUserInRole(USERROLE_REPOSITORY_BROWSER_USER));
            } finally {
                if (user != null) {
                    user.logout();
                }
            }
        }
    }

    @Test
    public void group_admin_is_in_role_repository_browser_user() throws Exception {
        Value[] orginal = session.getNode("/hippo:configuration/hippo:groups/admin").getProperty(HIPPO_MEMBERS).getValues();
        session.getNode("/hippo:configuration/hippo:groups/admin").setProperty(HIPPO_MEMBERS, new String[]{"author"});
        session.save();
        Session user = null;
        try {
            user = server.login("author", "author".toCharArray());
            assertTrue(String.format("author should be allowed to browse the repository servlet since admin group should " +
                            "be allowed to do so"),
                    ((HippoSession)user).isUserInRole(USERROLE_REPOSITORY_BROWSER_USER));

        } finally {
            if (user != null) {
                user.logout();
            }
            session.getNode("/hippo:configuration/hippo:groups/admin").setProperty(HIPPO_MEMBERS, orginal);
            session.save();
        }
    }

    @Test
    public void author_and_editor_do_not_have_jcr_write_on_folders_or_directories_below_content() throws Exception {

        validateFolderPermissions("author");
        validateFolderPermissions("editor");

    }

    private void validateFolderPermissions(final String userName) throws RepositoryException {
        final Session user = server.login(userName, userName.toCharArray());

        try {
            assertTrue(user.hasPermission("/content/testdirectory/foo", Session.ACTION_READ));
            assertFalse(user.hasPermission("/content/testdirectory/foo", Session.ACTION_ADD_NODE));
            assertTrue(user.hasPermission("/content/testdirectory", JCR_READ));
            assertFalse(user.hasPermission("/content/testdirectory", JCR_MODIFY_PROPERTIES));
            assertFalse(user.hasPermission("/content/testdirectory/testfolder/foo", Session.ACTION_ADD_NODE));
            assertFalse(user.hasPermission("/content/testdirectory/testfolder", JCR_MODIFY_PROPERTIES));
            user.getNode("/content/testdirectory").addNode("newAuthorDirectory", NT_DIRECTORY);
            user.save();
            fail("Expected unauthorized");
        } catch (RepositoryException e) {
            // expected
        } finally {
            user.logout();
        }
    }


    @Test
    public void author_and_editor_do_have_write_access_to_draft_documents_they_are_holder_on() throws Exception {
        final Node handle = session.getNode("/content/testdirectory").addNode("doc", NT_HANDLE);
        final Node liveVariant = handle.addNode("doc", "hippo:testdocument");
        liveVariant.setProperty(HIPPOSTD_STATE, new String[] {"published"});
        liveVariant.setProperty(HIPPOSTD_HOLDER, "author");
        final Node draftVariant = handle.addNode("doc", "hippo:testdocument");
        draftVariant.setProperty(HIPPOSTD_STATE, new String[] {"draft"});
        draftVariant.setProperty(HIPPOSTD_HOLDER, "author");
        session.save();

        final Session author = server.login("author", "author".toCharArray());

        try {
            assertFalse("only draft variant for which hippostd:holder matches should have jcr:write",
                    author.hasPermission(liveVariant.getPath() + "/foo", Session.ACTION_ADD_NODE));
            assertFalse(author.hasPermission(liveVariant.getPath(), JCR_MODIFY_PROPERTIES));

            assertTrue(author.hasPermission(draftVariant.getPath() + "/foo", Session.ACTION_ADD_NODE));
            assertTrue(author.hasPermission(draftVariant.getPath(), JCR_MODIFY_PROPERTIES));
        }  finally {
            author.logout();
        }

        final Session editor = server.login("editor", "editor".toCharArray());

        try {
            assertFalse(editor.hasPermission(liveVariant.getPath()+ "/foo", Session.ACTION_ADD_NODE));
            assertFalse(editor.hasPermission(liveVariant.getPath(), JCR_MODIFY_PROPERTIES));
            assertFalse("author is the holder so editor should not be allowed to edit",
                    editor.hasPermission(draftVariant.getPath()+ "/foo", Session.ACTION_ADD_NODE));
            assertFalse(editor.hasPermission(draftVariant.getPath(), JCR_MODIFY_PROPERTIES));
        }  finally {
            author.logout();
        }

    }

    /**
     * validate that editor/author by default can write to asset / image documents which do not have workflow
     */
    @Test
    public void author_and_editor_do_have_write_access_to_non_publishable_documents() throws Exception {

        final Session author = server.login("author", "author".toCharArray());
        final Session editor = server.login("author", "author".toCharArray());

        for (Session user : new Session[]{author, editor}) {

            try {
                // on folder
                assertTrue(user.hasPermission("/content/gallery/foo", Session.ACTION_READ));
                assertTrue(user.hasPermission("/content/gallery", JCR_READ));

                assertFalse(user.hasPermission("/content/gallery", Session.ACTION_ADD_NODE));
                assertFalse(user.hasPermission("/content/gallery", JCR_MODIFY_PROPERTIES));
                assertFalse(user.hasPermission("/content/gallery/hippos", JCR_MODIFY_PROPERTIES));

                // on handle
                assertFalse(user.hasPermission("/content/gallery/hippos/Hippo.jpg/foo", Session.ACTION_ADD_NODE));
                assertFalse(user.hasPermission("/content/gallery/hippos/Hippo.jpg", JCR_MODIFY_PROPERTIES));

                // on imageset
                assertTrue(user.hasPermission("/content/gallery/hippos/Hippo.jpg/Hippo.jpg/foo", Session.ACTION_ADD_NODE));
                assertTrue(user.hasPermission("/content/gallery/hippos/Hippo.jpg/Hippo.jpg", JCR_MODIFY_PROPERTIES));

                // on hippo:resource
                assertTrue(user.hasPermission("/content/gallery/hippos/Hippo.jpg/Hippo.jpg/hippogallery:thumbnail/foo", Session.ACTION_ADD_NODE));
                assertTrue(user.hasPermission("/content/gallery/hippos/Hippo.jpg/Hippo.jpg/hippogallery:thumbnail", JCR_MODIFY_PROPERTIES));

            } finally {
                user.logout();
            }
        }

    }

    @Test
    public void author_editor_do_not_have_read_access_to_hippo_configuration_modules() throws RepositoryException {
        final Session author = server.login("author", "author".toCharArray());
        final Session editor = server.login("author", "author".toCharArray());

        for (Session user : new Session[]{author, editor}) {
            try {
                assertFalse(user.hasPermission("/hippo:configuration/hippo:modules", Session.ACTION_READ));
            } finally {
                user.logout();
            }
        }
    }

    private final String[] testuserConfig = new String[]{
            "/hippo:configuration/hippo:users/testuser", "hipposys:user",
            "hipposys:password", "testuser",
            "hipposys:securityprovider", "internal"
    };

    @Test
    public void any_user_can_read_hippo_derivatives() throws Exception {

        build(testuserConfig, session);
        session.save();

        final Session author = server.login("author", "author".toCharArray());
        final Session editor = server.login("author", "author".toCharArray());
        final Session testuser = server.login("testuser", "testuser".toCharArray());

        for (Session user : new Session[]{author, editor, testuser}) {
            try {
                assertTrue(user.hasPermission("/hippo:configuration/hippo:derivatives", Session.ACTION_READ));
            } finally {
                user.logout();
            }
        }

        session.getNode("/hippo:configuration/hippo:users/testuser").remove();
        session.save();
    }

    // /hippo:configuration gets implicit read access due to a jcr:path constraint.
    @Test
    public void any_user_can_read_hippo_configuration_node() throws Exception {
        build(testuserConfig, session);
        session.save();

        final Session author = server.login("author", "author".toCharArray());
        final Session editor = server.login("author", "author".toCharArray());
        final Session testuser = server.login("testuser", "testuser".toCharArray());

        for (Session user : new Session[]{author, editor, testuser}) {
            try {
                assertTrue(user.hasPermission("/hippo:configuration", Session.ACTION_READ));

                final DomainInfoPrivilege[] privileges =  (DomainInfoPrivilege[])user.getAccessControlManager().getPrivileges("/hippo:configuration");

                // this is quite a specific use case: Although the user can read "/hippo:configuration", (s)he has no
                // privileges on that node, not even read...this is because the read is implicit from a read/write
                // privilege on a descendant node

                assertTrue(privileges.length == 0);

            } finally {
                user.logout();
            }
        }

        session.getNode("/hippo:configuration/hippo:users/testuser").remove();
        session.save();
    }
}
