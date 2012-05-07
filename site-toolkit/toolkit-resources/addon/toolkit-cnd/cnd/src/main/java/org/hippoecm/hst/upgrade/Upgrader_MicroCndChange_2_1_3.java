/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.upgrade;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterItemVisitor.NamespaceVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class Upgrader_MicroCndChange_2_1_3 implements UpdaterModule {

    public void register(final UpdaterContext context) {
        context.registerName("upgrader-microchange_2_1_3");
        context.registerStartTag("hst-2_1_2");
        context.registerEndTag("hst-2_1_3");
        // delete the 'hst' init node to make sure the compatible hst namespcae gets reloaded
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize"){
            @Override
            protected void leaving(final Node node, final int level) throws RepositoryException {
                if (node.hasNode("hst")) {
                    node.getNode("hst").remove();
                }
            }
        });
    }
}
