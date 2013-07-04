/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.IModel;

/**
 * The browser service provides clients (plugins) the means to select a model in
 * "the browser".  Client plugins can use this service to display the model.
 *
 * @param <T> type of models supported by the service
 */
public interface IBrowseService<T extends IModel> extends IClusterable {

    String BROWSER_ID = "browser.id";

    void browse(T model);
}
