/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.container.HstContainerConfigImpl;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;

public class AbstractPipelineTestCase extends AbstractSpringTestCase {

    protected HstComponentFactory componentFactory;
    protected Pipelines pipelines;
    protected Pipeline defaultPipeline;
    protected ServletConfig servletConfig;
    protected HstContainerConfig requestContainerConfig;


    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.componentFactory = (HstComponentFactory) getComponent(HstComponentFactory.class.getName());
        this.pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        this.defaultPipeline = this.pipelines.getDefaultPipeline();
        this.servletConfig = (ServletConfig) getComponent(ServletConfig.class.getName());
        this.requestContainerConfig = new HstContainerConfigImpl(this.servletConfig.getServletContext(), getClass().getClassLoader());
    }

}
