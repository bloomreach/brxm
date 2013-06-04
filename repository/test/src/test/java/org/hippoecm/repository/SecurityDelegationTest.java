/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoSession;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class SecurityDelegationTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        if (!users.hasNode("ion")) {
            final Node ion = users.addNode("ion", "hipposys:user");
            ion.setProperty("hipposys:password", "delphy");
            final Node apollo = users.addNode("apollo", "hipposys:user");
            apollo.setProperty("hipposys:password", "olympus");
        }

        final Node root = session.getRootNode();
        if (!root.hasNode("athens")) {
            final Node athens = root.addNode("athens");
            final Node assembly = athens.addNode("assembly", "hippo:authtestdocument");
            assembly.setProperty("from", "athens");
            assembly.setProperty("species", "human");
            final Node academy = athens.addNode("academy", "hippo:authtestdocument");
            academy.setProperty("passion", "wisdom");
        }

        final Node domains = session.getNode("/hippo:configuration/hippo:domains");
        if (!domains.hasNode("apollosdomain")) {
            // apollo has access to greek institutions such as the assembly because
            // apollo is athenian, ion has not
            final Node apollosdomain = domains.addNode("apollosdomain", "hipposys:domain");
            final Node institutions = apollosdomain.addNode("institutions", "hipposys:domainrule");
            final Node includeAssembly = institutions.addNode("include-assembly", "hipposys:facetrule");
            includeAssembly.setProperty("hipposys:equals", true);
            includeAssembly.setProperty("hipposys:facet", "from");
            includeAssembly.setProperty("hipposys:type", "String");
            includeAssembly.setProperty("hipposys:value", "athens");
            final Node apolloisadmin = apollosdomain.addNode("apolloisadmin", "hipposys:authrole");
            apolloisadmin.setProperty("hipposys:users", new String[]{"apollo"});
            apolloisadmin.setProperty("hipposys:role", "admin");
        }
        if (!domains.hasNode("ionsdomain")) {
            final Node ionsdomain = domains.addNode("ionsdomain", "hipposys:domain");
            final Node ionisadmin = ionsdomain.addNode("ionisadmin", "hipposys:authrole");
            ionisadmin.setProperty("hipposys:users", new String[]{"ion"});
            ionisadmin.setProperty("hipposys:role", "admin");
        }

        session.save();
    }

    /**
     * Sanity test that configuration setup is correct
     */
    @Test
    public void apolloCanAccessTheAssemblyButIonCannot() throws Exception {
        final Session apollo = session.getRepository().login(new SimpleCredentials("apollo", "olympus".toCharArray()));
        final Node assembly = apollo.getNode("/athens/assembly");
        assembly.setProperty("members", "apollo, etc.");
        apollo.save();

        final Session ion = session.getRepository().login(new SimpleCredentials("ion", "delphy".toCharArray()));
        assertFalse(ion.nodeExists("/athens/assembly"));
    }

    @Test
    public void apolloDelegatesAssemblyAccessToIon() throws Exception {
        final HippoSession ion = (HippoSession) session.getRepository().login(new SimpleCredentials("ion", "delphy".toCharArray()));
        final Session apollo = session.getRepository().login(new SimpleCredentials("apollo", "olympus".toCharArray()));
        final Session sonOfApollo = ion.createSecurityDelegate(apollo);

        // because ion turns out to be the son of apollo he is of athenian descent and can join the assembly
        assertTrue(sonOfApollo.nodeExists("/athens/assembly"));
        final Node assembly = sonOfApollo.getNode("/athens/assembly");
        assembly.setProperty("members", "ion, etc.");
        sonOfApollo.save();
    }

    @Test
    public void ionCanAccessTheAcademyByProgrammaticDomainRuleExtension() throws Exception {

        // include /athens/academy to ions domain by adding a facet that matches the passion property of the academy node
        final FacetRule facetRule = new FacetRule("passion", "wisdom", true, false, PropertyType.STRING);
        final DomainRuleExtension domainRuleExtension = new DomainRuleExtension("ionsdomain", "schools", Arrays.asList(facetRule));

        final HippoSession ion = (HippoSession) session.getRepository().login(new SimpleCredentials("ion", "delphy".toCharArray()));
        final Session apollo = session.getRepository().login(new SimpleCredentials("apollo", "olympus".toCharArray()));

        final Session sonOfApolloAndPhilosopher = ion.createSecurityDelegate(apollo, domainRuleExtension);
        final Node academy = sonOfApolloAndPhilosopher.getNode("/athens/academy");
        academy.setProperty("members", "ion, etc.");
        sonOfApolloAndPhilosopher.save();
    }

    @Test
    public void apolloCanBeRevokedAssemblyAccessByProgrammaticDomainRuleExtension() throws Exception {

        // exclude /athens/assembly from apollos domain by adding a facet rule to the existing institutions domain rule
        // that contradicts the species property on the assembly node
        final FacetRule facetRule = new FacetRule("species", "god", true, false, PropertyType.STRING);
        final DomainRuleExtension domainRuleExtension = new DomainRuleExtension("apollosdomain", "institutions", Arrays.asList(facetRule));

        final HippoSession apollo = (HippoSession) session.getRepository().login(new SimpleCredentials("apollo", "olympus".toCharArray()));
        final Session ion = session.getRepository().login(new SimpleCredentials("ion", "delphy".toCharArray()));
        final Session fatherOfIon = apollo.createSecurityDelegate(ion, domainRuleExtension);

        assertFalse(fatherOfIon.nodeExists("/athens/assembly"));
    }
}
