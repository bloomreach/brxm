package org.hippoecm.hst.core.jcr.pool;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.UnsupportedRepositoryOperationException;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class ReadOnlyPooledSessionDecoratorProxyFactoryImpl extends PooledSessionDecoratorProxyFactoryImpl
{
    protected Map<String, Boolean> readOnlyMethodMap;
    protected List<Advice> advices = new ArrayList<Advice>();
    
    public ReadOnlyPooledSessionDecoratorProxyFactoryImpl()
    {
        super();
        
        this.readOnlyMethodMap = new HashMap<String, Boolean>();
        this.readOnlyMethodMap.put("move", Boolean.TRUE);
        this.readOnlyMethodMap.put("save", Boolean.TRUE);
        this.readOnlyMethodMap.put("removeLockToken", Boolean.TRUE);
        this.readOnlyMethodMap.put("setNamespacePrefix", Boolean.TRUE);

        this.advices.add(new ReadOnlySessionInterceptor());
    }
    
    protected List<Advice> getAdvices()
    {
        return this.advices;
    }
    
    public void setReadOnlyMethods(List<String> readOnlyMethods)
    {
        this.readOnlyMethodMap = new HashMap<String, Boolean>();
        
        if (readOnlyMethods != null)
        {
            for (String methodName : readOnlyMethods)
            {
                this.readOnlyMethodMap.put(methodName, Boolean.TRUE);
            }
        }
    }
    
    private class ReadOnlySessionInterceptor implements MethodInterceptor
    {
        public Object invoke(MethodInvocation invocation) throws Throwable
        {
            Method method = invocation.getMethod();
            
            if (readOnlyMethodMap != null && readOnlyMethodMap.containsKey(method.getName()))
            {
                throw new UnsupportedRepositoryOperationException("Read-only session does not support this operation: " + method.getName());
            }
            
            Object ret = invocation.proceed();
            return ret;
        }
    }
}
