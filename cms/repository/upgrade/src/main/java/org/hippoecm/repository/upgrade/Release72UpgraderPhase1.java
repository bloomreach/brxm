/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.upgrade;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class Release72UpgraderPhase1 implements UpdaterModule {
    public void register(final UpdaterContext context) {
        context.registerName("upgrade-pre");
        context.registerStartTag("m13");
        context.registerEndTag("v20902-phase1");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippo:derived") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasProperty("hippo:related")) {
                    node.getProperty("hippo:related").remove();
                }
                if (node.hasProperty("hippo:compute")) {
                    node.getProperty("hippo:compute").remove();
                }
            }
        }.setAtomic());
    }
}
