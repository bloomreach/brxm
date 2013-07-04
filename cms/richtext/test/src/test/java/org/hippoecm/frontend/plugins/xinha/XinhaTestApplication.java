/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.xinha;

import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.protocol.http.WebApplication;

public class XinhaTestApplication extends WebApplication {

    @Override
    public RuntimeConfigurationType getConfigurationType() {
        // suppress development mode warning from test output
        return RuntimeConfigurationType.DEPLOYMENT;
    }

    @Override
    public Class<? extends Page> getHomePage() {
        try {
            return (Class<? extends Page>) getClass().getClassLoader().loadClass(getInitParameter("test-page-class"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
