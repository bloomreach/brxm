/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.beanwriter.rest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.services.ContentBeansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeanWriterResourceTest  {
    private static final Logger log = LoggerFactory.getLogger(BeanWriterResourceTest.class);




    @Test
    public void testRunBeanWriter() throws Exception {


        final String namespacePrefix  ="appstore";


        final DefaultPluginContext ctx = new DefaultPluginContext(new PluginRestful("test"));
        ctx.setProjectNamespacePrefix("appstore");
        final ContentBeansService contentBeansService = new ContentBeansService(ctx);
        contentBeansService.createBeans();


    }



    private Session getHippoSession() throws RepositoryException {
        final HippoRepository hippoRepository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        return hippoRepository.login("admin", "admin".toCharArray());

    }
}