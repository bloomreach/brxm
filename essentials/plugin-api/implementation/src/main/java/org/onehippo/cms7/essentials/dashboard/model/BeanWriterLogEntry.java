/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model;

/**
 * @version "$Id$"
 */
public class BeanWriterLogEntry {

    private String beanName;
    private String methodName;
    private ActionType actionType;


    public BeanWriterLogEntry(final ActionType actionType) {

        this.actionType = actionType;
    }

    public BeanWriterLogEntry(final String beanName, final ActionType actionType) {
        this.beanName = beanName;
        this.actionType = actionType;
    }

    public BeanWriterLogEntry(final String beanName, final String methodName, final ActionType actionType) {
        this.beanName = beanName;
        this.methodName = methodName;
        this.actionType = actionType;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(final String beanName) {
        this.beanName = beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(final ActionType actionType) {
        this.actionType = actionType;
    }


    public String getMessage() {
        if(actionType==ActionType.CREATED_CLASS){
            return "Created HST bean: " + getBeanName();
        } else if(actionType==ActionType.CREATED_METHOD){
            return "Created Method: " + getMethodName();
        }

        return actionType.toString();
    }
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BeanWriterLogEntry{");
        sb.append("beanName='").append(beanName).append('\'');
        sb.append(", methodName='").append(methodName).append('\'');
        sb.append(", actionType=").append(actionType);
        sb.append('}');
        return sb.toString();
    }


}
