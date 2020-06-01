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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Session;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.ext.LoggerWrapper;

/**
 * TestRepositorySessionLifecycles
 * 
 * @version $Id$
 */
public class TestRepositorySessionLifecycles extends AbstractSessionPoolSpringTestCase {
    
    protected BasicPoolingRepository poolingRepository;
    protected WarningRecordingLogger recordingLogger;
   
    @Before
    public void setUp() throws Exception {
        super.setUp();
        poolingRepository = (BasicPoolingRepository) getComponent(PoolingRepository.class.getName());
        assertNotNull(poolingRepository);
        recordingLogger = new WarningRecordingLogger(poolingRepository.getLogger());
        poolingRepository.setLogger(recordingLogger);
    }
    
    @Test
    public void testWhenSessionPoolClosed() throws Exception {
        Session session = poolingRepository.login();
        poolingRepository.close();
        session.logout();
    }
    
    @Test
    public void testWarningLogsWhenEmbeddedRepositoryClosed() throws Exception {
        Session session = poolingRepository.login();
        ((JcrHippoRepository) poolingRepository.getRepository()).closeHippoRepository();
        session.logout();
        
        for (String line : recordingLogger.getLoggings())
        {
            if (line.contains("Failed to log out session."))
            {
                if (line.contains(UndeclaredThrowableException.class.getName()))
                {
                    fail("The logging message looks weird. Please use a checked exception in proxy operations. log line: " + line);
                }
            }
        }
    }
    
    private static class WarningRecordingLogger extends LoggerWrapper
    {
        private List<String> loggings = new LinkedList<String>();
        
        public WarningRecordingLogger(Logger logger)
        {
            super(logger, WarningRecordingLogger.class.getName());
        }
        
        public void warn(String msg)
        {
            loggings.add(msg);
            super.warn(msg);
        }
        
        public void warn(String format, Object arg)
        {
            loggings.add(format + ", " + Arrays.asList(arg));
            super.warn(format, arg);
        }
        
        public void warn(String format, Object [] argArray)
        {
            loggings.add(format + ", " + Arrays.asList(argArray));
            super.warn(format, argArray);
        }
        
        public void warn(String format, Object arg1, Object arg2)
        {
            loggings.add(format + ", " + Arrays.asList(arg1, arg2));
            super.warn(format, arg1, arg2);
        }
        
        public void warn(String msg, Throwable t)
        {
            StringWriter writer = new StringWriter();
            PrintWriter out = new PrintWriter(writer);
            t.printStackTrace(out);
            out.flush();
            loggings.add(msg + "\n" + writer.toString());
            super.warn(msg, t);
        }
        
        public List<String> getLoggings()
        {
            return loggings;
        }
    }
}
