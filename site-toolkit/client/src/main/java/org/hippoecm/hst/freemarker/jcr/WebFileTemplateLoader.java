/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker.jcr;

import java.io.IOException;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.util.WebFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebFileTemplateLoader extends AbstractTemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(WebFileTemplateLoader.class);

    public Object findTemplateSource(String templateSource) throws IOException {
        if (templateSource == null || !templateSource.startsWith(ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL)) {
            return null;
        }

        String absPath = WebFileUtils.webFilePathToJcrPath(templateSource);
        log.info("Trying to load freemarker template for web file from '{}'", absPath);

        final RepositorySource repositorySource = getLoadingCache().get(absPath);

        if (repositorySource == null || !repositorySource.isFound()) {
            return null;
        }

        return repositorySource;
    }

}
