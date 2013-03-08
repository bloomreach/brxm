/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container.valves;

import org.hippoecm.hst.container.annotations.Orderable;
import org.hippoecm.hst.core.container.OrderableValve;

/**
 * AbstractOrderableValve
 * <P>
 * This abstract class determines the valve name and other attributes by reading the properties of {@link OrderableValve}.
 * e.g., {@link OrderableValve#getName()}, {@link OrderableValve#getAfter()}, {@link OrderableValve#getBefore()}, etc.
 * </P>
 * <P>
 * If those properties return empty values, then it checks if the implementation class or one of its interfaces is 
 * annotated by {@link Orderable}. If the implementation class or one of its declaring interfaces is annotated by 
 * {@link Orderable}, the annotation attributes are used.
 * </P>
 * <P>
 * If the implementation class does neither specify the properties by itself nor is annotated by {@link Orderable},
 * then the FQCN of the implementation class is used instead for the 'name' and null is used for other properties (e.g., 'before', 'after', etc.).
 * </P>
 * <P>
 * NOTE: Here is best practices you should consider when implementing an orderable valve by extending this abtract class.
 * <UL>
 *   <LI>
 *     If your valve implementation class extends {@link AbstractOrderableValve} and
 *     you let your implementation class implement a role marker interface annotated by {@link Orderable},
 *     then you'd better not override the methods such as {@link OrderableValve#getName()} again
 *     because it may confuse developers when searching for the effective valve name.
 *   </LI>
 *   <LI>
 *     If your valve implementation class itself is annotated by {@link Orderable} and it extends {@link AbstractOrderableValve},
 *     then you'd better not override the methods such as {@link OrderableValve#getName()} again
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
 * </P>
 */
public abstract class AbstractOrderableValve extends AbstractValve implements OrderableValve {

    private String name;
    private String before;
    private String after;

    private volatile boolean orderableInfoSearched;
    private Orderable orderableInfo;
    private Class<?> orderableAnnotatedType;

    @Override
    public String getName() {
        if (name == null || "".equals(name)) {
            lookupOrderableAnnotation();

            if (orderableInfo != null) {
                name = orderableInfo.name();

                if (name == null || "".equals(name)) {
                    Class<?> role = orderableInfo.role();

                    if (role != null && role != Object.class) {
                        name = role.getName();
                    }
                }
            }
        }

        if (name == null || "".equals(name)) {
            if (orderableAnnotatedType != null) {
                name = orderableAnnotatedType.getName();
            } else {
                name = getClass().getName();
            }
        }

        return name;
    }

    /**
     * Sets the valve name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getBefore() {
        if (before == null || "".equals(before)) {
            lookupOrderableAnnotation();

            if (orderableInfo != null) {
                before = orderableInfo.before();
            }
        }

        return before;
    }

    /**
     * Sets postrequisite valve names that should follow this valve.
     * The <code>before</code> can have multiple valve names, separated by ' ', ',', '\t', '\r' or '\n'
     * @param before
     */
    public void setBefore(String before) {
        this.before = before;
    }

    @Override
    public String getAfter() {
        if (after == null || "".equals(after)) {
            lookupOrderableAnnotation();

            if (orderableInfo != null) {
                after = orderableInfo.after();
            }
        }

        return after;
    }

    /**
     * Sets prerequisite valve names that should follow this valve.
     * The <code>after</code> can have multiple valve names, separated by ' ', ',', '\t', '\r' or '\n'
     * @param after
     */
    public void setAfter(String after) {
        this.after = after;
    }

    private void lookupOrderableAnnotation() {
        if (!orderableInfoSearched) {
            synchronized (this) {
                if (orderableInfoSearched) {
                    return;
                }

                try {
                    Class<?> clazz = getClass();
    
                    orderableInfo = clazz.getAnnotation(Orderable.class);
    
                    if (orderableInfo != null) {
                        orderableAnnotatedType = clazz;
                        return;
                    }
    
                    if (orderableInfo == null) {
                        for (Class<?> ifaceClazz : clazz.getInterfaces()) {
                            orderableInfo = ifaceClazz.getAnnotation(Orderable.class);
    
                            if (orderableInfo != null) {
                                orderableAnnotatedType = ifaceClazz;
                                return;
                            }
                        }
                    }
                } finally {
                    orderableInfoSearched = true;
                }
            }
        }
    }
}
