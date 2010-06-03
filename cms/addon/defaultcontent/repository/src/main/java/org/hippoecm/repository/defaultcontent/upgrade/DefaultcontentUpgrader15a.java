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
package org.hippoecm.repository.defaultcontent.upgrade;


import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultcontentUpgrader15a implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DefaultcontentUpgrader15a.class);
    
    public void register(final UpdaterContext context) {
        context.registerName("default-content-upgrade-v15a");
        context.registerStartTag("v12a-defaultcontent");
        context.registerEndTag("v15a-defaultcontent");
        context.registerAfter("upgrade-v13a");
    }
}
