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
package org.hippoecm.hst.pagecomposer.dependencies.ext;

import org.hippoecm.hst.pagecomposer.dependencies.CssDependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsScriptDependency;
import org.hippoecm.hst.pagecomposer.dependencies.VersionedDependency;

/**
 * Dependency for core EXT sources.
 *
 * @version $Id$
 */
public class ExtCore extends VersionedDependency {

    public ExtCore() {
        this("3.3.0");
    }

    public ExtCore(String version) {
        super("lib/ext/core", version);

        addDependency(new CssDependency("resources/css/ext-all.css"));
        addDependency(new JsDependency("adapter/ext/ext-base.js", "adapter/ext/ext-base-debug.js"));
        addDependency(new JsDependency("ext-all.js", "ext-all-debug-w-comments.js"));
        addDependency(new JsScriptDependency("resources/images/default/s.gif") {

            @Override
            protected String getScript(String path) {
                return "Ext.BLANK_IMAGE_URL = '" + path + "';";
            }
        });
    }
}
