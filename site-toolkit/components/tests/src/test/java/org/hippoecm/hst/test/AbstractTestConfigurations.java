/*
 *  Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.test;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.api.model.EventPathsInvalidator;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractTestConfigurations extends AbstractSpringTestCase {

    protected HstModelProvider provider;
    protected EventPathsInvalidator invalidator;

    /**
     * addAnnotatedClassesConfigurationParam must be added before super setUpClass, hence redefine same setUpClass method
     * to hide the super.setUpClass and invoke that explicitly
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        String classXmlFileName = AbstractTestConfigurations.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractTestConfigurations.class.getName().replace(".", "/") + "-*.xml";

        AbstractSpringTestCase.addAnnotatedClassesConfigurationParam(classXmlFileName);
        AbstractSpringTestCase.addAnnotatedClassesConfigurationParam(classXmlFileName2);

        AbstractSpringTestCase.setUpClass();
    }


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        provider = HstServices.getComponentManager().getComponent(HstModelProvider.class);
        invalidator = ((InternalHstModel) provider.getHstModel()).getEventPathsInvalidator();
    }

    protected void createHstConfigBackup(Session session) throws RepositoryException {
        if (!session.nodeExists("/hst-backup")) {
            JcrUtils.copy(session, "/hst:hst", "/hst-backup");
            session.save();
        }

        if (session.nodeExists("/hst:site2")) {
            if (!session.nodeExists("/hst-backup2")) {
                JcrUtils.copy(session, "/hst:site2", "/hst-backup2");
                session.save();
            }
        }
    }

    protected void restoreHstConfigBackup(Session session) throws RepositoryException {
        if (session.nodeExists("/hst-backup")) {
            if (session.nodeExists("/hst:hst")) {
                session.removeItem("/hst:hst");
            }
            JcrUtils.copy(session, "/hst-backup", "/hst:hst");
            session.removeItem("/hst-backup");
            session.save();
        }

        if (session.nodeExists("/hst-backup2")) {
            if (session.nodeExists("/hst:site2")) {
                session.removeItem("/hst:site2");
            }
            JcrUtils.copy(session, "/hst-backup2", "/hst:site2");
            session.removeItem("/hst-backup2");
            session.save();
        }
    }

    protected Session createSession() throws RepositoryException {
        return getRepository().login(getAdminCredentials());
    }

    protected Credentials getAdminCredentials() {
        return new SimpleCredentials("admin", "admin".toCharArray());
    }

    protected Repository getRepository() {
        return HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
    }

}
