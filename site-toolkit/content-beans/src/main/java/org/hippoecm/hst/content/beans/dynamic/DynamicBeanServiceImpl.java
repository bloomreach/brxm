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
package org.hippoecm.hst.content.beans.dynamic;

import java.lang.reflect.Modifier;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.tuple.Pair;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.MethodCall;

public class DynamicBeanServiceImpl implements DynamicBeanService {

    public static final String DEFAULT_BASE_CLASS = "org.hippoecm.hst.content.beans.standard.HippoDocument";

    private static final Logger log = LoggerFactory.getLogger(DynamicBeanServiceImpl.class);

    @Override
    public Class<? extends HippoBean> createBean(Node node) throws RepositoryException, ObjectBeanManagerException {
        return createBean(node, getDefaultBaseClass());
    }

    private Class<? extends HippoBean> getDefaultBaseClass() throws ObjectBeanManagerException {
        try {
            Class<? extends HippoBean> baseClass = (Class<? extends HippoBean>) Class.forName(DEFAULT_BASE_CLASS);
            log.info("HippoDocument class will be used as base class for bean generation.");
            return baseClass;
        } catch (ClassNotFoundException e) {
            throw new ObjectBeanManagerException("The base class is not found.", e);
        }
    }

    @Override
    public Class<? extends HippoBean> createBean(Node node, Class<? extends HippoBean> baseClass) throws RepositoryException, ObjectBeanManagerException {
        try {
            final String jcrPrimaryNodeType = node.getPrimaryNodeType().getName();
            Builder<? extends HippoBean> builder = new ByteBuddy().with(new NamingStrategy.SuffixingRandom(jcrPrimaryNodeType.replace(':', '_')))
                    .subclass(baseClass);
            final String[] namespace = jcrPrimaryNodeType.split(":");
            if (namespace.length == 2) {
                //TODO: Fields should be received over object type service
                final Node typeNode = node.getSession()
                        .getNode("/hippo:namespaces/" + namespace[0] + "/" + namespace[1] + "/hipposysedit:nodetype/hipposysedit:nodetype");
                NodeIterator itr = typeNode.getNodes();
                while (itr.hasNext()) {
                    final Node child = itr.nextNode();
                    if (child.getPrimaryNodeType().getName().equals("hipposysedit:field") && child.hasProperty("hipposysedit:type")) {
                        final String returnType = child.getProperty("hipposysedit:type").getString();
                        final Boolean hasMultiple = child.getProperty("hipposysedit:multiple").getBoolean();
                        final String fullPropertyName = child.getProperty("hipposysedit:path").getString();
                        String propertyName = fullPropertyName.split(":")[1];
                        propertyName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);

                        if (ClassUtils.getMethodIfAvailable(baseClass, propertyName) == null) {
                            //the method doesn't exist in the base class, it will be added
                            builder = addMethod(builder, propertyName, returnType, fullPropertyName, hasMultiple);
                        }
                    }
                }

            }

            return builder.make().load(getClass().getClassLoader()).getLoaded();

        } catch (Exception e) {
            throw new ObjectBeanManagerException("The dynamic bean couldn't been generated.", e);
        }

    }

    private static Builder<? extends HippoBean> addMethod(Builder<? extends HippoBean> builder, final String methodName, final String returnType,
            final String propertyName, final Boolean hasMultiple) {
        try {
            Pair<Class<?>, String> retutnTypeAndMethodName = null;
            if (returnType.equals("String") || returnType.equals("Html") || returnType.equals("Text") || returnType.equals("StaticDropdown")
                    || returnType.equals("DynamicDropdown") || returnType.equals("RadioGroup")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.STRING.getMethod(hasMultiple);
            } else if (returnType.equals("Date")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.DATE.getMethod(hasMultiple);
            } else if (returnType.equals("Boolean") || returnType.equals("selection:BooleanRadioGroup")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.BOOLEAN.getMethod(hasMultiple);
            } else if (returnType.equals("Double")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.DOUBLE.getMethod(hasMultiple);
            } else if (returnType.equals("Long")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.LONG.getMethod(hasMultiple);
            } else if (returnType.equals("Docbase")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.DOCBASE.getMethod(hasMultiple);
            } else if (returnType.equals("hippogallerypicker:imagelink")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.IMAGE.getMethod(hasMultiple);
            } else if (returnType.equals("hippostd:html")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.HTML.getMethod(hasMultiple);
            } else if (returnType.contains(":")) {
                retutnTypeAndMethodName = DynamicBeanMethodTypes.DOCUMENT.getMethod(hasMultiple);
            } else {
                log.warn("Return type is not matched. The method '{}' couldn't been added to the bean class.", propertyName);
                return builder;
            }
            builder = builder.defineMethod(methodName, retutnTypeAndMethodName.getLeft(), Modifier.PUBLIC)
                    .intercept((MethodCall.invoke(DynamicBeanWrapper.class.getMethod(retutnTypeAndMethodName.getRight(), String.class)))
                            .onMethodCall(MethodCall.invoke(HippoItem.class.getMethod("getDynamicBeanWrapper"))).with(propertyName));
        } catch (Exception e) {
            log.warn("The method '{}' couldn't been added to the bean class", methodName);
        }
        return builder;
    }
}
