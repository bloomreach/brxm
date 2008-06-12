/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.template;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.sa.plugins.standardworkflow.types.TypeDescriptor;

public interface ITemplateEngine extends IClusterable {

    String ENGINE = "engine";

    String TEMPLATE = "template";

    String MODE = "mode";

    String EDIT_MODE = "edit";

    TypeDescriptor getType(String type);

    TypeDescriptor getType(IModel model);

    IClusterConfig getTemplate(TypeDescriptor type, String mode);

}
