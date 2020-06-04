/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;


/**
 * <p>
 *     Classes that want to use dynamic bean interceptors should extend this class AND add the exact same constructor
 *     as the one for DynamicBeanInterceptor. The reason for this exact signature can be found in
 *     {@link DynamicBeanBuilder#addBeanMethodCustomField(Class, String, String, boolean, Node)} through:
 *     <pre>
 *         final DynamicBeanInterceptor interceptor =
 *                     (DynamicBeanInterceptor) constructor.newInstance(propertyName, multiple, documentTypeNode);
 *     </pre>
 * </p>
 * <p>
 *     The concrete classes must implement a method that returns the POJO that this interceptor should provide. That
 *     method must be in the following format:
 *     <pre>
 *             public Foo createFoo(@net.bytebuddy.implementation.bind.annotation.Super(proxyType = net.bytebuddy.dynamic.TargetType.class) Object superObject) {
 *                final HippoBean hippoBean = (HippoBean) superObject;
 *                return new Foo(hippoBean);
 *             }
 *     </pre>
 *     byte buddy does the magic by invoking this 'createFoo' (note the method name is irrelevant, it is the @Super
 *     annotation
 * </p>
 * <p>
 *     Concurrency: DynamicBeanInterceptor concrete instances must be thread-safe since they are used concurrently: Per
 *     dynamic bean class interceptors are created once, but used concurrently by dynamic bean instances
 * </p>
 * <p>
 *     <b>Note:</b> implementations are only allowed to use {@code documentTypeNode} or its backing JCR Session (HST
 *     config reader session) during constructor phase. After the construction of the interceptor, the backing JCR session
 *     is returned to the session pool and might very well be logged out.
 * </p>
 */
public abstract class DynamicBeanInterceptor {

    private final String propertyName;
    private final boolean multiple;

    /**
     * <p>
     *     Although this constructor doesn't do anything with {@code documentTypeNode}, the argument is still there
     *     since this is the exact constructor that concrete implementations MUST implement, see
     *     DynamicBeanBuilder#addBeanMethodCustomField :
     *     <pre>
     *         final DynamicBeanInterceptor interceptor =
     *                        (DynamicBeanInterceptor) constructor.newInstance(propertyName, multiple, documentTypeNode);
     *     </pre>
     * </p>
     * @param propertyName field name in the document (eg: myproject:author)
     * @param multiple whether the field type is multiple
     * @param documentTypeNode node of the document type (eg: /hippo:namespaces/myproject/newsdocument). The backing JCR
     *                         session behind this {@link Node} is the HST Config reader session and is a Pooled Session.
     *                         After the construction of the object has been done, this session is returned to the pool
     *                         and any access to {@code documentTypeNode} or to its backing jcr session is forbidden since
     *                         it will result in JCR Exceptions!
     *
     */
    protected DynamicBeanInterceptor(final String propertyName, final boolean multiple, final Node documentTypeNode) {

        this.propertyName = propertyName;
        this.multiple = multiple;
        // never store documentTypeNode as instance variable, see comment above
    }

    /**
     * @return field name in the document (eg: myproject:author)
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return  whether the field type is multiple
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * 
     * @return cmsType class definition
     */
    public abstract Class<? extends InterceptorEntity> getCmsType();

}
