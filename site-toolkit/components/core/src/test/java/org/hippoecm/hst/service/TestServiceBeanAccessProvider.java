package org.hippoecm.hst.service;

import java.util.Calendar;

import junit.framework.TestCase;

import org.hippoecm.hst.proxy.ProxyUtils;

public class TestServiceBeanAccessProvider extends TestCase {

    public void testServiceBeanProxy() throws IllegalAccessException, NoSuchFieldException {
        BlogService blogService = new BlogService();

        // create proxied implementation bean for IBlog interface.
        IBlog blog = (IBlog) ProxyUtils.createBeanAccessProviderProxy(new ServiceBeanAccessProvider(blogService), IBlog.class);
        
        // Now, play with the proxied bean
        
        blog.setTitle("Wonderful HST2!");
        assertEquals("The title is not equal.", "Wonderful HST2!", blog.getTitle());
        assertEquals("The title is not equal.", "Wonderful HST2!", blogService.getProperties().get("myblog:title"));
        
        blog.setContent("HST2 is two thumbs up!");
        assertEquals("The content is not equal.", "HST2 is two thumbs up!", blog.getContent());
        assertEquals("The content is not equal.", "HST2 is two thumbs up!", blogService.getProperties().get("myblog:content"));
        
        blog.setVersion(1);
        assertEquals("The version is not equal.", 1, blog.getVersion());
        assertEquals("The version is not equal.", new Long(1), blogService.getProperties().get("hippostd:version"));
        
        blog.setWritable(true);
        assertTrue("The blog is not writable.", blog.isWritable());
        assertEquals("The blog is not writable.", new Boolean(true), blogService.getProperties().get("hippostd:writable"));
        
        Calendar now = Calendar.getInstance();
        blog.setModifiedDate(now);
        assertEquals("The modified date is not equal.", now, blog.getModifiedDate());
        assertEquals("The modified date is not equal.", now, blogService.getProperties().get("hippostd:modifiedDate"));
    }
    
}
