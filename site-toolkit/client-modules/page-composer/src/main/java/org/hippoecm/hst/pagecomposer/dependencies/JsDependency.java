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
package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.LinkedList;
import java.util.List;

/**
 * Javascript file dependency. Can be configured to have multiple debug sources and only a single default source.
 * 
 * @version $Id$
 */
public class JsDependency extends Dependency {

    private List<JsDependency> devDependencies;

    /**
     * Construct a javascript dependency to a file
     * @param file
     */
    public JsDependency(String file) {
        super(file);
    }

    /**
     * Construct a javascript dependency to a file with multiple debug files
     * @param file
     * @param debugFiles
     */
    public JsDependency(String file, String... debugFiles) {
        super(file);
        devDependencies = new LinkedList<JsDependency>();
        for (String devPath : debugFiles) {
            devDependencies.add(new JsDependency(devPath));
        }
    }

    @Override
    protected void addSelf(List<Dependency> all, boolean devMode) {
        if (devMode && devDependencies != null) {
            for (Dependency dependency : devDependencies) {
                dependency.setParent(getParent());
                all.add(dependency);
            }
        } else {
            super.addSelf(all, devMode);
        }
    }

    @Override
    public String asString(String path) {
        return "<script type=\"text/javascript\" src=\"" + path + "\"></script>";
    }

}
