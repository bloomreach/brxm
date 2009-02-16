package org.hippoecm.hst.test;

import junit.framework.TestCase;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;

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
public abstract class AbstractSpringTestCase
{
    protected ComponentManager componentManager;

    @Before
    public void setUp() throws Exception {
        this.componentManager = new SpringComponentManager();
        ((SpringComponentManager) this.componentManager).setConfigurations(getConfigurations());
        
        this.componentManager.initialize();
        this.componentManager.start();
    }

    @After
    public void tearDown() throws Exception {
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

    protected <T> T getComponent(String name) {
        return this.componentManager.<T>getComponent(name);
    }
}
