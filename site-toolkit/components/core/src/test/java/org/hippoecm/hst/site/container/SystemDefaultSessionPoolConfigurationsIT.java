/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.container;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.LazyMultipleRepositoryImpl;
import org.hippoecm.hst.core.jcr.pool.MultipleRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepositoryMBean;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TestSystemDefaultSessionPoolConfigurations
 * <P>
 * Testing if the default settings of SpringComponentManager.properties are properly
 * effective on SpringComponentManager-jcr.xml as intended.
 * This test will help avoid any typos, duplicate configuration, etc.
 * </P>
 */
public class SystemDefaultSessionPoolConfigurationsIT extends AbstractSpringTestCase {

    private MultipleRepository multipleRepository;

    private Credentials defaultCreds;
    private Credentials previewCreds;
    private Credentials writableCreds;
    private Credentials binariesCreds;
    private Credentials hstconfigreaderCreds;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        multipleRepository = getComponent(Repository.class.getName());
        assertNotNull(multipleRepository);

        defaultCreds = getComponent(Credentials.class.getName() + ".default");
        assertNotNull(defaultCreds);

        previewCreds = getComponent(Credentials.class.getName() + ".preview");
        assertNotNull(previewCreds);

        writableCreds = getComponent(Credentials.class.getName() + ".writable");
        assertNotNull(writableCreds);

        binariesCreds = getComponent(Credentials.class.getName() + ".binaries");
        assertNotNull(binariesCreds);

        hstconfigreaderCreds = getComponent(Credentials.class.getName() + ".hstconfigreader");
        assertNotNull(hstconfigreaderCreds);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testDefaultSettingsOfDefaultSessionPool() throws Exception {
        BasicPoolingRepository repo = (BasicPoolingRepository) multipleRepository.getRepositoryByCredentials(defaultCreds);
        assertNotNull(repo);

        assertEquals(100, repo.getMaxActive());
        assertEquals(25, repo.getMaxIdle());
        assertEquals(0, repo.getMinIdle());
        assertEquals(0, repo.getInitialSize());
        assertEquals(10000, repo.getMaxWait());
        assertEquals(PoolingRepositoryMBean.WHEN_EXHAUSTED_BLOCK, repo.getWhenExhaustedAction());
        assertTrue(repo.getTestOnBorrow());
        assertFalse(repo.getTestOnReturn());
        assertFalse(repo.getTestWhileIdle());
        assertEquals(60000, repo.getTimeBetweenEvictionRunsMillis());
        assertEquals(1, repo.getNumTestsPerEvictionRun());
        assertEquals(300000, repo.getMinEvictableIdleTimeMillis());
        assertTrue(repo.getRefreshOnPassivate());
        assertEquals(300000, repo.getMaxRefreshIntervalOnPassivate());
        assertEquals(3600000, repo.getMaxTimeToLiveMillis());
    }

    @Test
    public void testDefaultSettingsOfPreviewSessionPool() throws Exception {
        BasicPoolingRepository repo = (BasicPoolingRepository) multipleRepository.getRepositoryByCredentials(previewCreds);
        assertNotNull(repo);

        assertEquals(100, repo.getMaxActive());
        assertEquals(5, repo.getMaxIdle());
        assertEquals(0, repo.getMinIdle());
        assertEquals(0, repo.getInitialSize());
        assertEquals(10000, repo.getMaxWait());
        assertEquals(PoolingRepositoryMBean.WHEN_EXHAUSTED_BLOCK, repo.getWhenExhaustedAction());
        assertTrue(repo.getTestOnBorrow());
        assertFalse(repo.getTestOnReturn());
        assertFalse(repo.getTestWhileIdle());
        assertEquals(60000, repo.getTimeBetweenEvictionRunsMillis());
        assertEquals(1, repo.getNumTestsPerEvictionRun());
        assertEquals(300000, repo.getMinEvictableIdleTimeMillis());
        assertTrue(repo.getRefreshOnPassivate());
        assertEquals(300000, repo.getMaxRefreshIntervalOnPassivate());
        assertEquals(3600000, repo.getMaxTimeToLiveMillis());
    }

    @Test
    public void testDefaultSettingsOfWritableSessionPool() throws Exception {
        BasicPoolingRepository repo = (BasicPoolingRepository) multipleRepository.getRepositoryByCredentials(writableCreds);
        assertNotNull(repo);

        assertEquals(100, repo.getMaxActive());
        assertEquals(5, repo.getMaxIdle());
        assertEquals(0, repo.getMinIdle());
        assertEquals(0, repo.getInitialSize());
        assertEquals(10000, repo.getMaxWait());
        assertEquals(PoolingRepositoryMBean.WHEN_EXHAUSTED_BLOCK, repo.getWhenExhaustedAction());
        assertTrue(repo.getTestOnBorrow());
        assertFalse(repo.getTestOnReturn());
        assertFalse(repo.getTestWhileIdle());
        assertEquals(60000, repo.getTimeBetweenEvictionRunsMillis());
        assertEquals(1, repo.getNumTestsPerEvictionRun());
        assertEquals(300000, repo.getMinEvictableIdleTimeMillis());
        assertTrue(repo.getRefreshOnPassivate());
        assertEquals(300000, repo.getMaxRefreshIntervalOnPassivate());
        assertEquals(3600000, repo.getMaxTimeToLiveMillis());
    }

    @Test
    public void testDefaultSettingsOfBinariesSessionPool() throws Exception {
        BasicPoolingRepository repo = (BasicPoolingRepository) multipleRepository.getRepositoryByCredentials(binariesCreds);
        assertNotNull(repo);

        assertEquals(100, repo.getMaxActive());
        assertEquals(10, repo.getMaxIdle());
        assertEquals(0, repo.getMinIdle());
        assertEquals(0, repo.getInitialSize());
        assertEquals(10000, repo.getMaxWait());
        assertEquals(PoolingRepositoryMBean.WHEN_EXHAUSTED_BLOCK, repo.getWhenExhaustedAction());
        assertTrue(repo.getTestOnBorrow());
        assertFalse(repo.getTestOnReturn());
        assertFalse(repo.getTestWhileIdle());
        assertEquals(60000, repo.getTimeBetweenEvictionRunsMillis());
        assertEquals(1, repo.getNumTestsPerEvictionRun());
        assertEquals(300000, repo.getMinEvictableIdleTimeMillis());
        assertTrue(repo.getRefreshOnPassivate());
        assertEquals(300000, repo.getMaxRefreshIntervalOnPassivate());
        assertEquals(3600000, repo.getMaxTimeToLiveMillis());
    }

    @Test
    public void testDefaultSettingsOfHstConfigReaderSessionPool() throws Exception {
        BasicPoolingRepository repo = (BasicPoolingRepository) multipleRepository.getRepositoryByCredentials(hstconfigreaderCreds);
        assertNotNull(repo);

        assertEquals(25, repo.getMaxActive());
        assertEquals(5, repo.getMaxIdle());
        assertEquals(0, repo.getMinIdle());
        assertEquals(0, repo.getInitialSize());
        assertEquals(10000, repo.getMaxWait());
        assertEquals(PoolingRepositoryMBean.WHEN_EXHAUSTED_BLOCK, repo.getWhenExhaustedAction());
        assertTrue(repo.getTestOnBorrow());
        assertFalse(repo.getTestOnReturn());
        assertFalse(repo.getTestWhileIdle());
        assertEquals(60000, repo.getTimeBetweenEvictionRunsMillis());
        assertEquals(1, repo.getNumTestsPerEvictionRun());
        assertEquals(300000, repo.getMinEvictableIdleTimeMillis());
        assertTrue(repo.getRefreshOnPassivate());
        assertEquals(300000, repo.getMaxRefreshIntervalOnPassivate());
        assertEquals(3600000, repo.getMaxTimeToLiveMillis());
    }

    @Test
    public void testDefaultSettingsOfDisposableSessionPool() throws Exception {
        LazyMultipleRepositoryImpl repo = (LazyMultipleRepositoryImpl) multipleRepository;
        Map<String, String> configMap = repo.getDefaultConfigMap();

        assertEquals(5, NumberUtils.toInt(configMap.get("maxActive")));
        assertEquals(5, NumberUtils.toInt(configMap.get("maxIdle")));
        assertEquals(0, NumberUtils.toInt(configMap.get("minIdle")));
        assertEquals(0, NumberUtils.toInt(configMap.get("initialSize")));
        assertEquals(10000, NumberUtils.toLong(configMap.get("maxWait")));
        assertEquals(PoolingRepositoryMBean.WHEN_EXHAUSTED_BLOCK, configMap.get("whenExhaustedAction"));
        assertTrue(BooleanUtils.toBoolean(configMap.get("testOnBorrow")));
        assertFalse(BooleanUtils.toBoolean(configMap.get("testOnReturn")));
        assertFalse(BooleanUtils.toBoolean(configMap.get("testWhileIdle")));
        assertEquals(10000, NumberUtils.toLong(configMap.get("timeBetweenEvictionRunsMillis")));
        assertEquals(1, NumberUtils.toInt(configMap.get("numTestsPerEvictionRun")));
        assertEquals(180000, NumberUtils.toLong(configMap.get("minEvictableIdleTimeMillis")));
        assertTrue(BooleanUtils.toBoolean(configMap.get("refreshOnPassivate")));
        assertEquals(300000, NumberUtils.toLong(configMap.get("maxRefreshIntervalOnPassivate")));
    }
}
