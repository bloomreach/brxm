/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.repository.api.StringCodecFactory;

/**
 * The settings service provides plugin access to global settings and factories.
 */
public interface ISettingsService extends IClusterable {

    String SERVICE_ID = "service.settings";

    StringCodecFactory getStringCodecFactory();
}
