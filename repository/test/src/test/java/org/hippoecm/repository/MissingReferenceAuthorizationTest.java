/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.security.AccessControlException;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MissingReferenceAuthorizationTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");

        if (!users.hasNode("oneTrueMissingReferenceUser")) {
            final Node user = users.addNode("oneTrueMissingReferenceUser", "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("oneFalseMissingReferenceUser")) {
            final Node user = users.addNode("oneFalseMissingReferenceUser", "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("combinedTrueMissingReferenceUser")) {
            final Node user = users.addNode("combinedTrueMissingReferenceUser", "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("combinedFalseMissingReferenceUser")) {
            final Node user = users.addNode("combinedFalseMissingReferenceUser", "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("onlyTrueMissingReferencesUser")) {
            final Node user = users.addNode("onlyTrueMissingReferencesUser", "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("onlyFalseMissingReferencesUser")) {
            final Node user = users.addNode("onlyFalseMissingReferencesUser", "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }

        if (!users.hasNode("combinedFalseMissingUUIDReferenceUser")) {
            final Node user = users.addNode("combinedFalseMissingUUIDReferenceUser", "hipposys:user");
            user.setProperty("hipposys:password", "password");
        }


        final Node root = session.getRootNode();
        if (!root.hasNode("test")) {
            final Node test = root.addNode("test");
            final Node folder = test.addNode("folder", "hippostd:folder");
            folder.setProperty("hippostd:foldertype", new String[]{"foo", "bar"});
            final Node authDocument = folder.addNode("authDocument", "hippo:authtestdocument");
            authDocument.setProperty("authDocumentProp", "foo");
            final Node compound = authDocument.addNode("compound", "hippo:authtestdocument");
            compound.setProperty("compoundProp", "bar");
            final Node testDocument = folder.addNode("testDocument", "hippo:testdocument");
            testDocument.setProperty("testDocumentProp", "lux");
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");

        if (!domains.hasNode("oneTrueMissingReferenceDomain")) {
            final Node oneTrueMissingReferenceDomain = domains.addNode("oneTrueMissingReferenceDomain", "hipposys:domain");
            {
                final Node domainRule = oneTrueMissingReferenceDomain.addNode("domain-with-one-true-missing-reference-in-one-facetrule", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("non-existing-path-rule", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", true);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder/non/existing");
            }

            final Node adminRole = oneTrueMissingReferenceDomain.addNode("oneTrueMissingReferenceUser", "hipposys:authrole");
            adminRole.setProperty("hipposys:users", new String[]{"oneTrueMissingReferenceUser"});
            adminRole.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("oneFalseMissingReferenceDomain")) {
            final Node oneFalseMissingReferenceDomain = domains.addNode("oneFalseMissingReferenceDomain", "hipposys:domain");
            {
                final Node domainRule = oneFalseMissingReferenceDomain.addNode("domain-with-one-false-missing-reference-in-one-facetrule", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("non-existing-path-rule", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", false);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder/non/existing");
            }

            final Node adminRole = oneFalseMissingReferenceDomain.addNode("oneFalseMissingReferenceUser", "hipposys:authrole");
            adminRole.setProperty("hipposys:users", new String[]{"oneFalseMissingReferenceUser"});
            adminRole.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("combinedTrueMissingReferenceDomain")) {
            final Node combinedTrueMissingReferenceDomain = domains.addNode("combinedTrueMissingReferenceDomain", "hipposys:domain");
            {
                final Node domainRule = combinedTrueMissingReferenceDomain.addNode("domain-with-one-true-missing-reference-in-one-facetrule", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("path-rule", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", true);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder");

                final Node facetRule2 = domainRule.addNode("non-existing-path-rule", "hipposys:facetrule");
                facetRule2.setProperty("hipposys:equals", true);
                facetRule2.setProperty("hipposys:facet", "jcr:path");
                facetRule2.setProperty("hipposys:type", "Reference");
                facetRule2.setProperty("hipposys:value", "/test/folder/non/existing");
            }

            final Node adminRole = combinedTrueMissingReferenceDomain.addNode("combinedTrueMissingReferenceUser", "hipposys:authrole");
            adminRole.setProperty("hipposys:users", new String[]{"combinedTrueMissingReferenceUser"});
            adminRole.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("combinedFalseMissingReferenceDomain")) {
            final Node combinedFalseMissingReferenceDomain = domains.addNode("combinedFalseMissingReferenceDomain", "hipposys:domain");
            {
                final Node domainRule = combinedFalseMissingReferenceDomain.addNode("domain-with-one-false-missing-reference-in-one-facetrule", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("path-rule", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", true);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder");

                final Node facetRule2 = domainRule.addNode("non-existing-path-rule", "hipposys:facetrule");
                facetRule2.setProperty("hipposys:equals", false);
                facetRule2.setProperty("hipposys:facet", "jcr:path");
                facetRule2.setProperty("hipposys:type", "Reference");
                facetRule2.setProperty("hipposys:value", "/test/folder/non/existing");
            }

            final Node adminRole = combinedFalseMissingReferenceDomain.addNode("combinedFalseMissingReferenceUser", "hipposys:authrole");
            adminRole.setProperty("hipposys:users", new String[]{"combinedFalseMissingReferenceUser"});
            adminRole.setProperty("hipposys:role", "admin");
        }


        if (!domains.hasNode("onlyTrueMissingReferencesDomain")) {
            final Node onlyTrueMissingReferencesDomain = domains.addNode("onlyTrueMissingReferencesDomain", "hipposys:domain");
            {
                final Node domainRule = onlyTrueMissingReferencesDomain.addNode("domain-with-only-true-missing-references-facetrules", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("non-existing-path-rule1", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", true);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder/non/existing/one");

                final Node facetRule2 = domainRule.addNode("non-existing-path-rule2", "hipposys:facetrule");
                facetRule2.setProperty("hipposys:equals", true);
                facetRule2.setProperty("hipposys:facet", "jcr:path");
                facetRule2.setProperty("hipposys:type", "Reference");
                facetRule2.setProperty("hipposys:value", "/test/folder/non/existing/two");
            }

            final Node adminRole = onlyTrueMissingReferencesDomain.addNode("onlyTrueMissingReferencesUser", "hipposys:authrole");
            adminRole.setProperty("hipposys:users", new String[]{"onlyTrueMissingReferencesUser"});
            adminRole.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("onlyFalseMissingReferencesDomain")) {
            final Node onlyFalseMissingReferencesDomain = domains.addNode("onlyFalseMissingReferencesDomain", "hipposys:domain");
            {
                final Node domainRule = onlyFalseMissingReferencesDomain.addNode("domain-with-only-false-missing-references-facetrules", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("non-existing-path-rule1", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", false);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder/non/existing/one");

                final Node facetRule2 = domainRule.addNode("non-existing-path-rule2", "hipposys:facetrule");
                facetRule2.setProperty("hipposys:equals", false);
                facetRule2.setProperty("hipposys:facet", "jcr:path");
                facetRule2.setProperty("hipposys:type", "Reference");
                facetRule2.setProperty("hipposys:value", "/test/folder/non/existing/two");
            }

            final Node adminRole = onlyFalseMissingReferencesDomain.addNode("onlyFalseMissingReferencesUser", "hipposys:authrole");
            adminRole.setProperty("hipposys:users", new String[]{"onlyFalseMissingReferencesUser"});
            adminRole.setProperty("hipposys:role", "admin");
        }

        if (!domains.hasNode("combinedFalseMissingUUIDReferenceDomain")) {
            final Node combinedFalseMissingReferenceDomain = domains.addNode("combinedFalseMissingUUIDReferenceDomain", "hipposys:domain");
            {
                final Node domainRule = combinedFalseMissingReferenceDomain.addNode("domain-with-one-false-missing-reference-in-one-facetrule", "hipposys:domainrule");
                final Node facetRule = domainRule.addNode("path-rule", "hipposys:facetrule");
                facetRule.setProperty("hipposys:equals", true);
                facetRule.setProperty("hipposys:facet", "jcr:path");
                facetRule.setProperty("hipposys:type", "Reference");
                facetRule.setProperty("hipposys:value", "/test/folder");

                final Node facetRule2 = domainRule.addNode("non-existing-path-rule", "hipposys:facetrule");
                facetRule2.setProperty("hipposys:equals", false);
                // INSTEAD OF jcr:path now jcr:uuid !!!!!!!!!!!!!!!!!!!!!!!!!!!!
                facetRule2.setProperty("hipposys:facet", "jcr:uuid");
                facetRule2.setProperty("hipposys:type", "Reference");
                facetRule2.setProperty("hipposys:value", "/test/folder/non/existing");
            }

            final Node adminRole = combinedFalseMissingReferenceDomain.addNode("combinedFalseMissingUUIDReferenceUser", "hipposys:authrole");
            adminRole.setProperty("hipposys:users", new String[]{"combinedFalseMissingUUIDReferenceUser"});
            adminRole.setProperty("hipposys:role", "admin");
        }

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");

        if (users.hasNode("oneTrueMissingReferenceUser")) {
            users.getNode("oneTrueMissingReferenceUser").remove();
        }
        if (users.hasNode("oneFalseMissingReferenceUser")) {
            users.getNode("oneFalseMissingReferenceUser").remove();
        }
        if (users.hasNode("combinedTrueMissingReferenceUser")) {
            users.getNode("combinedTrueMissingReferenceUser").remove();
        }
        if (users.hasNode("combinedFalseMissingReferenceUser")) {
            users.getNode("combinedFalseMissingReferenceUser").remove();
        }
        if (users.hasNode("onlyTrueMissingReferencesUser")) {
            users.getNode("onlyTrueMissingReferencesUser").remove();
        }
        if (users.hasNode("onlyFalseMissingReferencesUser")) {
            users.getNode("onlyFalseMissingReferencesUser").remove();
        }
        if (users.hasNode("combinedFalseMissingUUIDReferenceUser")) {
            users.getNode("combinedFalseMissingUUIDReferenceUser").remove();
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        if (domains.hasNode("oneTrueMissingReferenceDomain")) {
            domains.getNode("oneTrueMissingReferenceDomain").remove();
        }
        if (domains.hasNode("oneFalseMissingReferenceDomain")) {
            domains.getNode("oneFalseMissingReferenceDomain").remove();
        }
        if (domains.hasNode("combinedTrueMissingReferenceDomain")) {
            domains.getNode("combinedTrueMissingReferenceDomain").remove();
        }
        if (domains.hasNode("combinedFalseMissingReferenceDomain")) {
            domains.getNode("combinedFalseMissingReferenceDomain").remove();
        }
        if (domains.hasNode("onlyTrueMissingReferencesDomain")) {
            domains.getNode("onlyTrueMissingReferencesDomain").remove();
        }
        if (domains.hasNode("onlyFalseMissingReferencesDomain")) {
            domains.getNode("onlyFalseMissingReferencesDomain").remove();
        }
        if (domains.hasNode("combinedFalseMissingUUIDReferenceDomain")) {
            domains.getNode("combinedFalseMissingUUIDReferenceDomain").remove();
        }

        session.save();
        super.tearDown();
    }


    @Test(expected = AccessControlException.class)
    public void one_true_missing_reference_in_facet_rule_domain_rule() throws Exception {
        // oneTrueMissingReferenceUser has domain with single facet rule that has equals = true & non existing reference
        // should result in no read access below /test/folder
        Session oneTrueMissingReferenceUser = null;
        try {
            final Credentials creds = new SimpleCredentials("oneTrueMissingReferenceUser", "password".toCharArray());
            oneTrueMissingReferenceUser = server.login(creds);
            assertTrue(oneTrueMissingReferenceUser.nodeExists("/test"));
            assertFalse(oneTrueMissingReferenceUser.nodeExists("/test/folder"));
            oneTrueMissingReferenceUser.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (oneTrueMissingReferenceUser != null) {
                oneTrueMissingReferenceUser.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void one_false_missing_reference_in_facet_rule_domain_rule() throws Exception {

        // oneFalseMissingReferenceUser has domain with single facet rule that has equals = false & non existing reference
        // should result in no read access below /test/folder
        Session oneFalseMissingReferenceUser = null;
        try {
            final Credentials creds = new SimpleCredentials("oneFalseMissingReferenceUser", "password".toCharArray());
            oneFalseMissingReferenceUser = server.login(creds);
            assertTrue(oneFalseMissingReferenceUser.nodeExists("/test"));
            assertFalse(oneFalseMissingReferenceUser.nodeExists("/test/folder"));
            oneFalseMissingReferenceUser.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (oneFalseMissingReferenceUser != null) {
                oneFalseMissingReferenceUser.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void combined_true_missing_reference_in_facet_rules_domain_rule() throws Exception {

        // combinedTrueMissingReferenceUser has domain with two AND-ed facet rules : both have equals = true and one contains
        // non existing reference. Result should be no read access below /test/folder
        Session combinedTrueMissingReferenceUser = null;
        try {
            final Credentials creds = new SimpleCredentials("combinedTrueMissingReferenceUser", "password".toCharArray());
            combinedTrueMissingReferenceUser = server.login(creds);
            assertTrue(combinedTrueMissingReferenceUser.nodeExists("/test"));
            assertFalse(combinedTrueMissingReferenceUser.nodeExists("/test/folder"));
            combinedTrueMissingReferenceUser.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (combinedTrueMissingReferenceUser != null) {
                combinedTrueMissingReferenceUser.logout();
            }
        }
    }

    @Test
    public void combined_false_missing_reference_in_facet_rules_domain_rule() throws Exception {

        // combinedFalseMissingReferenceUser has domain with two AND-ed facet rules : one rule matches everything below /test/folder
        // and other one has a non-existing reference but has equals = false. Result should be that everything below /test/folder
        // is readable
        Session combinedFalseMissingReferenceUser = null;
        try {
            final Credentials creds = new SimpleCredentials("combinedFalseMissingReferenceUser", "password".toCharArray());
            combinedFalseMissingReferenceUser = server.login(creds);

            assertTrue(combinedFalseMissingReferenceUser.nodeExists("/test"));
            assertTrue(combinedFalseMissingReferenceUser.nodeExists("/test/folder"));
            assertTrue(combinedFalseMissingReferenceUser.nodeExists("/test/folder/authDocument"));
            assertTrue(combinedFalseMissingReferenceUser.nodeExists("/test/folder/authDocument/compound"));
            assertTrue(combinedFalseMissingReferenceUser.nodeExists("/test/folder/testDocument"));

            combinedFalseMissingReferenceUser.checkPermission("/test/folder", "jcr:read");
            combinedFalseMissingReferenceUser.checkPermission("/test/folder", "jcr:write");

            combinedFalseMissingReferenceUser.checkPermission("/test/folder/authDocument", "jcr:read");
            combinedFalseMissingReferenceUser.checkPermission("/test/folder/authDocument", "jcr:write");

            combinedFalseMissingReferenceUser.checkPermission("/test/folder/authDocument/compound", "jcr:read");
            combinedFalseMissingReferenceUser.checkPermission("/test/folder/authDocument/compound", "jcr:write");

            combinedFalseMissingReferenceUser.checkPermission("/test/folder/testDocument", "jcr:read");
            combinedFalseMissingReferenceUser.checkPermission("/test/folder/testDocument", "jcr:write");

        } finally {
            if (combinedFalseMissingReferenceUser != null) {
                combinedFalseMissingReferenceUser.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void only_true_missing_references_in_facet_rules_domain_rule() throws Exception {

        // onlyTrueMissingReferencesDomain has domain with two AND-ed facet rules : both have equals = true and one contains
        // non existing reference. Result should be no read access below /test/folder
        Session onlyTrueMissingReferencesUser = null;
        try {
            final Credentials creds = new SimpleCredentials("onlyTrueMissingReferencesUser", "password".toCharArray());
            onlyTrueMissingReferencesUser = server.login(creds);
            assertTrue(onlyTrueMissingReferencesUser.nodeExists("/test"));
            assertFalse(onlyTrueMissingReferencesUser.nodeExists("/test/folder"));
            onlyTrueMissingReferencesUser.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (onlyTrueMissingReferencesUser != null) {
                onlyTrueMissingReferencesUser.logout();
            }
        }
    }

    @Test(expected = AccessControlException.class)
    public void only_false_missing_references_in_facet_rules_domain_rule() throws Exception {

        // onlyFalseMissingReferencesDomain has domain with two AND-ed facet rules : both have equals = true and one contains
        // non existing reference. Result should be no read access below /test/folder
        Session onlyFalseMissingReferencesUser = null;
        try {
            final Credentials creds = new SimpleCredentials("onlyFalseMissingReferencesUser", "password".toCharArray());
            onlyFalseMissingReferencesUser = server.login(creds);
            assertTrue(onlyFalseMissingReferencesUser.nodeExists("/test"));
            assertFalse(onlyFalseMissingReferencesUser.nodeExists("/test/folder"));
            onlyFalseMissingReferencesUser.checkPermission("/test/folder", "jcr:read");
        } finally {
            if (onlyFalseMissingReferencesUser != null) {
                onlyFalseMissingReferencesUser.logout();
            }
        }
    }

    @Test
    public void combined_false_missing_reference_UUID_in_facet_rules_domain_rule() throws Exception {

        // combinedFalseMissingUUIDReferenceUser has domain with two AND-ed facet rules : one rule matches everything below /test/folder
        // and other one has a non-existing reference but has equals = false and with jcr:uuid. Result should be that everything below /test/folder
        // is readable
        Session combinedFalseMissingUUIDReferenceUser = null;
        try {
            final Credentials creds = new SimpleCredentials("combinedFalseMissingUUIDReferenceUser", "password".toCharArray());
            combinedFalseMissingUUIDReferenceUser = server.login(creds);

            assertTrue(combinedFalseMissingUUIDReferenceUser.nodeExists("/test"));
            assertTrue(combinedFalseMissingUUIDReferenceUser.nodeExists("/test/folder"));
            assertTrue(combinedFalseMissingUUIDReferenceUser.nodeExists("/test/folder/authDocument"));
            assertTrue(combinedFalseMissingUUIDReferenceUser.nodeExists("/test/folder/authDocument/compound"));
            assertTrue(combinedFalseMissingUUIDReferenceUser.nodeExists("/test/folder/testDocument"));

            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder", "jcr:read");
            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder", "jcr:write");

            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder/authDocument", "jcr:read");
            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder/authDocument", "jcr:write");

            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder/authDocument/compound", "jcr:read");
            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder/authDocument/compound", "jcr:write");

            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder/testDocument", "jcr:read");
            combinedFalseMissingUUIDReferenceUser.checkPermission("/test/folder/testDocument", "jcr:write");

        } finally {
            if (combinedFalseMissingUUIDReferenceUser != null) {
                combinedFalseMissingUUIDReferenceUser.logout();
            }
        }
    }

}
