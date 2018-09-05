/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.junit.Before;
import org.onehippo.cms7.services.HippoServiceRegistry;

public class AbstractHstLinkRewritingIT  extends AbstractBeanTestCase {

    protected ObjectConverter objectConverter;
    protected HstLinkCreator linkCreator;
    protected HstManager hstManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.objectConverter = getObjectConverter();
        // TODO link creator cannot be fetched via spring component any more
        this.linkCreator = HippoServiceRegistry.getService(HstModelRegistry.class).getHstModel("/site").getHstLinkCreator();
        this.hstManager = getComponent(HstManager.class.getName());
        //TODO SS: review this
        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.registerHstModel("/site2", this.getClass().getClassLoader(), componentManager, true);

    }

    protected Session createAdminSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

}
