package org.hippoecm.hst.core.jcr.pool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hippoecm.hst.proxy.ProxyUtils;
import org.springframework.aop.framework.ProxyFactory;

public class PooledSessionDecoratorProxyFactoryImpl implements SessionDecorator, PoolingRepositoryAware
{
    protected PoolingRepository poolingRepository;
    
    public PooledSessionDecoratorProxyFactoryImpl()
    {
    }
    
    public final Session decorate(Session session)
    {
        ProxyFactory factory = new ProxyFactory(session);
        factory.addAdvice(new PooledSessionInterceptor());
        
        List<Advice> advices = getAdvices();
        
        if (advices != null)
        {
            for (Advice advice : advices)
            {
                factory.addAdvice(advice);
            }
        }
        
        return (Session) factory.getProxy();
    }
    
    public void setPoolingRepository(PoolingRepository poolingRepository)
    {
        this.poolingRepository = poolingRepository;
    }
    
    protected List<Advice> getAdvices()
    {
        return null;
    }
    
    private class PooledSessionInterceptor implements MethodInterceptor
    {
        private boolean closed;
        
        public Object invoke(MethodInvocation invocation) throws Throwable
        {
            Object ret = null;
            
            if (this.closed)
            {
                throw new RepositoryException("Session is already closed!");
            }
            else
            {
                if ("logout".equals(invocation.getMethod().getName()))
                {
                    Session session = (Session) invocation.getThis();
                    this.closed = true;
                    poolingRepository.returnSession(session);
                }
                else
                {
                    ret = invocation.proceed();
                }
            }
            
            if (ret != null && ret instanceof Item)
            {
                Set<String> unsupportedMethodNames = new HashSet<String>();
                unsupportedMethodNames.add("getSession");
                ret = ProxyUtils.createdUnsupportableProxyObject(ret, unsupportedMethodNames);
            }
            
            return ret;
        }
    }

}
