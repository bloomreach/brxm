/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader12c implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(Upgrader12c.class);

    public void register(final UpdaterContext context) {
        context.registerName("upgrade-v12c");
        context.registerStartTag("v12b");
        context.registerEndTag("v12c");

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor(
                "/hippo:configuration/hippo:frontend/cms/cms-reports/todoReport") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                if (node.hasNode("reporting:listener")) {
                    node.getNode("reporting:listener").setProperty("frontend:nodeTypes",
                            new String[] { "hippostdpubwf:request" });
                }
                if (node.hasNode("reporting:query")) {
                    Property prop = node.getProperty("reporting:query/jcr:statement");
                    if ("SELECT * FROM hippo:request".equals(prop.getString())) {
                        prop.setValue("SELECT * FROM hippostdpubwf:request");
                    }
                }
            }
        });
    }

}
