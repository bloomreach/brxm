/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.yui.logger;

import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.wicketstuff.yui.YuiHeaderContributor;

public class YuiLogBehavior extends AbstractHeaderContributor {
    private static final long serialVersionUID = 1L;

    @Override
    public IHeaderContributor[] getHeaderContributors() {
        IHeaderContributor[] dependencies = YuiHeaderContributor.forModule("logger").getHeaderContributors();
        IHeaderContributor[] logger = new IHeaderContributor[dependencies.length + 1];
        System.arraycopy(dependencies, 0, logger, 0, dependencies.length);
        logger[logger.length - 1] = HeaderContributor.forJavaScript(YuiLogBehavior.class, "YuiLogBehavior.js");
        return logger;
    }

}
