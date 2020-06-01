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
package org.onehippo.cm.engine.site;

import org.onehippo.cm.engine.autoexport.ModuleInfo;

import static org.onehippo.cm.engine.site.SiteIntegrationTest.SITE_INTEGRATION_TEST;

public class SiteModuleInfo extends ModuleInfo {

    public SiteModuleInfo(final String fixtureName) {
        super(fixtureName);
    }

    public SiteModuleInfo(final String fixtureName, final String moduleName) {
        super(fixtureName, moduleName);
    }

    public SiteModuleInfo(final String fixtureName, final String moduleName, final String inName, final String outName) {
        super(fixtureName, moduleName, inName, outName);
    }

    @Override
    protected String getFixtureRootFolder() {
        return SITE_INTEGRATION_TEST;
    }
}
