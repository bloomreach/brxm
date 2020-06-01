/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.request;

import javax.jcr.Credentials;

import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CustomContextCredentialsProvider
 */
public class CustomContextCredentialsProvider implements ContextCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomContextCredentialsProvider.class);

    protected Credentials defaultCredentials;
    protected Credentials customCredentials;
    protected Credentials defaultCredentialsForPreviewMode;
    protected Credentials writableCredentials;


    public CustomContextCredentialsProvider(final Credentials defaultCredentials,
                                            final Credentials customCredentials,
                                             final Credentials defaultCredentialsForPreviewMode,
                                             final Credentials writableCredentials) {
        this.defaultCredentials = defaultCredentials;
        this.customCredentials = customCredentials;
        this.defaultCredentialsForPreviewMode = defaultCredentialsForPreviewMode;
        this.writableCredentials = writableCredentials;
    }

    public Credentials getDefaultCredentials(HstRequestContext requestContext) {
        if (defaultCredentialsForPreviewMode != null && requestContext.isPreview()) {
            return defaultCredentialsForPreviewMode;
        }
        if (requestContext.getResolvedMount() != null && requestContext.getResolvedMount().getMount().getName().equals("demosite_fr")) {
            log.info("Using custom pool {}", customCredentials);
            return customCredentials;
        }
        return defaultCredentials;
    }

    public Credentials getWritableCredentials(HstRequestContext requestContext) {
        return writableCredentials;
    }

}

