/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.jcr.pool;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;

public class TestOCM extends AbstractSpringTestCase {
    
    protected Repository repository;
    protected Credentials defaultCredentials;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.repository = getComponent(Repository.class.getName());
        this.defaultCredentials = (Credentials) getComponent(Credentials.class.getName() + ".default");
    }

    @Test
    public void testOCM() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(ComponentConfiguration.class);
        Mapper mapper = new AnnotationMapperImpl(classes);
        
        Session session = this.repository.login(this.defaultCredentials);
        
        ObjectContentManager ocm = new ObjectContentManagerImpl(session, mapper);
        
        ComponentConfiguration compConfig = (ComponentConfiguration) ocm.getObject("/hst:testconfiguration/hst:configuration/hst:components/pages/newsoverview");
        System.out.println("path: " + compConfig.getPath());
        System.out.println("reference name: " + compConfig.getReferenceName());
        System.out.println("content base path: " + compConfig.getComponentContentBasePath());
        System.out.println("class name: " + compConfig.getComponentClassName());
        System.out.println("render path: " + compConfig.getRenderPath());
        
        session.logout();
    }
}
