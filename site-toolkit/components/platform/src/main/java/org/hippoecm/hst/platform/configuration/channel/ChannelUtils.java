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
package org.hippoecm.hst.platform.configuration.channel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.proxy.ProxyFactory;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelUtils {


    static final Logger log = LoggerFactory.getLogger(ChannelUtils.class);

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

        T parametersInfo = (T) factory.createInvokerProxy(proxyClasses[0].getClassLoader(), invoker, proxyClasses);

        return parametersInfo;
    }

    public static Class<?> getChannelInfoClass(final String className, final String contextPath) throws ClassNotFoundException {
        return getWebsiteClassLoader(contextPath).loadClass(className);
    }

    public static Class<? extends ChannelInfo> getChannelInfoClass(final Channel channel) {
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName == null) {
            log.debug("No channelInfoClassName defined. Return just the ChannelInfo interface class");
            return ChannelInfo.class;
        }
        try {
            return (Class<? extends ChannelInfo>) getWebsiteClassLoader(channel.getContextPath()).loadClass(channelInfoClassName);
        } catch (ClassNotFoundException cnfe) {
            log.warn("Configured class '{}' was not found", channelInfoClassName, cnfe);
        } catch (ClassCastException cce) {
            log.warn("Configured class '{}' does not extend ChannelInfo",
                    channelInfoClassName, cce);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<Class<? extends ChannelInfo>> getChannelInfoMixins(final Channel channel) {
        final List<String> channelInfoMixinNames = channel.getChannelInfoMixinNames();

        if (CollectionUtils.isEmpty(channelInfoMixinNames)) {
            return Collections.emptyList();
        }

        List<Class<? extends ChannelInfo>> mixins = new ArrayList<>();

        for (String channelInfoMixinName : channelInfoMixinNames) {
            try {
                Class<? extends ChannelInfo> mixinClazz =
                        (Class<? extends ChannelInfo>) getWebsiteClassLoader(channel.getContextPath()).loadClass(channelInfoMixinName);
                mixins.add(mixinClazz);
            } catch (ClassNotFoundException cnfe) {
                log.warn("Configured mixin class {} was not found.", channelInfoMixinName, cnfe);
            } catch (ClassCastException cce) {
                log.warn("Configured mixin class {} does not extend ChannelInfo", channelInfoMixinName, cce);
            }
        }

        return mixins;
    }

    private static ClassLoader getWebsiteClassLoader(final String contextPath) {
        if (contextPath == null) {
            throw new ModelLoadingException("Cannot get a classloader if there is no contextPath provided");
        }
        final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        final HstModel hstModel = hstModelRegistry.getHstModel(contextPath);
        if (hstModel == null) {
            throw new ModelLoadingException(String.format("Cannot get ClassLoader for contextPath '%s' since there is no " +
                    "registered HstModel for that contextPath", contextPath));
        }
        return hstModel.getWebsiteClassLoader();
    }
}
