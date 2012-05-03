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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

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
public class BasicPoolingRepository implements PoolingRepository, PoolingRepositoryMBean, MultipleRepositoryAware {
    
    private Logger log = LoggerFactory.getLogger(BasicPoolingRepository.class);
    
    private volatile boolean active;
    private boolean closableWhenNotInUse;
    private volatile boolean closingWhenNotInUse;
    
    private Repository repository;
    private Credentials defaultCredentials;           // credentials provided by the configuration or the user
    private Credentials internalDefaultCredentials;   // credentials used for real JCR API invocations.
    private boolean isSimpleDefaultCredentials;
    private SessionDecorator sessionDecorator;
    
    private String repositoryProviderClassName = "org.hippoecm.hst.core.jcr.pool.JcrHippoRepositoryProvider";
    private JcrRepositoryProvider jcrRepositoryProvider;
    private String repositoryAddress;
    private String defaultCredentialsUserID;
    private String defaultCredentialsUserIDSeparator = String.valueOf('\uFFFF');
    private char [] defaultCredentailsPassword;
    private String defaultWorkspaceName;
    
    private boolean refreshOnPassivate = true;
    private long maxRefreshIntervalOnPassivate;
    private boolean keepChangesOnRefresh = false;
    private long sessionsRefreshPendingTimeMillis;
    
    private PooledSessionRefresher pooledSessionRefresher;
    
    private ResourceLifecycleManagement pooledSessionLifecycleManagement;
    private MultipleRepository multipleRepository;
    
    private PoolingCounter poolingCounter;
    
    public void setLogger(Logger log) {
        this.log = log;
    }
    
    public Logger getLogger() {
        return log;
    }
    
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
    
    public void setRepositoryAddress(String repositoryAddress) {
        this.repositoryAddress = repositoryAddress;
    }
    
    public String getRepositoryAddress() {
        return this.repositoryAddress;
    }

    public void setDefaultCredentials(Credentials defaultCredentials) {
        isSimpleDefaultCredentials = (defaultCredentials != null && (defaultCredentials instanceof SimpleCredentials));
        this.defaultCredentials = defaultCredentials;
        internalDefaultCredentials = defaultCredentials;
        
        if (isSimpleDefaultCredentials) {
            String userID = ((SimpleCredentials) defaultCredentials).getUserID();
            String userIDOnly = StringUtils.substringBefore(userID, defaultCredentialsUserIDSeparator);
            
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
    
    public void setDefaultCredentialsUserIDSeparator(String defaultCredentialsUserIDSeparator) {
        this.defaultCredentialsUserIDSeparator = defaultCredentialsUserIDSeparator;
    }
    
    public String getDefaultCredentialsUserIDSeparator() {
        return this.defaultCredentialsUserIDSeparator;
    }
    
    public void setDefaultCredentialsPassword(char [] defaultCredentailsPassword) {
        if (defaultCredentailsPassword == null) {
            this.defaultCredentailsPassword = null;
        } else {
            this.defaultCredentailsPassword = new char [defaultCredentailsPassword.length];
            System.arraycopy(defaultCredentailsPassword, 0, this.defaultCredentailsPassword, 0, defaultCredentailsPassword.length);
        }
    }
    
    public char [] getDefaultCredentialsPassword() {
        if (defaultCredentailsPassword == null) {
            return null;
        }
        
        char [] value = new char[defaultCredentailsPassword.length];
        System.arraycopy(defaultCredentailsPassword, 0, value, 0, defaultCredentailsPassword.length);
        return value;
    }
    
    public void setDefaultWorkspaceName(String defaultWorkspaceName) {
        this.defaultWorkspaceName = defaultWorkspaceName;
    }
    
    public String getDefaultWorkspaceName() {
        return defaultWorkspaceName;
    }
    
    public void setRefreshOnPassivate(boolean refreshOnPassivate) {
        this.refreshOnPassivate = refreshOnPassivate;
    }
    
    public boolean getRefreshOnPassivate() {
        return this.refreshOnPassivate;
    }
    
    public void setMaxRefreshIntervalOnPassivate(long maxRefreshIntervalOnPassivate) {
        this.maxRefreshIntervalOnPassivate = maxRefreshIntervalOnPassivate;
    }
    
    public long getMaxRefreshIntervalOnPassivate() {
        return maxRefreshIntervalOnPassivate;
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

    public PooledSessionRefresher getPooledSessionRefresher() {
        return pooledSessionRefresher;
    }

    public void setPooledSessionRefresher(PooledSessionRefresher pooledSessionRefresher) {
        this.pooledSessionRefresher = pooledSessionRefresher;
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
        
        if (this.pooledSessionLifecycleManagement instanceof PoolingRepositoryAware) {
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
            log.error("RepositoryException: ",e);
        }

        return descriptor;
    }

    public String[] getDescriptorKeys() {
        String[] descriptorKeys = null;
        try {
            descriptorKeys = getRepository().getDescriptorKeys();
        } catch (RepositoryException e) {
            log.error("RepositoryException: ",e);
        }

        return descriptorKeys;
    }

    public Value getDescriptorValue(String key) {
        try {
            return getRepository().getDescriptorValue(key);
        } catch (RepositoryException e) {
            log.error("RepositoryException: ",e);
        }
        return null;
    }

    public Value[] getDescriptorValues(String key) {
        try {
            return getRepository().getDescriptorValues(key);
        } catch (RepositoryException e) {
            log.error("RepositoryException: ",e);
        }
        return null;
    }

    public boolean isSingleValueDescriptor(String key) {
        try {
            return getRepository().isSingleValueDescriptor(key);
        } catch (RepositoryException e) {
            log.error("RepositoryException: ",e);
        }
        return false;
    }

    public boolean isStandardDescriptor(String key) {
        try {
            return getRepository().isStandardDescriptor(key);
        } catch (RepositoryException e) {
            log.error("RepositoryException: ",e);
        }
        return false;
    }

    /**
     * <strong>BasicPoolingRepository will return a session by this method.</strong>
     *
     * @throws LoginException
     * @throws RepositoryException
     * @return a read-only session
     */
    public Session login() throws LoginException, RepositoryException {
        if (sessionPool == null || !isActive()) {
            throw new RepositoryException("The session pool of the pooling repository has not been initialized or closed.");
        }
        
        Session session = null;
        
        try {
            session = (Session) sessionPool.borrowObject();
            
            // If client retrieves a session, then register it as a disposable. 
            if (pooledSessionLifecycleManagement != null && pooledSessionLifecycleManagement.isActive()) {
                pooledSessionLifecycleManagement.registerResource(session);
            }
            
            if (poolingCounter != null) {
                poolingCounter.sessionObtained();
            }
        } catch (NoSuchElementException e) {
            throw new NoAvailableSessionException("No session is available now. Probably the session pool was exhausted.");
        } catch (Exception e) {
            throw new LoginException("Failed to borrow session from the pool. " + e, e);
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
        if (sessionPool == null) {
            if (log.isDebugEnabled()) {
                log.debug("The session pool of the pooling repository has not been initialized yet.");
            }
            
            return;
        }
        
        try {
            this.sessionPool.returnObject(session);
            
            if (poolingCounter != null) {
                poolingCounter.sessionReturned();
            }
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
    private GenericObjectPool sessionPool;
    /**
     * The maximum number of active sessions that can be allocated from
     * this pool at the same time, or negative for no limit.
     */
    private int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    /**
     * The maximum number of sessions that can remain idle in the
     * pool, without extra ones being released, or negative for no limit.
     */
    private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    /**
     * The minimum number of active sessions that can remain idle in the
     * pool, without extra ones being created, or 0 to create none.
     */
    private int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;
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
    private String whenExhaustedAction = WHEN_EXHAUSTED_BLOCK;
    /**
     * The initial number of sessions that are created when the pool
     * is started.
     */
    private int initialSize = 0;
    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available sessions) for a session to be returned before
     * throwing an exception, or <= 0 to wait indefinitely.
     */
    private long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;
    /**
     * The indication of whether objects will be validated before being
     * borrowed from the pool.  If the object fails to validate, it will be
     * dropped from the pool, and we will attempt to borrow another.
     */
    private boolean testOnBorrow = true;
    /**
     * The indication of whether objects will be validated before being
     * returned to the pool.
     */
    private boolean testOnReturn = false;
    /**
     * The number of milliseconds to sleep between runs of the idle object
     * evictor thread.  When non-positive, no idle object evictor thread will
     * be run.
     */
    private long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
    /**
     * The number of objects to examine during each run of the idle object
     * evictor thread (if any).
     */
    private int numTestsPerEvictionRun = GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
    /**
     * The minimum amount of time an object may sit idle in the pool before it
     * is eligable for eviction by the idle object evictor (if any).
     */
    private long minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    /**
     * The indication of whether objects will be validated by the idle object
     * evictor (if any).  If an object fails to validate, it will be dropped
     * from the pool.
     */
    private boolean testWhileIdle = false;
    /**
     * The query that will be used to validate sessions from this pool
     * before returning them to the caller.  If specified, this query
     * <strong>MUST</strong> be a valid statement.
     */
    protected String validationQuery = null;
    
    public synchronized boolean isActive() {
        return active;
    }
    
    public boolean isClosableWhenNotInUse() {
        return closableWhenNotInUse;
    }
    
    void setClosableWhenNotInUse(boolean closableWhenNotInUse) {
        this.closableWhenNotInUse = closableWhenNotInUse;
    }
    
    public synchronized boolean isClosingWhenNotInUse() {
        return closingWhenNotInUse;
    }
    
    synchronized void setClosingWhenNotInUse(boolean closingWhenNotInUse) {
        this.closingWhenNotInUse = closingWhenNotInUse;
    }
    
    public synchronized void initialize() throws RepositoryException {
        doClose();
        doInitialize();
        active = true;
    }
    
    private void doInitialize() throws RepositoryException {
        if (getRepository() == null && getRepositoryProviderClassName() != null) {
            try {
                this.jcrRepositoryProvider = (JcrRepositoryProvider) Class.forName(getRepositoryProviderClassName()).newInstance();
            } catch (Exception e) {
                throw new RepositoryException("Cannot create an instance of JcrRepositoryProvider: " + getRepositoryProviderClassName(), e);
            }
            
            Repository jcrrepository = this.jcrRepositoryProvider.getRepository(getRepositoryAddress());
            setRepository(jcrrepository);
        }
        
        if (getDefaultCredentials() == null && getDefaultCredentialsUserID() != null) {
            setDefaultCredentials(new SimpleCredentials(getDefaultCredentialsUserID(), getDefaultCredentialsPassword()));
        }
        
        setSessionDecorator(new PooledSessionDecoratorProxyFactoryImpl());
        
        if (getPooledSessionRefresher() == null) {
            setPooledSessionRefresher(new DefaultPooledSessionRefresher());
        }
        
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
            try {
                PoolUtils.prefill(sessionPool, initialSize);
            } catch (Exception e) {
                throw new RepositoryException("Failed to prefill initial sessions.", e);
            }
        }
    }

    /*
     * Clears any sessions sitting idle in the pool by removing them from the idle instance pool.
     * */
    public void clear() {
        if (this.sessionPool != null) {
            try {
                this.sessionPool.clear();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to clear session pool.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to clear session pool. {}", e.toString());
                }
            }
        }
    }
    
    /**
     * Close and release all sessions that are currently stored in the
     * session pool associated with our data source.  All open (active)
     * session remain open until closed.  Once the pooling repository has
     * been closed, no more sessions can be obtained.
     * @throws Exception 
     */
    public synchronized void close() {
        active = false;
        doClose();
    }
    
    private void doClose() {
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
    public int getMaxActive() {
        return this.maxActive;
    }

    /**
     * Sets the maximum number of active connections that can be
     * allocated at the same time. Use a negative value for no limit.
     * 
     * @param maxActive the new value for maxActive
     * @see #getMaxActive()
     */
    public void setMaxActive(int maxActive) {
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
    public int getMaxIdle() {
        return this.maxIdle;
    }

    /**
     * Sets the maximum number of connections that can remain idle in the
     * pool.
     * 
     * @see #getMaxIdle()
     * @param maxIdle the new value for maxIdle
     */
    public void setMaxIdle(int maxIdle) {
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
    public int getMinIdle() {
        return this.minIdle;
    }

    /**
     * Sets the minimum number of idle connections in the pool.
     * 
     * @param minIdle the new value for minIdle
     * @see GenericObjectPool#setMinIdle(int)
     */
    public void setMinIdle(int minIdle) {
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
    public int getInitialSize() {
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
    public void setInitialSize(int initialSize) {
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
    public long getMaxWait() {
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
    public void setMaxWait(long maxWait) {
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
    public boolean getTestOnBorrow() {
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
    public void setTestOnBorrow(boolean testOnBorrow) {
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
    public boolean getTestOnReturn() {
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
    public void setTestOnReturn(boolean testOnReturn) {
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
    public long getTimeBetweenEvictionRunsMillis() {
        return this.timeBetweenEvictionRunsMillis;
    }

    /**
     * Sets the {@link #timeBetweenEvictionRunsMillis} property.
     * 
     * @param timeBetweenEvictionRunsMillis the new time between evictor runs
     * @see #timeBetweenEvictionRunsMillis
     */
    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
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
    public int getNumTestsPerEvictionRun() {
        return this.numTestsPerEvictionRun;
    }

    /**
     * Sets the value of the {@link #numTestsPerEvictionRun} property.
     * 
     * @param numTestsPerEvictionRun the new {@link #numTestsPerEvictionRun} 
     * value
     * @see #numTestsPerEvictionRun
     */
    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
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
    public long getMinEvictableIdleTimeMillis() {
        return this.minEvictableIdleTimeMillis;
    }

    /**
     * Sets the {@link #minEvictableIdleTimeMillis} property.
     * 
     * @param minEvictableIdleTimeMillis the minimum amount of time an object
     * may sit idle in the pool 
     * @see #minEvictableIdleTimeMillis
     */
    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
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
    public boolean getTestWhileIdle() {
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
    public void setTestWhileIdle(boolean testWhileIdle) {
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
    public int getNumActive() {
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
    public int getNumIdle() {
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
    public String getValidationQuery() {
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
    public void setValidationQuery(String validationQuery) {
        this.validationQuery = (validationQuery != null ? validationQuery.trim() : null);
    }

    /**
     * Returns the action when the pool is exhausted
     * returning them.
     * 
     * @return the action when the pool is exhausted
     * @see #whenExhaustedAction
     */
    public String getWhenExhaustedAction() {
        return this.whenExhaustedAction;
    }
    
    /** 
     * <p>Sets the {@link #whenExhaustedAction}.</p>
     * 
     * @param whenExhaustedAction the new value for the action when the pool is exhausted
     */
    public void setWhenExhaustedAction(String whenExhaustedAction) {
        this.whenExhaustedAction = (whenExhaustedAction != null ? whenExhaustedAction.trim() : WHEN_EXHAUSTED_BLOCK);
    }
    
    public PoolingCounter getPoolingCounter() {
        return poolingCounter;
    }
    
    public void setPoolingCounter(PoolingCounter poolingCounter) {
        this.poolingCounter = poolingCounter;
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
        
        public void activateObject(Object object) throws RepositoryException {
            // If sessionRefreshPendingAfter is set to specific time millis,
            // each session should be refreshed if it is not refreshed after the time millis.
            if (object instanceof PooledSession) {
                PooledSession session = (PooledSession) object;
                session.activate();
                
                if (poolingCounter != null) {
                    poolingCounter.sessionActivated();
                }
            
                if (sessionsRefreshPendingTimeMillis > 0L) { 
                    if (session.lastRefreshed() < sessionsRefreshPendingTimeMillis) {  
                        getPooledSessionRefresher().refresh(session, keepChangesOnRefresh);
                    }
                }
            }
        }

        public void destroyObject(Object object) throws RepositoryException {
            Session session = (Session) object;

            try {
                if (session instanceof PooledSession) {
                    PooledSession pooledSession = (PooledSession) session;
                    // passivate session first, not to return session multiple times
                    // because GenericObjectPool does not invoke passivate()
                    // when validation fails during returning object.
                    pooledSession.passivate();
                    pooledSession.logoutSession();
                } else {
                    // HSTTWO-1337: Hippo Repository requires to check isLive() before logout(), refresh(), etc.
                    if (session.isLive()) {
                        session.logout();
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to log out session.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to log out session. {}", e.toString());
                }
            }
            
            if (poolingCounter != null) {
                poolingCounter.sessionDestroyed();
            }
        }

        public Object makeObject() throws RepositoryException {
            Session session = null;
            
            if (internalDefaultCredentials == null && defaultWorkspaceName == null) {
                session = getRepository().login();
            } else if (internalDefaultCredentials != null && defaultWorkspaceName == null) {
                session = getRepository().login(internalDefaultCredentials);
            } else if (internalDefaultCredentials != null && defaultWorkspaceName != null) {
                session = getRepository().login(internalDefaultCredentials, defaultWorkspaceName);
            } else if (internalDefaultCredentials == null && defaultWorkspaceName != null) {
                session = getRepository().login(defaultWorkspaceName);
            }
            
            if (session != null && sessionDecorator != null) {
                session = sessionDecorator.decorate(session, getDefaultCredentialsUserID());
            }
            
            if (poolingCounter != null) {
                poolingCounter.sessionCreated();
            }
            
            return session;
        }

        public void passivateObject(Object object) throws RepositoryException {
            Session session = (Session) object;
            
            if (session instanceof PooledSession) {
                PooledSession pooledSession = (PooledSession) object;
                
                if (refreshOnPassivate) {
                    if (maxRefreshIntervalOnPassivate > 0L) {
                        if (System.currentTimeMillis() - pooledSession.lastRefreshed() > maxRefreshIntervalOnPassivate) {
                            getPooledSessionRefresher().refresh(pooledSession, keepChangesOnRefresh);
                        }
                    } else {
                        getPooledSessionRefresher().refresh(pooledSession, keepChangesOnRefresh);
                    }
                }
                
                pooledSession.passivate();
                
                if (poolingCounter != null) {
                    poolingCounter.sessionPassivated();
                }
            }
        }

        public boolean validateObject(Object object) {
            Session session = (Session) object;
            boolean validated = false;
            
            try {
                validated = session.isLive();
            } catch (Exception ignore) {
            }
            
            if (!validated) {
                return validated;
            }
            
            String validationQueryValue = getValidationQuery();

            if (validationQueryValue != null) {
                Node nodeFound = null;

                try {
                    if ("".equals(validationQueryValue)) {
                        nodeFound = session.getRootNode();
                    } else {
                        nodeFound = session.getRootNode().getNode(validationQueryValue);
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
