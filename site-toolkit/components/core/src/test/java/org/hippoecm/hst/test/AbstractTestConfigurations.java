/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.JcrUtils;

public class AbstractTestConfigurations extends AbstractSpringTestCase {

    @Override
    protected String[] getConfigurations() {
        String classXmlFileName = AbstractTestConfigurations.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractTestConfigurations.class.getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }


    protected void createHstConfigBackup(Session session) throws RepositoryException {
        if (!session.nodeExists("/hst-backup")) {
            JcrUtils.copy(session, "/hst:hst", "/hst-backup");
            session.save();
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
    }

    protected Session createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

}
