/*
 *  Copyright 2010 Hippo.
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

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Upgrader19a implements UpdaterModule {

    static final Logger log = LoggerFactory.getLogger(Upgrader19a.class);

    public void register(final UpdaterContext context) {
        context.registerName("repository-upgrade-v19a");
        context.registerStartTag("v18a");
        context.registerEndTag("v19a");

        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippo", getClass()
                .getClassLoader().getResourceAsStream("hippo.cnd")));
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "hippostd", getClass()
                .getClassLoader().getResourceAsStream("hippostd.cnd")));
    }

}
