package org.hippoecm.hst.test;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * <p>
 * AbstractSpringTestCase
 * </p>
 * <p>
 * 
 * </p>
 * 
 * @author <a href="mailto:w.ko@onehippo.com">Woonsan Ko</a>
 * @version $Id$
 *  
 */
public abstract class AbstractSpringTestCase extends TestCase
{
    protected ApplicationContext appContext;

    /**
     * setup Spring context as part of test setup
     */
    protected void setUp() throws Exception
    {        
        super.setUp();
        this.appContext = new FileSystemXmlApplicationContext(getConfigurations());
    }

    /**
     * close Spring context as part of test teardown
     */
    protected void tearDown() throws Exception
    {
    }

    /**
     * required specification of spring configurations
     */
    protected abstract String[] getConfigurations();
    
    protected ApplicationContext getApplicationContext()
    {
        return this.appContext;
    }
}
