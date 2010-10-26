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

/**
 * Css dependency.
 *
 * @version $Id$
 */
public class CssDependency extends Dependency {

    public CssDependency(String path) {
        super(path);
    }

    @Override
    public String asString(String path) {
        return "<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"" + path + "\"/>";
    }
}
