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
package org.hippoecm.frontend.service.render;

import java.io.Serializable;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.sa.core.PluginContext;
import org.hippoecm.frontend.service.Message;
import org.hippoecm.frontend.service.topic.MessageListener;
import org.hippoecm.frontend.service.topic.TopicService;

public class ProviderReference implements Serializable, MessageListener {
    private static final long serialVersionUID = 1L;

    public static final int SET_PROVIDER = 1;
    public static final int GET_PROVIDER = 2;

    public interface IView {

        public IDataProvider getDataProvider();

        void updateDataProvider(IDataProvider provider);
    }

    public static class ProviderMessage extends Message {
        private static final long serialVersionUID = 1L;

        private IDataProvider provider;

        public ProviderMessage(int type, IDataProvider provider) {
            super(type);
            this.provider = provider;
        }

        public IDataProvider getProvider() {
            return provider;
        }
    }

    private TopicService topic;
    private IView view;

    public ProviderReference(String serviceId, IView view) {
        this.view = view;

        this.topic = new TopicService(serviceId);
        topic.addListener(this);
    }

    public void init(PluginContext context) {
        topic.init(context);
        topic.publish(new ProviderMessage(GET_PROVIDER, null));
    }

    public void destroy() {
        topic.destroy();
    }

    public void setDataProvider(IDataProvider provider) {
        topic.publish(new ProviderMessage(SET_PROVIDER, provider));
    }
    
    public void onMessage(Message message) {
        if (message instanceof ProviderMessage) {
            switch (message.getType()) {
            case GET_PROVIDER:
                topic.publish(new ProviderMessage(SET_PROVIDER, view.getDataProvider()));
                break;

            case SET_PROVIDER:
                IDataProvider newProvider = ((ProviderMessage) message).getProvider();
                IDataProvider oldProvider = view.getDataProvider();
                if (newProvider == null) {
                    if (oldProvider != null) {
                        view.updateDataProvider(newProvider);
                    }
                } else {
                    if (!newProvider.equals(oldProvider)) {
                        view.updateDataProvider(newProvider);
                    }
                }
                break;
            }
        }
    }

}
