/*
 * Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugins.brx;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.service.MavenAssemblyService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenCargoService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.WebXmlService;

/**
 * Install the BRX SSO and User Provisioning features (aka: IAM Integration).
 */
public class BrxIamInstruction implements Instruction {

    public static final String FILTER_NAME = "BrxAuthFilter";
    private static final List<String> URL_PATTERNS = Collections.singletonList("/*");

    @Inject private WebXmlService webXmlService;

    @Override
    public Status execute(final Map<String, Object> parameters) {

        // Add filter-mapping to web.xml
        // Note: this relies on a Java annotation to register the filter first, to avoid ordering problems
        webXmlService.insertFilterMapping(Module.CMS, FILTER_NAME, URL_PATTERNS, "HstFilter");

        return Status.SUCCESS;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<Type, String> changeMessageQueue) {
        changeMessageQueue.accept(Type.EXECUTE, "Add new filter to web.xml for BRX IAM Integration.");
    }
}
