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
package org.hippoecm.frontend.sa;

import java.util.List;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.model.IJcrNodeModelListener;
import org.hippoecm.frontend.sa.plugin.impl.PluginManager;
import org.hippoecm.frontend.sa.service.IJcrService;

public class JcrService implements IJcrService {
    private static final long serialVersionUID = 1L;

    private PluginManager mgr;
    
    JcrService(PluginManager mgr) {
        this.mgr = mgr;
    }

    public void flush(JcrNodeModel model) {
        List<IJcrNodeModelListener> listeners = mgr.getServices(IJcrService.class.getName(),
                IJcrNodeModelListener.class);
        for (IJcrNodeModelListener listener : listeners) {
            listener.onFlush(model);
        }
    }

}
