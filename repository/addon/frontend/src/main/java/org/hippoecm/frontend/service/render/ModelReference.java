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

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.service.topic.Message;
import org.hippoecm.frontend.service.topic.MessageListener;
import org.hippoecm.frontend.service.topic.TopicService;

public class ModelReference<T extends IModel> implements Serializable, MessageListener {
    private static final long serialVersionUID = 1L;

    public static final int GET_MODEL = 1;
    public static final int SET_MODEL = 2;

    public interface IView<T extends IModel> {
        
        T getModel();
        
        void updateModel(T model);
    }
    
    public static class ModelMessage<T extends IModel> extends Message {
        private static final long serialVersionUID = 1L;

        private T model;

        public ModelMessage(int type, T model) {
            super(type);

            this.model = model;
        }

        public T getModel() {
            return model;
        }
    }

    private TopicService topic;
    private IView<T> view;

    public ModelReference(String serviceId, IView<T> view) {
        this.view = view;

        this.topic = new TopicService(serviceId);
        topic.addListener(this);
    }

    public void init(PluginContext context) {
        topic.init(context);
        ModelMessage message = new ModelMessage(GET_MODEL, null);
        message.setSource(topic);
        topic.publish(message);
    }

    public void destroy() {
        topic.destroy();
    }

    public T getModel() {
        return view.getModel();
    }

    public void setModel(T model) {
        topic.publish(new ModelMessage(SET_MODEL, model));
    }

    public void onMessage(Message message) {
        if (message instanceof ModelMessage) {
            switch (message.getType()) {
            case GET_MODEL:
                TopicService source = ((ModelMessage) message).getSource();
                if (source != null) {
                    source.onPublish(new ModelMessage<T>(SET_MODEL, view.getModel()));
                } else {
                    topic.publish(new ModelMessage<T>(SET_MODEL, view.getModel()));
                }
                break;

            case SET_MODEL:
                T newModel = ((ModelMessage<T>) message).getModel();
                T oldModel = view.getModel();
                if (newModel == null) {
                    if (oldModel != null) {
                        view.updateModel(newModel);
                    }
                } else {
                    if (!newModel.equals(oldModel)) {
                        view.updateModel(newModel);
                    }
                }
                break;
            }
        }
    }

}
