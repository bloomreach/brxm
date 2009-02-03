package org.hippoecm.hst.core.container;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringComponentManager implements ComponentManager {
    private ClassPathXmlApplicationContext applicationContext;

    public SpringComponentManager() {
        this.applicationContext = new ClassPathXmlApplicationContext(getConfigurations());
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
