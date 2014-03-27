/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.content.beans.manager.workflow;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.hippoecm.repository.api.Workflow;

public abstract class BaseWorkflowCallbackHandler<T extends Workflow> implements QualifiedWorkflowCallbackHandler<T> {

    private Class<? extends T> workflowType;

    /**
     * Dynamically derive this class its actual Parameterized Type
     * <p>
     * Concrete implementations of this base class also can easily override/replace this method
     * and then simply return the concrete class used to parameterize this base class.
     * </p>
     * @return the actual Parameterized Type of this class
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends T> getWorkflowType() {
        if (workflowType == null) {
            Class cls = this.getClass();
            while (!cls.getSuperclass().equals(BaseWorkflowCallbackHandler.class)) {
                cls = cls.getSuperclass();
            }
            Type clsType = cls.getGenericSuperclass();
            if (clsType instanceof ParameterizedType) {
                this.workflowType = (Class<? extends T>)((ParameterizedType)clsType).getActualTypeArguments()[0];
            }
            else {
                this.workflowType = (Class<? extends T>)Workflow.class;
            }
        }
        return workflowType;
    }
}
