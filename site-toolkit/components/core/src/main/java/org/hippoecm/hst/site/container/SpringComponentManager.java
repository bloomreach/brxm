package org.hippoecm.hst.site.container;

import java.util.Properties;

import org.hippoecm.hst.core.container.ComponentManager;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringComponentManager implements ComponentManager {
    
    protected AbstractApplicationContext applicationContext;
    protected Properties initProps;
    protected String [] configurations;

    public SpringComponentManager() {
        this(null);
    }
    
    public SpringComponentManager(Properties initProps) {
        this.initProps = initProps;
    }
    
    public void initialize() {
        String [] configurations = getConfigurations();
        
        if (null == configurations) {
            String classXmlFileName = getClass().getName().replace(".", "/") + "*.xml";
            configurations = new String[] { classXmlFileName };            
        }

        this.applicationContext = new ClassPathXmlApplicationContext(configurations, false);
        
        if (this.initProps != null) {
            PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
            ppc.setIgnoreUnresolvablePlaceholders(true);
            ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
            ppc.setProperties(this.initProps);
            this.applicationContext.addBeanFactoryPostProcessor(ppc);
        }
        
        this.applicationContext.refresh();
    }

    public void start() {
        this.applicationContext.start();
    }

    public void stop() {
        this.applicationContext.stop();
    }
    
    public void close() {
        this.applicationContext.close();
    }

    public <T> T getComponent(String name) {
        return (T) this.applicationContext.getBean(name);
    }

    public String[] getConfigurations() {
        return this.configurations;
    }
    
    public void setConfigurations(String [] configurations) {
        this.configurations = configurations;
    }
}
