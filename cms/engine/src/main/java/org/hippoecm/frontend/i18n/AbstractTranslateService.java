/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.i18n;

import java.util.Map;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ITranslateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTranslateService implements IModelProvider<IModel>, ITranslateService {

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(AbstractTranslateService.class);

    public AbstractTranslateService(IPluginContext context, IPluginConfig config) {
        if (config != null && config.getString(ITranslateService.TRANSLATOR_ID) != null) {
            context.registerService(this, config.getString(ITranslateService.TRANSLATOR_ID));
        }
    }

    public String translate(Map<String, String> criteria) {
        IModel model = getModel(criteria);
        if (model != null) {
            return (String) model.getObject();
        }
        return null;
    }

}
