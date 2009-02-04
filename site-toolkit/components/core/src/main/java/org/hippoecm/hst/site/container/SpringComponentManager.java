package org.hippoecm.hst.site.container;

import java.util.Properties;

import org.hippoecm.hst.core.container.ComponentManager;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringComponentManager implements ComponentManager {
    
    private AbstractApplicationContext applicationContext;

    public SpringComponentManager() {
        this(null);
    }
    
    public SpringComponentManager(Properties initProps) {
        this.applicationContext = new ClassPathXmlApplicationContext(getConfigurations(), false);
        
        if (initProps != null) {
            PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
            ppc.setIgnoreUnresolvablePlaceholders(true);
            ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
            ppc.setProperties(initProps);
            this.applicationContext.addBeanFactoryPostProcessor(ppc);
        }
    }

    public void start() {
        this.applicationContext.refresh();
        this.applicationContext.start();
    }

    public void stop() {
        this.applicationContext.stop();
    }

    public Object getComponent(String name) {
        return this.applicationContext.getBean(name);
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + "*.xml";
        return new String[] { classXmlFileName };
    }
}
