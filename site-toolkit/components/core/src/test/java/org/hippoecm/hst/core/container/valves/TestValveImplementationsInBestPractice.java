/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container.valves;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Pattern;

import org.hippoecm.hst.container.annotations.Orderable;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.OrderableValve;
import org.hippoecm.hst.core.container.ValveContext;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * TestValveImplementationsInBestPractice
 * <P>
 * This unit test checks if the built-in valve implementations confirm the best practices such as:
 * <UL>
 *   <LI>
 *     If a built-in valve implementation class implements {@link OrderableValve} and a role marker interface
 *     having {@link Orderable} annotation, then it should not override the methods such as
 *     {@link OrderableValve#getName()} again because it may confuse developers
 *     when searching for the effective valve name.
 *   </LI>
 *   <LI>
 *     If a built-in valve implementation class itself is annotated by {@link Orderable} and it implements
 *     {@link OrderableValve}, then it should not override the methods such as {@link OrderableValve#getName()} again
 *     because the overriden {@link OrderableValve#getName()} has precedence over the annotated attribute,
 *     which may cause unnecessary confusions.
 *   </LI>
 *   <LI>
 *     As a rule of thumb, implement your valve class to extend {@link AbstractOrderableValve},
 *     add a role marker interface annotated by {@link Orderable}, and let your valve implementation
 *     implement the role marker interface. In most cases, this will be the neatest solution.
 *     In rare cases, you might want to reuse a valve implementation class for multiple valve beans.
 *     In that case, you'd better override the methods of {@OrderableValve} manually, and you'd better not 
 *     use {@link Orderable} annotation in the implementation class or one of its interfaces, in order to
 *     avoid unnecessary confusions.
 *   </LI>
 * </UL>
 * See <a href="https://issues.onehippo.com/browse/HSTTWO-2485">HSTTWO-2485</a> for detail.
 * </P>
 * @see {@link AbstractOrderableValve}
 */
public class TestValveImplementationsInBestPractice {

    private static Logger log = LoggerFactory.getLogger(TestValveImplementationsInBestPractice.class);

    private static final String BASE_PACKAGE_FOR_VALVES = "org.hippoecm.hst";
    private static final Pattern VALVE_IMPL_CLASS_NAME_INCLUDED_PATTERN = Pattern.compile("^.+Valve.*");
    private static final Pattern VALVE_IMPL_CLASS_NAME_EXCLUDED_PATTERN = null;

    /**
     * A built-in valve implementation is not supposed to override {@link OrderableValve} interface again
     * if it implements a role marker interface having {@link Orderable} annotation already.
     */
    @Test
    public void testVerifyOrderableValveImpls() throws Exception {

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        if (VALVE_IMPL_CLASS_NAME_INCLUDED_PATTERN != null) {
            scanner.addIncludeFilter(new RegexPatternTypeFilter(VALVE_IMPL_CLASS_NAME_INCLUDED_PATTERN));
        }

        if (VALVE_IMPL_CLASS_NAME_EXCLUDED_PATTERN != null) {
            scanner.addExcludeFilter(new RegexPatternTypeFilter(VALVE_IMPL_CLASS_NAME_EXCLUDED_PATTERN));
        }

        Set<BeanDefinition> beanDefs = scanner.findCandidateComponents(BASE_PACKAGE_FOR_VALVES);

        boolean intentionalWrongCustomOrderableValveChecked = false;

        for (BeanDefinition beanDef : beanDefs) {
            Class<?> valveClazz = null;

            try {
                valveClazz = Class.forName(beanDef.getBeanClassName());
            } catch (Exception e) {
                log.debug("Skipping unloadable class: {}", beanDef.getBeanClassName());
                continue;
            }

            if (valveClazz.isInterface()) {
                log.debug("Skipping interface: {}", valveClazz);
                continue;
            }

            if (!OrderableValve.class.isAssignableFrom(valveClazz)) {
                log.debug("Skipping valve class not implementing OrderableValve interface: {}", valveClazz);
                continue;
            }

            Method overridenGetNameMethod = getOverridingMethod(valveClazz, "getName", (Class<?> []) null);

            // intentionally checks if the example valve which is not in best practice is filtered properly..
            if (IntentionalWrongCustomOrderableValveImpl.class == valveClazz) {
                intentionalWrongCustomOrderableValveChecked = true;
                assertNotNull(valveClazz.getName() + " must have #getName() intentially.", overridenGetNameMethod);
                continue;
            }

            Class<?> ifaceAnnotatedByOrderable = getInterfaceAnnotatedByOrderable(valveClazz);
            Orderable orderableDefinedInClass = valveClazz.getAnnotation(Orderable.class);

            if (orderableDefinedInClass != null) {
                assertNull(valveClazz.getName() + " should not override #getName() because it has interface " + 
                        "annotated by Orderable already. " +
                        "If a built-in valve implementation class itself is annotated by {@link Orderable} and it implements " +
                        "{@link OrderableValve}, then it should not override the methods such as {@link OrderableValve#getName()} again " +
                        "because the overriden {@link OrderableValve#getName()} has precedence over the annotated attribute, " +
                        "which may cause unnecessary confusions.",
                        overridenGetNameMethod);
            } else if (ifaceAnnotatedByOrderable != null) {
                assertNull(valveClazz.getName() + " should not override #getName() because it has interface " + 
                        "annotated by Orderable already. " +
                        "Note: If a built-in valve implementation class implements {@link OrderableValve} and a role marker interface " +
                        "having {@link Orderable} annotation, then it should not override the methods such as " +
                        "{@link OrderableValve#getName()} again because it may confuse other code maintainers " +
                        "when finding the effective valve name",
                        overridenGetNameMethod);
            }
        }

        assertTrue(intentionalWrongCustomOrderableValveChecked);
    }

    private Class<?> getInterfaceAnnotatedByOrderable(Class<?> valveClazz) throws Exception {
        for (Class<?> ifaceClazz : valveClazz.getInterfaces()) {
            Orderable orderableInfo = ifaceClazz.getAnnotation(Orderable.class);

            if (orderableInfo != null) {
                return ifaceClazz;
            }
        }

        return null;
    }

    private Method getOverridingMethod(Class<?> clazz, String methodName, Class<?> ... paramTypes) {
        try {
            Method method = clazz.getMethod(methodName, paramTypes);

            if (method != null && method.getDeclaringClass() == clazz) {
                return method;
            }
        } catch (NoSuchMethodException e) {
            // ignore because it's intended to return null when no such method is found.
        }

        return null;
    }

    @Orderable
    public interface IntentionalWrongCustomOrderableValve extends OrderableValve {
    }

    public static class IntentionalWrongCustomOrderableValveImpl extends AbstractOrderableValve implements IntentionalWrongCustomOrderableValve {
        @Override
        public void invoke(ValveContext context) throws ContainerException {
        }

        @Override
        public String getName() {
            return "overriden name without any good reason even though you implemented the role marker interface " +
                    "annotated by Orderable";
        }
    }
}
