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
package org.hippoecm.hst.pagecomposer.dependencies.ext.plugins;

import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;

/**
 * @version $Id$
 */
public class ExtMiframe extends ExtPlugin {

    public ExtMiframe(String version) {
        this(version, true);
    }

    public ExtMiframe(String version, boolean enableMessaging) {
        super("miframe", version);
        addDependency(new JsDependency("miframe.js", "miframe-debug.js"));
        if (enableMessaging) {
            addDependency(new JsDependency("modules/mifmsg.js"));
        }
    }
}
