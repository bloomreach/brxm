package org.hippoecm.hst.test;

import junit.framework.TestCase;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
public abstract class AbstractSpringTestCase extends TestCase {

    protected AbstractApplicationContext appContext;

    /**
     * setup Spring context as part of test setup
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.appContext = new ClassPathXmlApplicationContext(getConfigurations(), false);
        this.appContext.refresh();
        this.appContext.start();
    }

    /**
     * close Spring context as part of test teardown
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        this.appContext.stop();
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + "*.xml";
        return new String[] { classXmlFileName };
    }

    protected Object getComponent(String name) {
        return this.appContext.getBean(name);
    }
}
