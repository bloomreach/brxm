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
package org.onehippo.cms7.services.search.binder;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.apache.commons.beanutils.PropertyUtils;
import org.onehippo.cms7.services.search.annotation.Field;
import org.onehippo.cms7.services.search.util.ContentBeanPropertyUtils;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

public class LazyContentBeanProxyFactory {

    private static final String GETTER_POINTCUT_EXPR = "execution(* *.get*())";
    private static final String BOOLEAN_GETTER_POINTCUT_EXPR = "execution(* *.is*())";

    private static AspectJExpressionPointcut pointcutForGetter;
    private static AspectJExpressionPointcut pointcutForBooleanGetter;

    static {
        pointcutForGetter = new AspectJExpressionPointcut();
        pointcutForGetter.setExpression(GETTER_POINTCUT_EXPR);
        pointcutForBooleanGetter = new AspectJExpressionPointcut();
        pointcutForBooleanGetter.setExpression(BOOLEAN_GETTER_POINTCUT_EXPR);
    }

    private LazyContentBeanProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Object contentBean, PropertyValueProvider pvp, Class<?> ... interfaces) {
        ProxyFactory factory = new ProxyFactory();
        Advice advice = new LazyPropertySettingAdvice(pvp);
        factory.addAdvisor(new DefaultPointcutAdvisor(pointcutForGetter, advice));
        factory.addAdvisor(new DefaultPointcutAdvisor(pointcutForBooleanGetter, advice));
        factory.setTarget(contentBean);

        if (interfaces != null) {
            for (Class<?> intf : interfaces) {
                factory.addInterface(intf);
            }
        }

        return (T) factory.getProxy();
    }

    private static class LazyPropertySettingAdvice implements MethodBeforeAdvice {

        private PropertyValueProvider pvp;

        public LazyPropertySettingAdvice(PropertyValueProvider pvp) {
            this.pvp = pvp;
        }

        @Override
        public void before(Method method, Object [] args, Object target) throws Throwable {
            Object propValue = method.invoke(target, args);

            if (propValue == null) {
                Field field = method.getAnnotation(Field.class);

                if (field != null) {
                    String propertyName = ContentBeanPropertyUtils.getPropertyName(method);
    
                    if (propertyName != null) {
                        propValue = pvp.getValue(propertyName);
        
                        if (propValue != null) {
                            PropertyUtils.setProperty(target, propertyName, propValue);
                        }
                    }
                }
            }
        }
    }
}
