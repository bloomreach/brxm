/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.util;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.text.StrLookup;
import org.springframework.util.MethodInvoker;

/**
 * {@link StrLookup} implementation supporting a generic reflection on other class or object.
 * <p>
 * For example, a bean configuration with this class can be set with a platform-specific configuration implementation
 * such as <code>org.hippoecm.hst.core.container.ContainerConfiguration</code> with reflection related properties,
 * in order to directly depend on that platform specific API while running together with it.
 */
public class DelegateInvokingStrLookup extends StrLookup<String> {

    private Class<?> targetClass;

    private Object targetObject;

    private String targetMethod;

    private String staticMethod;

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public Object getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }

    public String getStaticMethod() {
        return staticMethod;
    }

    public void setStaticMethod(String staticMethod) {
        this.staticMethod = staticMethod;
    }

    @Override
    public String lookup(String key) {
        try {
            final MethodInvoker invoker = new MethodInvoker();
            invoker.setTargetClass(targetClass);
            invoker.setTargetObject(targetObject);
            invoker.setStaticMethod(staticMethod);
            invoker.setTargetMethod(targetMethod);
            invoker.setArguments(key);
            invoker.prepare();
            return (String) invoker.invoke();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found for the invocation target.", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No such method for the invocation target.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Invocation target exception while invoking the delegate invoker.", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegal access exception while invoking the delegate invoker.", e);
        }
    }

}
