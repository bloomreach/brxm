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
import org.hippoecm.hst.pagecomposer.dependencies.Dependency;
import org.hippoecm.hst.pagecomposer.dependencies.JsDependency;
import org.hippoecm.hst.pagecomposer.dependencies.PathDependency;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtBaseApp;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtBaseGrid;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtColorField;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtFloatingWindow;
import org.hippoecm.hst.pagecomposer.dependencies.ext.plugins.ExtMiframe;

/**
 * All dependencies used by the Ext part of the PageComposerApp are defined here
 *
 * @version $Id$
 */
public class ExtApp extends PathDependency {

    public ExtApp() {
        super("/hst/pagecomposer/sources");
        addDependency(new PathDependency("js",

                new ExtCore("3.3.0"),
                new ExtMiframe("2.1.5"),
                new ExtFloatingWindow("1.0.0"),
                new ExtBaseApp("1.0.0"),
                new ExtBaseGrid("1.0.0"),
                new ExtColorField("1.0.0"),

                new JsDependency("src/globals.js", "src/globals-debug.js"),
                new PathDependency("src/ext",
                        new JsDependency("PropertiesPanel.js","PropertiesPanel-debug.js"),
                        new JsDependency("PageModel.js","PageModel-debug.js"),
                        new JsDependency("PageEditor.js","PageEditor-debug.js")
                )
        ));
        addDependency(new PathDependency("css",
                new CssDependency("PageEditor.css"),
                new Dependency("hippo-cms.ico") {
                    @Override
                    public String asString(final String path) {
                        return "<link rel=\"shortcut icon\" href=\""+path+"\" type=\"image/x-icon\"/>";
                    }
                }
        ));
    }
}
