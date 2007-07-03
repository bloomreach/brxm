package org.hippocms.repository.webapp;

import javax.jcr.Session;

import org.apache.wicket.RequestCycle;

public class JcrSessionLocator
{
    public static Session getSession()
    {
        Main main = (Main)RequestCycle.get().getApplication();
        return main.getSession();
    }
}
