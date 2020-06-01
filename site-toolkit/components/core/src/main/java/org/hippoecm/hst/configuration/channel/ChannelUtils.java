/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.proxy.ProxyFactory;

public class ChannelUtils {

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T extends ChannelInfo> T getChannelInfo(final Map<String, Object> values,
                                                           final Class<? extends ChannelInfo> parametersInfoType,
                                                           final Class<? extends ChannelInfo>...mixinTypes) {
        final int mixinTypesLen = (mixinTypes != null) ? mixinTypes.length : 0;
        Class<?>[] proxyClasses;

        if (parametersInfoType == null) {
            if (mixinTypesLen == 0) {
                return null;
            }

            proxyClasses = mixinTypes;
        } else {
            proxyClasses = new Class[mixinTypesLen + 1];
            proxyClasses[0] = parametersInfoType;
            System.arraycopy(mixinTypes, 0, proxyClasses, 1, mixinTypesLen);
        }

        for (Class<?> proxyClass : proxyClasses) {
            if (!proxyClass.isInterface()) {
                throw new IllegalArgumentException(
                        "The ParametersInfo annotation type, " + proxyClass.getName() + ", must be an interface.");
            }
        }

        ProxyFactory factory = new ProxyFactory();

        Invoker invoker = new Invoker() {

            public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                int argCount = (args == null ? 0 : args.length);

                if ("toString".equals(methodName) && argCount == 0) {
                    StringBuilder builder =
                            new StringBuilder("ChannelInfoProxy [parametersInfoType=")
                            .append(parametersInfoType.getName())
                            .append(", mixinTypes=")
                            .append(Arrays.toString(mixinTypes))
                            .append(", properties=")
                            .append(values)
                            .append("]");
                    return  builder.toString();
                }

                if ("equals".equals(methodName) && argCount == 1) {
                    return super.equals(args[0]);
                }

                if ("hashCode".equals(methodName) && argCount == 0) {
                    return super.hashCode();
                }

                if ("getProperties".equals(methodName) && (args == null || args.length == 0)) {
                    return Collections.unmodifiableMap(values);
                }

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
                    Object parameterValue = values.get(parameterName);

                    if (parameterValue == null || "".equals(parameterValue)) {
                        // when the parameter value is null or an empty string we return the default value from the annotation
                        return panno.defaultValue();
                    }

                    return parameterValue;
                }
            }
        };

        T parametersInfo = (T) factory.createInvokerProxy(ChannelUtils.class.getClassLoader(), invoker, proxyClasses);

        return parametersInfo;
    }

}
