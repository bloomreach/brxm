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

import org.hippoecm.hst.pagecomposer.dependencies.CssDependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;
import org.hippoecm.hst.pagecomposer.dependencies.PathDependency;

/**
 * @version $Id$
 */
public class ExtColorField extends ExtPlugin {
    public ExtColorField(String version) {
        super("colorfield", version);
        addDependency(new JsDependency("colorfield.js", "colorfield.js"));
        addDependency(new PathDependency("css",
                new CssDependency("colorfield.css")
        ));

    }
}
