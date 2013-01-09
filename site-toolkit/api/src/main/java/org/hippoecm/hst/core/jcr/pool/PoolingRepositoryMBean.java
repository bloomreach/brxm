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


/**
 * PoolingRepositoryMBean
 * @version $Id$
 */
public interface PoolingRepositoryMBean {
    
    /**
     * When the sessions in the pool are exhausted, the pool will be blocked for the specified interval
     * to wait for available idle session.
     */
    String WHEN_EXHAUSTED_BLOCK = "block";
    
    /**
     * When the sessions in the pool are exhausted, the pool will throw exception instantly without
     * waiting for available idle session. 
     */
    String WHEN_EXHAUSTED_FAIL = "fail";
    
    /**
     * When the sessions in the pool are exhausted, the pool will grow the action session count to serve
     * the request. This option will make the max active count limit meaningless.
     */
    String WHEN_EXHAUSTED_GROW = "grow";
    
    /**
     * The key name of the counter which counts session creation.
     */
    String COUNTER_SESSION_CREATED = "Created";
    
    /**
     * The key name of the counter which counts session activation.
     */
    String COUNTER_SESSION_ACTIVATED = "Activated";
    
    /**
     * The key name of the counter which counts session obtained by login.
     */
    String COUNTER_SESSION_OBTAINED = "Obtained";
    
    /**
     * The key name of the counter which counts session returned by logout.
     */
    String COUNTER_SESSION_RETURNED = "Returned";
    
    /**
     * The key name of the counter which counts session passivation.
     */
    String COUNTER_SESSION_PASSIVATED = "Passivated";
    
    /**
     * The key name of the counter which counts session destroying.
     */
    String COUNTER_SESSION_DESTROYED = "Destroyed";
    
    /**
     * Initializes the pool
     * @throws Exception
     */
    void initialize() throws Exception;
    
    /**
     * Clears any sessions sitting idle in the pool by removing them from the idle instance pool.
     */
    void clear();
    
    /**
     * Closes the pool
     */
    void close() throws Exception;
    
    /**
     * Returns the current active session count in the pool.
     * @return
     */
    int getNumActive();

    /**
     * Returns the current idle session count in the pool.
     * @return
     */
    int getNumIdle();

    /**
     * Returns the initial size of the connection pool.
     * @return
     */
    int getInitialSize();
    
    /**
     * Sets the initial size of the connection pool.
     * @param initialSize the number of connections created when the pool is initialized
     */
    void setInitialSize(int initialSize);
    
    /**
     * Returns the maximum number of active connections that can be allocated at the same time.
     * @return
     */
    int getMaxActive();
    
    /**
     * Sets the maximum number of active connections that can be
     * allocated at the same time. Use a negative value for no limit.
     * @param maxActive the new value for maxActive
     * @see #getMaxActive()
     */
    void setMaxActive(int maxActive);
    
    /**
     * Returns the maximum number of connections that can remain idle in the pool. 
     * @return
     */
    int getMaxIdle();
    
    /**
     * Sets the maximum number of connections that can remain idle in the pool.
     * @see #getMaxIdle()
     * @param maxIdle the new value for maxIdle
     */
    void setMaxIdle(int maxIdle);
    
    /**
     * Returns the minimum number of idle connections in the pool
     * @return
     */
    int getMinIdle();
    
    /**
     * Sets the minimum number of idle connections in the pool.
     * @param minIdle the new value for minIdle
     */
    void setMinIdle(int minIdle);
    
    /**
     * Returns the maximum number of milliseconds that the pool will wait
     * for a connection to be returned before throwing an exception.
     * <p>A value less than or equal to zero means the pool is set to wait
     * indefinitely.</p>
     * @return the maxWait property value
     */
    long getMaxWait();
    
    /**
     * Sets the maxWait property.
     * <p>Use -1 to make the pool wait indefinitely.</p>
     * @param maxWait the new value for maxWait
     * @see #getMaxWait()
     */
    void setMaxWait(long maxWait);
    
    /**
     * Returns the {@link #minEvictableIdleTimeMillis} property.
     * @return the value of the {@link #minEvictableIdleTimeMillis} property
     * @see #minEvictableIdleTimeMillis
     */
    long getMinEvictableIdleTimeMillis();
    
    /**
     * Sets the {@link #minEvictableIdleTimeMillis} property.
     * @param minEvictableIdleTimeMillis the minimum amount of time an object
     * may sit idle in the pool 
     * @see #minEvictableIdleTimeMillis
     */
    void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis);
    
    /**
     * Returns the value of the {@link #numTestsPerEvictionRun} property.
     * @return the number of objects to examine during idle object evictor runs
     * @see #numTestsPerEvictionRun
     */
    int getNumTestsPerEvictionRun();
    
    /**
     * Sets the value of the {@link #numTestsPerEvictionRun} property.
     * @param numTestsPerEvictionRun the new {@link #numTestsPerEvictionRun} value
     * @see #numTestsPerEvictionRun
     */
    void setNumTestsPerEvictionRun(int numTestsPerEvictionRun);
    
    /**
     * Returns the {@link #testOnBorrow} property.
     * @return true if objects are validated before being borrowed from the pool
     * @see #testOnBorrow
     */
    boolean getTestOnBorrow();
    
    /**
     * Sets the {@link #testOnBorrow} property. This property determines
     * whether or not the pool will validate objects before they are borrowed
     * from the pool. For a <code>true</code> value to have any effect, the 
     * <code>validationQuery</code> property must be set to a non-null string.
     * @param testOnBorrow new value for testOnBorrow property
     */
    void setTestOnBorrow(boolean testOnBorrow);
    
    /**
     * Returns the value of the {@link #testOnReturn} property.
     * @return true if objects are validated before being returned to the pool
     * @see #testOnReturn
     */
    boolean getTestOnReturn();
    
    /**
     * Sets the <code>testOnReturn</code> property. This property determines
     * whether or not the pool will validate objects before they are returned
     * to the pool. For a <code>true</code> value to have any effect, the 
     * <code>validationQuery</code> property must be set to a non-null string.
     * @param testOnReturn new value for testOnReturn property
     */
    void setTestOnReturn(boolean testOnReturn);
    
    /**
     * Returns the value of the {@link #testWhileIdle} property.
     * @return true if objects examined by the idle object evictor are validated
     * @see #testWhileIdle
     */
    boolean getTestWhileIdle();
    
    /**
     * Sets the <code>testWhileIdle</code> property. This property determines
     * whether or not the idle object evictor will validate connections.  For a
     * <code>true</code> value to have any effect, the 
     * <code>validationQuery</code> property must be set to a non-null string.
     * @param testWhileIdle new value for testWhileIdle property
     */
    void setTestWhileIdle(boolean testWhileIdle);
    
    /**
     * Returns the value of the {@link #timeBetweenEvictionRunsMillis} property.
     * @return the time (in miliseconds) between evictor runs
     * @see #timeBetweenEvictionRunsMillis
     */
    long getTimeBetweenEvictionRunsMillis();
    
    /**
     * Sets the {@link #timeBetweenEvictionRunsMillis} property.
     * @param timeBetweenEvictionRunsMillis the new time between evictor runs
     * @see #timeBetweenEvictionRunsMillis
     */
    void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis);
    
    /**
     * Returns the validation query used to validate connections before
     * returning them.
     * @return the JCR validation query
     * @see #validationQuery
     */
    String getValidationQuery();
    
    /** 
     * <p>Sets the {@link #validationQuery}.</p>
     * @param validationQuery the new value for the JCR validation query
     */
    void setValidationQuery(String validationQuery);
    
    /**
     * Returns the action when the pool is exhausted returning them.
     * @return the action when the pool is exhausted
     * @see #whenExhaustedAction
     */
    String getWhenExhaustedAction();
    
    /** 
     * <p>Sets the {@link #whenExhaustedAction}.</p>
     * @param whenExhaustedAction the new value for the action when the pool is exhausted
     */
    void setWhenExhaustedAction(String whenExhaustedAction);
    
}
