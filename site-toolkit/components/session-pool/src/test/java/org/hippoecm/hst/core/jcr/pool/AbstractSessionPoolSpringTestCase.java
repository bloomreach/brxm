/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * AbstractSessionPoolSpringTestCase
 * 
 * @version $Id$
 *  
 */
public abstract class AbstractSessionPoolSpringTestCase
{

    protected final static Logger log = LoggerFactory.getLogger(AbstractSessionPoolSpringTestCase.class);
    protected AbstractRefreshableConfigApplicationContext applicationContext;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }

    @Before
    public void setUp() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.setConfigLocations(getConfigurations());
        applicationContext.refresh();
        applicationContext.start();
    }

    @After
    public void tearDown() throws Exception {
        applicationContext.stop();
        applicationContext.close();
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }
    
    protected <T> T getComponent(String name) {
        return (T) applicationContext.getBean(name);
    }
}
