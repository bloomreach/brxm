/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.proxy;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.Invoker;
import org.apache.commons.proxy.ObjectProvider;

/**
 * Extending the commons-proxy's ProxyFactory because the class in 1.0 release
 * does not provide internal handler as serializable ones.
 * 
 * @version $Id$
 */
public class ProxyFactory extends org.apache.commons.proxy.ProxyFactory 
{
    
    @Override
    public Object createDelegatorProxy(ClassLoader classLoader, ObjectProvider delegateProvider,
                                       Class[] proxyClasses)
    {
        return Proxy.newProxyInstance(classLoader, proxyClasses,
                new DelegatorInvocationHandler(delegateProvider));
    }

    @Override
    public Object createInterceptorProxy(ClassLoader classLoader, Object target, Interceptor interceptor,
                                         Class[] proxyClasses)
    {
        return Proxy
                .newProxyInstance(classLoader, proxyClasses, new InterceptorInvocationHandler(target, interceptor));
    }

    @Override
    public Object createInvokerProxy(ClassLoader classLoader, Invoker invoker,
                                     Class[] proxyClasses)
    {
        return Proxy.newProxyInstance(classLoader, proxyClasses, new InvokerInvocationHandler(invoker));
    }

//**********************************************************************************************************************
// Inner Classes
//**********************************************************************************************************************

    private static class DelegatorInvocationHandler extends AbstractInvocationHandler
    {
        private static final long serialVersionUID = 1L;
        
        private final ObjectProvider delegateProvider;

        protected DelegatorInvocationHandler(ObjectProvider delegateProvider)
        {
            this.delegateProvider = delegateProvider;
        }

        public Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable
        {
            try
            {
                return method.invoke(delegateProvider.getObject(), args);
            }
            catch (InvocationTargetException e)
            {
                throw e.getTargetException();
            }
        }
    }

    private static class InterceptorInvocationHandler extends AbstractInvocationHandler
    {
        private static final long serialVersionUID = 1L;
        
        private final Object target;
        private final Interceptor methodInterceptor;

        public InterceptorInvocationHandler(Object target, Interceptor methodInterceptor)
        {
            this.target = target;
            this.methodInterceptor = methodInterceptor;
        }

        public Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable
        {
            final ReflectionInvocation invocation = new ReflectionInvocation(target, method, args);
            return methodInterceptor.intercept(invocation);
        }
    }

    private abstract static class AbstractInvocationHandler implements InvocationHandler, Serializable
    {
        private static final long serialVersionUID = 1L;
        
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if (isHashCode(method))
            {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            else if (isEqualsMethod(method))
            {
                return Boolean.valueOf(proxy == args[0]);
            }
            else
            {
                return invokeImpl(proxy, method, args);
            }
        }

        protected abstract Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable;
    }

    private static class InvokerInvocationHandler extends AbstractInvocationHandler
    {
        private static final long serialVersionUID = 1L;
        
        private final Invoker invoker;

        public InvokerInvocationHandler(Invoker invoker)
        {
            this.invoker = invoker;
        }

        public Object invokeImpl(Object proxy, Method method, Object[] args) throws Throwable
        {
            return invoker.invoke(proxy, method, args);
        }
    }

    protected static boolean isHashCode(Method method)
    {
        return "hashCode".equals(method.getName()) &&
                Integer.TYPE.equals(method.getReturnType()) &&
                method.getParameterTypes().length == 0;
    }

    protected static boolean isEqualsMethod(Method method)
    {
        return "equals".equals(method.getName()) &&
                Boolean.TYPE.equals(method.getReturnType()) &&
                method.getParameterTypes().length == 1 &&
                Object.class.equals(method.getParameterTypes()[0]);
    }

    private static class ReflectionInvocation implements Invocation, Serializable
    {
        private static final long serialVersionUID = 1L;

        private transient Method method;
        private transient Object[] arguments;
        private transient Object target;

        public ReflectionInvocation(Object target, Method method, Object[] arguments)
        {
            this.method = method;
            this.arguments = (arguments == null ? ArrayUtils.EMPTY_OBJECT_ARRAY : arguments);
            this.target = target;
        }

        public Object[] getArguments()
        {
            return arguments;
        }

        public Method getMethod()
        {
            return method;
        }

        public Object getProxy()
        {
            return target;
        }

        public Object proceed() throws Throwable
        {
            try
            {
                return method.invoke(target, arguments);
            }
            catch (InvocationTargetException e)
            {
                throw e.getTargetException();
            }
        }
    }
}

