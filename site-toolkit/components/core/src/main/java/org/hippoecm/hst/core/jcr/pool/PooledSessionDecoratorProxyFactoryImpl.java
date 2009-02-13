package org.hippoecm.hst.core.jcr.pool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hippoecm.hst.proxy.ProxyUtils;
import org.springframework.aop.framework.ProxyFactory;

public class PooledSessionDecoratorProxyFactoryImpl implements SessionDecorator, PoolingRepositoryAware {
    
    protected PoolingRepository poolingRepository;

    public PooledSessionDecoratorProxyFactoryImpl() {
    }

    public final Session decorate(Session session) {
        ProxyFactory factory = new ProxyFactory(session);
        factory.addAdvice(new PooledSessionInterceptor());

        List<Advice> advices = getAdvices();

        if (advices != null) {
            for (Advice advice : advices) {
                factory.addAdvice(advice);
            }
        }

        return (Session) factory.getProxy();
    }

    public void setPoolingRepository(PoolingRepository poolingRepository) {
        this.poolingRepository = poolingRepository;
    }

    protected List<Advice> getAdvices() {
        return null;
    }

    private class PooledSessionInterceptor implements MethodInterceptor {
        private boolean alreadyReturned;

        public Object invoke(MethodInvocation invocation) throws Throwable {
            Object ret = null;

            if (this.alreadyReturned) {
                throw new RepositoryException("Session is already returned to the pool!");
            } else {
                String methodName = invocation.getMethod().getName();
                
                if ("logout".equals(methodName)) {
                    // when logout(), it acturally returns the session to the pool
                    Session session = (Session) invocation.getThis();
                    this.alreadyReturned = true;
                    poolingRepository.returnSession(session);
                } else if ("getRepository".equals(methodName)) {
                    // when getRepository(), it actually returns the session pooling repository
                    ret = poolingRepository;
                } else if ("impersonate".equals(methodName)) {
                    // when impersonate(), it actually returns a session which is borrowed 
                    // from another session pool repository based on the credentials.
                    Credentials credentials = (Credentials) invocation.getArguments()[0];
                    ret = poolingRepository.impersonate(credentials);
                } else {
                    ret = invocation.proceed();
                }
            }

            if (ret != null && ret instanceof Item) {
                Set<String> unsupportedMethodNames = new HashSet<String>();
                unsupportedMethodNames.add("getSession");
                ret = ProxyUtils.createdUnsupportableProxyObject(ret, unsupportedMethodNames);
            }

            return ret;
        }
    }

}
