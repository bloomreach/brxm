/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.model;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.scxml2.io.SCXMLReader.Configuration;
import org.apache.commons.scxml2.model.CustomAction;
import org.apache.commons.scxml2.model.SCXML;
import org.junit.Before;
import org.onehippo.repository.scxml.SCXMLUtils;

/**
 * AbstractCurrentFullWorkflowModelTest
 */
public abstract class AbstractCurrentFullWorkflowModelTest {

    protected SCXML scxml;

    @Before
    public void setUp() throws Exception {
        List<CustomAction> customActions = new LinkedList<>();

        customActions.add(new CustomAction("http://www.example.com/scxml/test", "beanprop", BeanPropSetAction.class));
        customActions.add(new CustomAction("http://www.example.com/scxml/test", "variantcopy", VariantCopyAction.class));

        Configuration configuration = new Configuration(null, null, customActions);
        scxml = SCXMLUtils.loadSCXML(getClass().getResource("current-full-workflow-parallel-model.scxml.xml"), configuration);
    }

}
