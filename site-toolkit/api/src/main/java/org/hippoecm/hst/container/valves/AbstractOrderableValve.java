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
 * This abstract class determines the properties of {@link OrderableValve} at runtime if those are not specified.
 * </P>
 * <P>
 * When determining those, this checks if the implementation class is annotated by {@link Orderable}.
 * If the implementation class or one of its declaring interfaces is annotated by {@link Orderable},
 * the annotation attributes are used.
 * </P>
 * <P>
 * If the implementation class does neither specify the properties by itself, nor is annotated by {@link Orderable},
 * then the FQCN of the implementation class is used for the name and null is used for other properties.
 * </P>
 */
public abstract class AbstractOrderableValve extends AbstractValve implements OrderableValve {

    private String name;
    private String before;
    private String after;

    private boolean orderableInfoSearched;
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
     * @param before
     */
    public void setAfter(String after) {
        this.after = after;
    }

    private void lookupOrderableAnnotation() {
        if (!orderableInfoSearched) {
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
