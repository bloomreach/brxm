package org.hippoecm.hst.service;

import java.util.Calendar;

import junit.framework.TestCase;

import org.hippoecm.hst.proxy.BeanAccessProvider;
import org.hippoecm.hst.proxy.ProxyUtils;

public class TestServiceBeanAccessProvider extends TestCase {

    public void testServiceBeanProxy() throws IllegalAccessException, NoSuchFieldException {
        BlogService blogService = new BlogService();

        // create proxied implementation bean for IBlog interface.
        BeanAccessProvider provider = new ServiceBeanAccessProvider(blogService);
        IBlog blog = (IBlog) ProxyUtils.createBeanAccessProviderProxy(new Class [] { IBlog.class }, provider);
        
        // Now, play with the proxied bean
        
        blog.setTitle("Wonderful HST2!");
        assertEquals("The title is not equal.", "Wonderful HST2!", blog.getTitle());
        
        blog.setContent("HST2 is two thumbs up!");
        assertEquals("The content is not equal.", "HST2 is two thumbs up!", blog.getContent());
        
        blog.setVersion(1);
        assertEquals("The version is not equal.", 1, blog.getVersion());
        
        blog.setWritable(true);
        assertTrue("The blog is not writable.", blog.isWritable());
        
        Calendar now = Calendar.getInstance();
        blog.setModifiedDate(now);
        assertEquals("The modified date is not equal.", now, blog.getModifiedDate());
    }
    
}
