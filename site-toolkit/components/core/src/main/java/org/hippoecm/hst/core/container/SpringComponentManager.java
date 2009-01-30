package org.hippoecm.hst.core.container;

import org.springframework.context.support.FileSystemXmlApplicationContext;

public class SpringComponentManager implements ComponentManager
{
    private FileSystemXmlApplicationContext applicationContext;
    
    public SpringComponentManager(String [] appConfigs)
    {
        FileSystemXmlApplicationContext appContext = new FileSystemXmlApplicationContext(appConfigs, false);
        appContext.refresh();
        this.applicationContext = appContext;
    }
    
    public void init()
    {
        this.applicationContext.refresh();
    }
    
    public Object getComponent(String name)
    {
        return this.applicationContext.getBean(name);
    }
}
