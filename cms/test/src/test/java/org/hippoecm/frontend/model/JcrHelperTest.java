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
package org.hippoecm.frontend.model;

import static org.junit.Assert.assertEquals;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

public class JcrHelperTest extends PluginTest {

    @Test
    public void primaryItemIsFound() throws RepositoryException {
        Node addNode = root.addNode("aap", "frontendtest:baseprimary");
        addNode.setProperty("frontendtest:primary", "noot");
        Item property = JcrHelper.getPrimaryItem(addNode);
        assertEquals("frontendtest:primary", property.getName());
    }

    @Test
    public void inheritedPrimaryItemIsFound() throws RepositoryException {
        Node addNode = root.addNode("aap", "frontendtest:subprimary");
        addNode.setProperty("frontendtest:primary", "noot");
        Item property = JcrHelper.getPrimaryItem(addNode);
        assertEquals("frontendtest:primary", property.getName());
    }
}
