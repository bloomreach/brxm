/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.proxy.ProxyFactory;

public class ChannelUtils {

    @SuppressWarnings("unchecked")
    public static <T> T getChannelInfo(final Map<String, Object> values, Class<?> parametersInfoType) {

        if (!parametersInfoType.isInterface()) {
            throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
        }

        ProxyFactory factory = new ProxyFactory();

        Invoker invoker = new Invoker() {

            public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                boolean isGetter = false;
                boolean isSetter = false;

                if (methodName.startsWith("get") && (args == null || args.length == 0)) {
                    isGetter = true;
                } else if (methodName.startsWith("is") && (args == null || args.length == 0)) {
                    isGetter = true;
                } else if (methodName.startsWith("set") && (args != null && args.length == 1)) {
                    isSetter = true;
                }

                Parameter panno = method.getAnnotation(Parameter.class);
                if (panno == null || (!isGetter && !isSetter)) {
                    throw new IllegalArgumentException("Method " + method.getName() + " is not a valid parameters info method");
                }

                String parameterName = panno.name();

                if (StringUtils.isBlank(parameterName)) {
                    throw new IllegalArgumentException("The parameter name is empty.");
                }
                if (isSetter) {
                    throw new UnsupportedOperationException("Setter method is not supported.");
                } else {
                    return values.get(parameterName);
                }
            }
        };

        T parametersInfo = (T) factory.createInvokerProxy(invoker, new Class[]{parametersInfoType});

        return parametersInfo;
    }

}
