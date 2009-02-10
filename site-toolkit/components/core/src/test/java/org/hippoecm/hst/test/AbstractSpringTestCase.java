package org.hippoecm.hst.test;

import junit.framework.TestCase;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.container.SpringComponentManager;

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

    protected ComponentManager componentManager;

    /**
     * setup Spring context as part of test setup
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        this.componentManager = new SpringComponentManager();
        ((SpringComponentManager) this.componentManager).setConfigurations(getConfigurations());
        
        this.componentManager.initialize();
        this.componentManager.start();
    }

    /**
     * close Spring context as part of test teardown
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        
        this.componentManager.stop();
        this.componentManager.close();
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
        return this.componentManager.getComponent(name);
    }
}
