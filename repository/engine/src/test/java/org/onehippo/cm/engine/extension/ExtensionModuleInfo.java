/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.extension;

import org.onehippo.cm.engine.autoexport.ModuleInfo;

import static org.onehippo.cm.engine.extension.ExtensionIntegrationTest.EXTENSIONS_INTEGRATION_TEST;

public class ExtensionModuleInfo extends ModuleInfo {

    public ExtensionModuleInfo(final String fixtureName) {
        super(fixtureName);
    }

    public ExtensionModuleInfo(final String fixtureName, final String moduleName) {
        super(fixtureName, moduleName);
    }

    public ExtensionModuleInfo(final String fixtureName, final String moduleName, final String inName, final String outName) {
        super(fixtureName, moduleName, inName, outName);
    }

    @Override
    protected String getFixtureRootFolder() {
        return EXTENSIONS_INTEGRATION_TEST;
    }
}
