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
package org.hippoecm.hst.core.jcr.pool;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JNDI Resource Factory for the combination of {@link MultipleRepository} and {@link BasicPoolingRepository}
 * <P>
 * JNDI Resource for MultipleRepository object, which dispatches to a pooling repository according to the input credentials, 
 * can be configured in the application context descriptor like the following for Tomcat.
 * </P>
 * <P>
 * <EM>The default separator character for multiple values is a comma ('.') like the following example. 
 * You can also configure the separator characters by setting 'separatorChars' attribute.</EM>
 * </P>
 * 
 * <code><pre>
 * &lt;Context ...>
 *   ...
 *   &lt;Resource name="jcr/repository" auth="Container"
 *             type="javax.jcr.Repository"
 *             factory="org.hippoecm.hst.core.jcr.pool.MultiplePoolingRepositoryFactory"
 *             repositoryAddress="rmi://localhost:1099/hipporepository, rmi://localhost:1099/hipporepository"
 *             defaultCredentialsUserID="admin, editor"
 *             defaultCredentialsPassword="admin, editor"
 *             readOnly="false, false"
 *             maxActive="250, 250"
 *             maxIdle="50, 50"
 *             initialSize="0, 0"
 *             maxWait="10000, 10000"
 *             testOnBorrow="true, true"
 *             testOnReturn="false, false"
 *             testWhileIdle="false, false"
 *             timeBetweenEvictionRunsMillis="60000, 60000"
 *             numTestsPerEvictionRun="1, 1"
 *             minEvictableIdleTimeMillis="60000, 60000"
 *             refreshOnPassivate="true, true" />
 *   ...
 * &lt;/Context>
 * </pre></code>
 * <BR/>
 * In addition, you should modify the web application deployment descriptor (/WEB-INF/web.xml) to
 * declare the JNDI name under which you will look up preconfigured repository in the above like the following:
 * <code><pre>
 * &lt;resource-ref>
 *   &lt;description>JCR Repository&lt;/description>
 *   &lt;res-ref-name>jcr/repository&lt;/res-ref-name>
 *   &lt;res-type>javax.jcr.Repository&lt;/res-type>
 *   &lt;res-auth>Container&lt;/res-auth>
 * &lt;/resource-ref>
 * </pre></code>
 * <BR/>
 * Finally, you can write codes in a JSP page to test it like the following example:
 * <code><pre>
 * &lt;%@ page language="java" import="javax.jcr.*, javax.naming.*" %>
 * &lt;%
 * Context initCtx = new InitialContext();
 * Context envCtx = (Context) initCtx.lookup("java:comp/env");
 * Repository repository = (Repository) envCtx.lookup("jcr/repository");
 * Credentials credentials = new SimpleCredentials("siteuser", "siteuser");
 * Session jcrSession = repository.login(credentials);
 * // do something...
 * jcrSession.logout();
 * %>
 * </pre></code>
 */
public class MultiplePoolingRepositoryFactory implements ObjectFactory {
    
    static Logger logger = LoggerFactory.getLogger(MultiplePoolingRepositoryFactory.class);
    
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        return getObjectInstance(obj);
    }
    
    public MultipleRepository getObjectInstance(Object obj) throws Exception {
        Map<String, String []> configMap = null;
        
        if (obj instanceof Reference) {
            configMap = getConfigurationMap((Reference) obj);
        } else if (obj instanceof Map) {
            configMap = (Map<String, String []>) obj;
        } else {
            throw new IllegalArgumentException("Invalid argument: " + obj);
        }
        
        return getObjectInstanceByConfigMap(configMap);
    }
    
    public MultipleRepository getObjectInstanceByConfigMap(Map<String, String []> configMap) throws Exception {

        int repositoryCount = 0;
        
        try {
            repositoryCount = configMap.get("repositoryAddress").length;
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("The repositoryAddress is not valid.");
            }
        }
        
        BasicPoolingRepository [] poolingRepositoryArray = new BasicPoolingRepository[repositoryCount];
        
        for (int i = 0; i < poolingRepositoryArray.length; i++) {
            poolingRepositoryArray[i] = new BasicPoolingRepository();
        }
        
        for (Map.Entry<String, String []> entry : configMap.entrySet()) {
            String type = entry.getKey();
            String [] valueArray = entry.getValue();
            
            if (type.equals("repositoryProviderClassName")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setRepositoryProviderClassName(valueArray[i]);
                }
            } else if (type.equals("repositoryAddress")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setRepositoryAddress(valueArray[i]);
                }
            } else if (type.equals("defaultCredentialsUserID")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setDefaultCredentialsUserID(valueArray[i]);
                }
            } else if (type.equals("defaultCredentialsUserIDSeparator")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setDefaultCredentialsUserIDSeparator(valueArray[i]);
                }
            } else if (type.equals("defaultCredentialsPassword")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setDefaultCredentialsPassword(valueArray[i].toCharArray());
                }
            } else if (type.equals("maxActive")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setMaxActive(Integer.parseInt(valueArray[i]));
                }
            } else if (type.equals("maxIdle")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setMaxIdle(Integer.parseInt(valueArray[i]));
                }
            } else if (type.equals("minIdle")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setMinIdle(Integer.parseInt(valueArray[i]));
                }
            } else if (type.equals("initialSize")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setInitialSize(Integer.parseInt(valueArray[i]));
                }
            } else if (type.equals("maxWait")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setMaxWait(Long.parseLong(valueArray[i]));
                }
            } else if (type.equals("testOnBorrow")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setTestOnBorrow(Boolean.parseBoolean(valueArray[i]));
                }
            } else if (type.equals("testOnReturn")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setTestOnReturn(Boolean.parseBoolean(valueArray[i]));
                }
            } else if (type.equals("testWhileIdle")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setTestWhileIdle(Boolean.parseBoolean(valueArray[i]));
                }
            } else if (type.equals("timeBetweenEvictionRunsMillis")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setTimeBetweenEvictionRunsMillis(Long.parseLong(valueArray[i]));
                }
            } else if (type.equals("numTestsPerEvictionRun")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setNumTestsPerEvictionRun(Integer.parseInt(valueArray[i]));
                }
            } else if (type.equals("minEvictableIdleTimeMillis")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setMinEvictableIdleTimeMillis(Long.parseLong(valueArray[i]));
                }
            } else if (type.equals("refreshOnPassivate")) {
                for (int i = 0; i < Math.min(valueArray.length, repositoryCount); i++) {
                    poolingRepositoryArray[i].setRefreshOnPassivate(Boolean.parseBoolean(valueArray[i]));
                }
            }
        }
        
        for (int i = 0; i < repositoryCount; i++) {
            poolingRepositoryArray[i].setDefaultCredentials(new SimpleCredentials(poolingRepositoryArray[i].getDefaultCredentialsUserID(), poolingRepositoryArray[i].getDefaultCredentialsPassword()));
        }
        
        for (int i = 0; i < repositoryCount; i++) {
            poolingRepositoryArray[i].initialize();
        }
        
        Map<Credentials, Repository> repositoryMap = new HashMap<Credentials, Repository>();
        
        for (int i = 0; i < repositoryCount; i++) {
            repositoryMap.put(poolingRepositoryArray[i].getDefaultCredentials(), poolingRepositoryArray[i]);
        }
        
        Credentials multipleRepositoryDefaultCredentials = (repositoryCount > 0 ? poolingRepositoryArray[0].getDefaultCredentials() : null);
        
        MultipleRepository multipleRepository = new MultipleRepositoryImpl(repositoryMap, multipleRepositoryDefaultCredentials);
        
        return multipleRepository;
    }
    
    private Map<String, String []> getConfigurationMap(Reference ref) {
        Map<String, String []> configMap = new HashMap<String, String[]>();
        
        String separatorChars = ",";
        RefAddr separatorCharsRefAddr = ref.get("separatorChars");
        if (separatorCharsRefAddr != null) {
            String value = (String) separatorCharsRefAddr.getContent();
            if (value != null && !"".equals(value)) {
                separatorChars = value.trim();
            }
        }
        
        Enumeration addrs = ref.getAll();
        
        while (addrs.hasMoreElements()) {
            RefAddr addr = (RefAddr) addrs.nextElement();
            String type = addr.getType();
            String value = (String) addr.getContent();
            
            String [] valueArray = StringUtils.splitPreserveAllTokens(value, separatorChars);
            
            for (int i = 0; i < valueArray.length; i++) {
                valueArray[i] = valueArray[i].trim();
            }
            
            configMap.put(type, valueArray);
        }
        
        return configMap;
    }

}
