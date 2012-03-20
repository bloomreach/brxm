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
package org.hippoecm.hst.upgrade;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor.NamespaceVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class Upgrader_MicroCndChange_2_1_2 implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: Upgrader2_24_A.java 31706 2011-12-07 12:34:06Z aschrijvers $";

    public void register(final UpdaterContext context) {
        context.registerName("upgrader-microchange_2_1_2");
        context.registerStartTag("hst-2_1_1");
        context.registerEndTag("hst-2_1_2");
        context.registerVisitor(new NamespaceVisitor(context, "hst", getClass().getClassLoader().getResourceAsStream("hst-types.cnd")));
    }
}
