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

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * <p>Basic implementation of <code>javax.jcr.Repository</code> that is
 * configured via JavaBeans properties.</p>
 *
 * @version $Id$
 */
public class BasicPoolingRepository implements PoolingRepository, MultipleRepositoryAware {
    
    private static Logger log = LoggerFactory.getLogger(BasicPoolingRepository.class);
    
    protected Repository repository;
    protected Credentials defaultCredentials;           // credentials provided by the configuration or the user
    protected Credentials internalDefaultCredentials;   // credentials used for real JCR API invocations.
    protected boolean isSimpleDefaultCredentials;
    protected SessionDecorator sessionDecorator;
    
    protected String repositoryProviderClassName = "org.hippoecm.hst.core.jcr.pool.JcrHippoRepositoryProvider";
    protected JcrRepositoryProvider jcrRepositoryProvider;
    protected String validRepositoryAddressPattern = "^[^\\$]*$";
    protected String repositoryAddress;
    protected String defaultCredentialsUserID;
    protected char [] defaultCredentailsPassword;
    
    protected boolean refreshOnPassivate = true;
    protected boolean keepChangesOnRefresh = false;
    protected long sessionsRefreshPendingTimeMillis; 
    protected ResourceLifecycleManagement pooledSessionLifecycleManagement;
    protected MultipleRepository multipleRepository;

    public void setRepositoryProviderClassName(String repositoryProviderClassName) {
        this.repositoryProviderClassName = repositoryProviderClassName;
    }
    
    public String getRepositoryProviderClassName() {
        return this.repositoryProviderClassName;
    }
    
    public void setRepository(Repository repository) throws RepositoryException {
        this.repository = repository;
    }

    public Repository getRepository() throws RepositoryException {
        return this.repository;
    }
    
    public void setValidRepositoryAddressPattern(String validRepositoryAddressPattern) {
        this.validRepositoryAddressPattern = validRepositoryAddressPattern;
    }
    
    public String getValidRepositoryAddressPattern() {
        return validRepositoryAddressPattern;
    }
    
    public void setRepositoryAddress(String repositoryAddress) {
        if (repositoryAddress != null && validRepositoryAddressPattern != null) {
            if (!Pattern.matches(validRepositoryAddressPattern, repositoryAddress)) {
                throw new IllegalArgumentException("Illegal repository address: " + repositoryAddress);
            }
        }
        
        this.repositoryAddress = repositoryAddress;
    }
    
    public String getRepositoryAddress() {
        return this.repositoryAddress;
    }

    public void setDefaultCredentials(Credentials defaultCredentials) {
        isSimpleDefaultCredentials = (defaultCredentials != null && (defaultCredentials instanceof SimpleCredentials));
        internalDefaultCredentials = this.defaultCredentials = defaultCredentials;
        
        if (isSimpleDefaultCredentials) {
            String userID = ((SimpleCredentials) defaultCredentials).getUserID();
            String userIDOnly = StringUtils.substringBefore(userID, "@");
            
            if (!userID.equals(userIDOnly)) {
                internalDefaultCredentials = new SimpleCredentials(userIDOnly, ((SimpleCredentials) defaultCredentials).getPassword());
            }
        }
    }

    public Credentials getDefaultCredentials() {
        return this.defaultCredentials;
    }
    
    public void setDefaultCredentialsUserID(String defaultCredentialsUserID) {
        this.defaultCredentialsUserID = defaultCredentialsUserID;
    }
    
    public String getDefaultCredentialsUserID() {
        return this.defaultCredentialsUserID;
    }
    
    public void setDefaultCredentialsPassword(char [] defaultCredentailsPassword) {
        this.defaultCredentailsPassword = defaultCredentailsPassword;
    }
    
    public char [] getDefaultCredentialsPassword() {
        return this.defaultCredentailsPassword;
    }
    
    public void setRefreshOnPassivate(boolean refreshOnPassivate) {
        this.refreshOnPassivate = refreshOnPassivate;
    }
    
    public boolean getRefreshOnPassivate() {
        return this.refreshOnPassivate;
    }
    
    public void setKeepChangesOnRefresh(boolean keepChangesOnRefresh) {
        this.keepChangesOnRefresh = keepChangesOnRefresh;
    }
    
    public boolean getKeepChangesOnRefresh() {
        return this.keepChangesOnRefresh;
    }
    
    public void setSessionsRefreshPendingAfter(long sessionsRefreshPendingTimeMillis) {
        this.sessionsRefreshPendingTimeMillis = sessionsRefreshPendingTimeMillis;
    }
    
    public long getSessionsRefreshPendingAfter() {
        return this.sessionsRefreshPendingTimeMillis;
    }

    public void setSessionDecorator(SessionDecorator sessionDecorator) {
        this.sessionDecorator = sessionDecorator;

        if (this.sessionDecorator != null && (this.sessionDecorator instanceof PoolingRepositoryAware)) {
            ((PoolingRepositoryAware) this.sessionDecorator).setPoolingRepository(this);
        }
    }

    public SessionDecorator getSessionDecorator() {
        return this.sessionDecorator;
    }
    
    public void setResourceLifecycleManagement(ResourceLifecycleManagement pooledSessionLifecycleManagement) {
        this.pooledSessionLifecycleManagement = pooledSessionLifecycleManagement;
        
        if (this.pooledSessionLifecycleManagement != null && this.pooledSessionLifecycleManagement instanceof PoolingRepositoryAware) {
            ((PoolingRepositoryAware) this.pooledSessionLifecycleManagement).setPoolingRepository(this);
        }
    }
    
    public ResourceLifecycleManagement getResourceLifecycleManagement() {
        return this.pooledSessionLifecycleManagement;
    }

    public String getDescriptor(String key) {
        String descriptor = null;

        try {
            descriptor = getRepository().getDescriptor(key);
        } catch (RepositoryException e) {
        }

        return descriptor;
    }

    public String[] getDescriptorKeys() {
        String[] descriptorKeys = null;

        try {
            descriptorKeys = getRepository().getDescriptorKeys();
        } catch (RepositoryException e) {
        }

        return descriptorKeys;
    }

    /**
     * <strong>BasicPoolingRepository will return a session by this method.</strong>
     *
     * @throws LoginException
     * @throws RepositoryException
     * @return a read-only session
     */
    public Session login() throws LoginException, RepositoryException {
        Session session = null;

        try {
            session = (Session) this.sessionPool.borrowObject();
        } catch (NoSuchElementException e) {
            throw new NoAvailableSessionException("No session is available now. Probably the session pool was exhasuted.");
        } catch (Exception e) {
            throw new LoginException("Failed to borrow session from the pool.", e);
        }

        return session;
    }

    /**
     * <strong>BasicPoolingRepository will return a session by this method.</strong>
     *
     * @throws LoginException
     * @throws RepositoryException
     * @return a writable session
     */
    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        if (equalsCredentials(credentials)) {
            return login();
        } else {
            throw new LoginException("login by credentials other than the default is not supported.");
        }
    }

    /**
     * <strong>BasicPoolingRepository does not support workspaceName parameter. So it returns a normal session.</strong>
     *
     * @throws LoginException
     * @throws RepositoryException
     * @return a read-only session
     */
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login();
    }

    /**
     * <strong>BasicPoolingRepository does not support workspaceName parameter. So it returns a writable session.</strong>
     *
     * @throws LoginException
     * @throws RepositoryException
     * @return a writable session
     */
    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        return login(credentials);
    }
    
    public void returnSession(Session session) {
        try {
            this.sessionPool.returnObject(session);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to return session to the pool.", e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to return session to the pool. {}", e.toString());
            }
        }
        
        if (this.sessionPool.getNumActive() < 0) {
            log.error("SEVERE: The session pool is broken with negative active session count. {}", this.sessionPool.getNumActive());
        }
    }
    
    public void setMultipleRepository(MultipleRepository multipleRepository) {
        this.multipleRepository = multipleRepository;
    }
    
    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        Session session = null;
        
        if (this.multipleRepository != null && this.multipleRepository.containsRepositoryByCredentials(credentials)) {
            session = this.multipleRepository.login(credentials);
        }
        
        return session;
    }

    // Pool implementation

    /**
     * The object pool that internally manages our sessions.
     */
    protected GenericObjectPool sessionPool;
    /**
     * The maximum number of active sessions that can be allocated from
     * this pool at the same time, or negative for no limit.
     */
    protected int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    /**
     * The maximum number of sessions that can remain idle in the
     * pool, without extra ones being released, or negative for no limit.
     */
    protected int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    /**
     * The minimum number of active sessions that can remain idle in the
     * pool, without extra ones being created, or 0 to create none.
     */
    protected int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;
    /**
     * The behavior of borrowing a session when the pool is exhausted.
     * <ul>
     *   <li>
     *     When {@link #whenExhaustedAction} is {@link PoolingRepository#WHEN_EXHAUSTED_FAIL}, 
     *     borrowing will throw a {@link NoAvailableSessionException}.
     *   </li>
     *   <li>
     *     When {@link #whenExhaustedAction} is {@link PoolingRepository#WHEN_EXHAUSTED_GROW}, 
     *     borrowing will create a new session and return it, 
     *     essentially making {@link #maxActive} meaningless.
     *   </li>
     *   <li>
     *     When {@link #whenExhaustedAction} is {@link PoolingRepository#WHEN_EXHAUSTED_BLOCK}, 
     *     borrowing will block until a new or idle session is available. 
     *     If a positive {@link #maxWait} value is supplied, 
     *     borrowing will block for at most that many milliseconds, 
     *     after which a {@link NoAvailableSessionException} will be thrown.
     *     If {@link #maxWait} is non-positive, borrowing will block indefinitely.
     *   </li>
     * </ul> 
     */
    protected String whenExhaustedAction = WHEN_EXHAUSTED_BLOCK;
    /**
     * The initial number of sessions that are created when the pool
     * is started.
     */
    protected int initialSize = 0;
    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available sessions) for a session to be returned before
     * throwing an exception, or <= 0 to wait indefinitely.
     */
    protected long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;
    /**
     * The indication of whether objects will be validated before being
     * borrowed from the pool.  If the object fails to validate, it will be
     * dropped from the pool, and we will attempt to borrow another.
     */
    protected boolean testOnBorrow = true;
    /**
     * The indication of whether objects will be validated before being
     * returned to the pool.
     */
    protected boolean testOnReturn = false;
    /**
     * The number of milliseconds to sleep between runs of the idle object
     * evictor thread.  When non-positive, no idle object evictor thread will
     * be run.
     */
    protected long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
    /**
     * The number of objects to examine during each run of the idle object
     * evictor thread (if any).
     */
    protected int numTestsPerEvictionRun = GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
    /**
     * The minimum amount of time an object may sit idle in the pool before it
     * is eligable for eviction by the idle object evictor (if any).
     */
    protected long minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    /**
     * The indication of whether objects will be validated by the idle object
     * evictor (if any).  If an object fails to validate, it will be dropped
     * from the pool.
     */
    protected boolean testWhileIdle = false;
    /**
     * The query that will be used to validate sessions from this pool
     * before returning them to the caller.  If specified, this query
     * <strong>MUST</strong> be a valid statement.
     */
    protected String validationQuery = null;
    
    public synchronized void initialize() throws Exception {
        doClose();
        doInitialize();
    }
    
    private void doInitialize() throws Exception {
        if (getRepository() == null && getRepositoryProviderClassName() != null) {
            try {
                this.jcrRepositoryProvider = (JcrRepositoryProvider) Class.forName(getRepositoryProviderClassName()).newInstance();
            } catch (Exception e) {
                throw new RepositoryException("Cannot create an instance of JcrRepositoryProvider: " + getRepositoryProviderClassName());
            }
            
            Repository repository = this.jcrRepositoryProvider.getRepository(getRepositoryAddress());
            setRepository(repository);
        }
        
        if (getDefaultCredentials() == null && getDefaultCredentialsUserID() != null) {
            setDefaultCredentials(new SimpleCredentials(getDefaultCredentialsUserID(), getDefaultCredentialsPassword()));
        }
        
        setSessionDecorator(new PooledSessionDecoratorProxyFactoryImpl());
        
        if (getResourceLifecycleManagement() == null) {
            setResourceLifecycleManagement(new PooledSessionResourceManagement());
        }

        // Initialize the object pool
        
        sessionPool = new GenericObjectPool(new SessionFactory());

        sessionPool.setMaxActive(maxActive);
        sessionPool.setMaxIdle(maxIdle);
        sessionPool.setMinIdle(minIdle);
        
        byte whenExhaustedActionFlag = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
        String whenExhaustedActionParam = new StringBuilder().append(this.getWhenExhaustedAction()).toString();
        
        if (WHEN_EXHAUSTED_FAIL.equalsIgnoreCase(whenExhaustedActionParam)) {
            whenExhaustedActionFlag = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
        } else if (WHEN_EXHAUSTED_GROW.equalsIgnoreCase(whenExhaustedActionParam)) {
            whenExhaustedActionFlag = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        }
        
        sessionPool.setWhenExhaustedAction(whenExhaustedActionFlag);
        sessionPool.setMaxWait(maxWait);
        sessionPool.setTestOnBorrow(testOnBorrow);
        sessionPool.setTestOnReturn(testOnReturn);
        sessionPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        sessionPool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        sessionPool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        sessionPool.setTestWhileIdle(testWhileIdle);

        if (initialSize > 0) {
            PoolUtils.prefill(sessionPool, initialSize);
        }
    }
    
    /**
     * Close and release all sessions that are currently stored in the
     * session pool associated with our data source.  All open (active)
     * session remain open until closed.  Once the pooling repository has
     * been closed, no more sessions can be obtained.
     * @throws Exception 
     */
    public synchronized void close() throws Exception {
        doClose();
    }
    
    private void doClose() throws Exception {
        if (this.sessionPool != null) {
            try {
                this.sessionPool.close();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to close session pool.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to close session pool. {}", e.toString());
                }
            }
            
            this.sessionPool = null;
        }
        
        if (this.repository != null && this.jcrRepositoryProvider != null) {
            try {
                this.jcrRepositoryProvider.returnRepository(this.repository);
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to return repository.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to return repository. {}", e.toString());
                }
            }
            
            this.repository = null;
            this.jcrRepositoryProvider = null;
        }
    }
    
    /**
     * <p>Returns the maximum number of active connections that can be
     * allocated at the same time.
     * </p>
     * <p>A negative number means that there is no limit.</p>
     * 
     * @return the maximum number of active connections
     */
    public synchronized int getMaxActive() {
        return this.maxActive;
    }

    /**
     * Sets the maximum number of active connections that can be
     * allocated at the same time. Use a negative value for no limit.
     * 
     * @param maxActive the new value for maxActive
     * @see #getMaxActive()
     */
    public synchronized void setMaxActive(int maxActive) {
        this.maxActive = maxActive;

        if (sessionPool != null) {
            sessionPool.setMaxActive(maxActive);
        }
    }

    /**
     * <p>Returns the maximum number of connections that can remain idle in the
     * pool.
     * </p>
     * <p>A negative value indicates that there is no limit</p>
     * 
     * @return the maximum number of idle connections
     */
    public synchronized int getMaxIdle() {
        return this.maxIdle;
    }

    /**
     * Sets the maximum number of connections that can remain idle in the
     * pool.
     * 
     * @see #getMaxIdle()
     * @param maxIdle the new value for maxIdle
     */
    public synchronized void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;

        if (sessionPool != null) {
            sessionPool.setMaxIdle(maxIdle);
        }
    }

    /**
     * Returns the minimum number of idle connections in the pool
     * 
     * @return the minimum number of idle connections
     * @see GenericObjectPool#getMinIdle()
     */
    public synchronized int getMinIdle() {
        return this.minIdle;
    }

    /**
     * Sets the minimum number of idle connections in the pool.
     * 
     * @param minIdle the new value for minIdle
     * @see GenericObjectPool#setMinIdle(int)
     */
    public synchronized void setMinIdle(int minIdle) {
        this.minIdle = minIdle;

        if (sessionPool != null) {
            sessionPool.setMinIdle(minIdle);
        }
    }

    /**
     * Returns the initial size of the connection pool.
     * 
     * @return the number of connections created when the pool is initialized
     */
    public synchronized int getInitialSize() {
        return this.initialSize;
    }

    /**
     * <p>Sets the initial size of the connection pool.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     * 
     * @param initialSize the number of connections created when the pool
     * is initialized
     */
    public synchronized void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    /**
     * <p>Returns the maximum number of milliseconds that the pool will wait
     * for a connection to be returned before throwing an exception.
     * </p>
     * <p>A value less than or equal to zero means the pool is set to wait
     * indefinitely.</p>
     * 
     * @return the maxWait property value
     */
    public synchronized long getMaxWait() {
        return this.maxWait;
    }

    /**
     * <p>Sets the maxWait property.
     * </p>
     * <p>Use -1 to make the pool wait indefinitely.
     * </p>
     * 
     * @param maxWait the new value for maxWait
     * @see #getMaxWait()
     */
    public synchronized void setMaxWait(long maxWait) {
        this.maxWait = maxWait;

        if (sessionPool != null) {
            sessionPool.setMaxWait(maxWait);
        }
    }

    /**
     * Returns the {@link #testOnBorrow} property.
     * 
     * @return true if objects are validated before being borrowed from the
     * pool
     * 
     * @see #testOnBorrow
     */
    public synchronized boolean getTestOnBorrow() {
        return this.testOnBorrow;
    }

    /**
     * Sets the {@link #testOnBorrow} property. This property determines
     * whether or not the pool will validate objects before they are borrowed
     * from the pool. For a <code>true</code> value to have any effect, the 
     * <code>validationQuery</code> property must be set to a non-null string.
     * 
     * @param testOnBorrow new value for testOnBorrow property
     */
    public synchronized void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;

        if (sessionPool != null) {
            sessionPool.setTestOnBorrow(testOnBorrow);
        }
    }

    /**
     * Returns the value of the {@link #testOnReturn} property.
     * 
     * @return true if objects are validated before being returned to the
     * pool
     * @see #testOnReturn
     */
    public synchronized boolean getTestOnReturn() {
        return this.testOnReturn;
    }

    /**
     * Sets the <code>testOnReturn</code> property. This property determines
     * whether or not the pool will validate objects before they are returned
     * to the pool. For a <code>true</code> value to have any effect, the 
     * <code>validationQuery</code> property must be set to a non-null string.
     * 
     * @param testOnReturn new value for testOnReturn property
     */
    public synchronized void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;

        if (sessionPool != null) {
            sessionPool.setTestOnReturn(testOnReturn);
        }
    }

    /**
     * Returns the value of the {@link #timeBetweenEvictionRunsMillis}
     * property.
     * 
     * @return the time (in miliseconds) between evictor runs
     * @see #timeBetweenEvictionRunsMillis
     */
    public synchronized long getTimeBetweenEvictionRunsMillis() {
        return this.timeBetweenEvictionRunsMillis;
    }

    /**
     * Sets the {@link #timeBetweenEvictionRunsMillis} property.
     * 
     * @param timeBetweenEvictionRunsMillis the new time between evictor runs
     * @see #timeBetweenEvictionRunsMillis
     */
    public synchronized void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;

        if (sessionPool != null) {
            sessionPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
    }

    /**
     * Returns the value of the {@link #numTestsPerEvictionRun} property.
     * 
     * @return the number of objects to examine during idle object evictor
     * runs
     * @see #numTestsPerEvictionRun
     */
    public synchronized int getNumTestsPerEvictionRun() {
        return this.numTestsPerEvictionRun;
    }

    /**
     * Sets the value of the {@link #numTestsPerEvictionRun} property.
     * 
     * @param numTestsPerEvictionRun the new {@link #numTestsPerEvictionRun} 
     * value
     * @see #numTestsPerEvictionRun
     */
    public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;

        if (sessionPool != null) {
            sessionPool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        }
    }

    /**
     * Returns the {@link #minEvictableIdleTimeMillis} property.
     * 
     * @return the value of the {@link #minEvictableIdleTimeMillis} property
     * @see #minEvictableIdleTimeMillis
     */
    public synchronized long getMinEvictableIdleTimeMillis() {
        return this.minEvictableIdleTimeMillis;
    }

    /**
     * Sets the {@link #minEvictableIdleTimeMillis} property.
     * 
     * @param minEvictableIdleTimeMillis the minimum amount of time an object
     * may sit idle in the pool 
     * @see #minEvictableIdleTimeMillis
     */
    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;

        if (sessionPool != null) {
            sessionPool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        }
    }

    /**
     * Returns the value of the {@link #testWhileIdle} property.
     * 
     * @return true if objects examined by the idle object evictor are
     * validated
     * @see #testWhileIdle
     */
    public synchronized boolean getTestWhileIdle() {
        return this.testWhileIdle;
    }

    /**
     * Sets the <code>testWhileIdle</code> property. This property determines
     * whether or not the idle object evictor will validate connections.  For a
     * <code>true</code> value to have any effect, the 
     * <code>validationQuery</code> property must be set to a non-null string.
     * 
     * @param testWhileIdle new value for testWhileIdle property
     */
    public synchronized void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;

        if (sessionPool != null) {
            sessionPool.setTestWhileIdle(testWhileIdle);
        }
    }

    /**
     * [Read Only] The current number of active connections that have been
     * allocated from this data source.
     * 
     * @return the current number of active connections
     */
    public synchronized int getNumActive() {
        if (sessionPool != null) {
            return sessionPool.getNumActive();
        } else {
            return 0;
        }
    }

    /**
     * [Read Only] The current number of idle connections that are waiting
     * to be allocated from this data source.
     * 
     * @return the current number of idle connections
     */
    public synchronized int getNumIdle() {
        if (sessionPool != null) {
            return sessionPool.getNumIdle();
        } else {
            return 0;
        }
    }

    /**
     * Returns the validation query used to validate connections before
     * returning them.
     * 
     * @return the SQL validation query
     * @see #validationQuery
     */
    public synchronized String getValidationQuery() {
        return this.validationQuery;
    }

    /** 
     * <p>Sets the {@link #validationQuery}.</p>
     * <p>
     * Note: this method currently has no effect once the pool has been
     * initialized.  The pool is initialized the first time one of the
     * following methods is invoked: <code>getConnection, setLogwriter,
     * setLoginTimeout, getLoginTimeout, getLogWriter.</code></p>
     * 
     * @param validationQuery the new value for the validation query
     */
    public synchronized void setValidationQuery(String validationQuery) {
        this.validationQuery = (validationQuery != null ? validationQuery.trim() : null);
    }

    /**
     * Returns the action when the pool is exhausted
     * returning them.
     * 
     * @return the action when the pool is exhausted
     * @see #whenExhaustedAction
     */
    public synchronized String getWhenExhaustedAction() {
        return this.whenExhaustedAction;
    }
    
    /** 
     * <p>Sets the {@link #whenExhaustedAction}.</p>
     * 
     * @param whenExhaustedAction the new value for the action when the pool is exhausted
     */
    public synchronized void setWhenExhaustedAction(String whenExhaustedAction) {
        this.whenExhaustedAction = (whenExhaustedAction != null ? whenExhaustedAction.trim() : WHEN_EXHAUSTED_BLOCK);
    }
    
    private boolean equalsCredentials(Credentials credentials) {
        if (isSimpleDefaultCredentials && (credentials instanceof SimpleCredentials)) {
            SimpleCredentials other = (SimpleCredentials) credentials;
            return ((SimpleCredentials) this.defaultCredentials).getUserID().equals(other.getUserID());
        } else {
            return this.defaultCredentials.equals(credentials);
        }
    }
    
    private class SessionFactory implements PoolableObjectFactory {
        
        public void activateObject(Object object) throws Exception {
            // If sessionRefreshPendingAfter is set to specific time millis,
            // each session should be refreshed if it is not refreshed after the time millis.
            if (object instanceof PooledSession) {
                PooledSession session = (PooledSession) object;
                session.activate();
            
                if (sessionsRefreshPendingTimeMillis > 0L) { 
                    if (session.lastRefreshed() < sessionsRefreshPendingTimeMillis) {  
                        session.refresh(keepChangesOnRefresh);
                    }
                }
            }
            
            // If client retrieves a session, then register it as a disposable. 
            if (pooledSessionLifecycleManagement != null && pooledSessionLifecycleManagement.isActive()) {
                pooledSessionLifecycleManagement.registerResource(object);
            }
        }

        public void destroyObject(Object object) throws Exception {
            Session session = (Session) object;

            try {
                if (session instanceof PooledSession) {
                    ((PooledSession) session).logoutSession();
                } else {
                    session.logout();
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to log out session.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to log out session. {}", e.toString());
                }
            }
        }

        public Object makeObject() throws Exception {
            Session session = getRepository().login(internalDefaultCredentials);
            
            if (session != null && sessionDecorator != null) {
                session = sessionDecorator.decorate(session, getDefaultCredentialsUserID());
            }
            
            return session;
        }

        public void passivateObject(Object object) throws Exception {
            PooledSession session = (PooledSession) object;
            
            if (refreshOnPassivate) {
                session.refresh(keepChangesOnRefresh);
            }
            
            session.passivate();
            
            // If client returns the session he used, then unregister it 
            if (pooledSessionLifecycleManagement != null && pooledSessionLifecycleManagement.isActive()) {
                pooledSessionLifecycleManagement.unregisterResource(object);
            }
        }

        public boolean validateObject(Object object) {
            Session session = (Session) object;
            boolean validated = session.isLive();

            if (!validated) {
                return validated;
            }

            String validationQuery = getValidationQuery();

            if (validationQuery != null) {
                Node nodeFound = null;

                try {
                    if ("".equals(validationQuery)) {
                        nodeFound = session.getRootNode();
                    } else {
                        nodeFound = session.getRootNode().getNode(validationQuery);
                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to find validation query node.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to find validation query node. {}", e.toString());
                    }
                }

                if (nodeFound == null) {
                    validated = false;
                }
            }

            return validated;
        }
    }

}
