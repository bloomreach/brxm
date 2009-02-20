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
package org.hippoecm.hst.core.jcr.pool;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * JNDI Resource Factory for {@link BasicPoolingRepository}
 * 
 * JNDI Resource can be configured like the following for Tomcat:
 * 
 * <code><pre>
 * 
 * <Context ...>
 *   ...
 *   <Resource name="jcr/MyReposicoty" auth="Container"
 *             type="javax.jcr.Repository"
 *             factory="org.hippoecm.hst.core.jcr.pool.BasicPoolingRepositoryFactory"
 *             repositoryAddress="rmi://127.0.0.1:1099/hipporepository"
 *             defaultCredentialsUserID="admin"
 *             defaultCredentialsPassword="admin"
 *             readOnly="false"
 *             maxActive="250"
 *             maxIdle="50"
 *             initialSize="0"
 *             maxWait="10000"
 *             testOnBorrow="true"
 *             testOnReturn="false"
 *             testWhileIdle="false"
 *             timeBetweenEvictionRunsMillis="60000"
 *             numTestsPerEvictionRun="1"
 *             minEvictableIdleTimeMillis="60000" />
 *   ...
 * </Context>
 * </pre></code>
 */
public class BasicPoolingRepositoryFactory implements ObjectFactory {

    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
            throws Exception {
        
        BasicPoolingRepository poolingRepository = new BasicPoolingRepository();
        
        poolingRepository.setSessionDecorator(new PooledSessionDecoratorProxyFactoryImpl());
        
        Reference ref = (Reference) obj;
        Enumeration addrs = ref.getAll();
        
        String defaultCredentialsUserID = null;
        String defaultCredentialsPassword = null; 
        
        while (addrs.hasMoreElements()) {
            RefAddr addr = (RefAddr) addrs.nextElement();
            String type = addr.getType();
            String value = (String) addr.getContent();
            
            if (type.equals("repositoryAddress")) {
                poolingRepository.setRepositoryAddress(value);
            } else if (type.equals("defaultCredentialsUserID")) {
                poolingRepository.setDefaultCredentialsUserID(value);
            } else if (type.equals("defaultCredentialsPassword")) {
                poolingRepository.setDefaultCredentialsPassword(value.toCharArray());
            } else if (type.equals("readOnly")) {
                poolingRepository.setReadOnly(Boolean.parseBoolean(value));
            } else if (type.equals("sessionLifecycleManageable") && Boolean.parseBoolean(value)) {
                poolingRepository.setResourceLifecycleManagement(new PooledSessionResourceManagement());
            } else if (type.equals("maxActive")) {
                poolingRepository.setMaxActive(Integer.parseInt(value));
            } else if (type.equals("maxIdle")) {
                poolingRepository.setMaxIdle(Integer.parseInt(value));
            } else if (type.equals("minIdle")) {
                poolingRepository.setMinIdle(Integer.parseInt(value));
            } else if (type.equals("initialSize")) {
                poolingRepository.setInitialSize(Integer.parseInt(value));
            } else if (type.equals("maxWait")) {
                poolingRepository.setMaxWait(Long.parseLong(value));
            } else if (type.equals("testOnBorrow")) {
                poolingRepository.setTestOnBorrow(Boolean.parseBoolean(value));
            } else if (type.equals("testOnReturn")) {
                poolingRepository.setTestOnReturn(Boolean.parseBoolean(value));
            } else if (type.equals("testWhileIdle")) {
                poolingRepository.setTestWhileIdle(Boolean.parseBoolean(value));
            } else if (type.equals("timeBetweenEvictionRunsMillis")) {
                poolingRepository.setTimeBetweenEvictionRunsMillis(Long.parseLong(value));
            } else if (type.equals("numTestsPerEvictionRun")) {
                poolingRepository.setNumTestsPerEvictionRun(Integer.parseInt(value));
            } else if (type.equals("minEvictableIdleTimeMillis")) {
                poolingRepository.setMinEvictableIdleTimeMillis(Long.parseLong(value));
            } 
        }
        
        poolingRepository.initialize();
        
        return poolingRepository;
    }

}
