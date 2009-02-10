package org.hippoecm.hst.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.hippoecm.hst.proxy.ProxyUtils;
import org.junit.Test;

public class TestServiceBeanAccessProvider {

    @Test
    public void testServiceBeanProxy() throws IllegalAccessException, NoSuchFieldException {
        BlogService blogService = new BlogService();

        // create proxied implementation bean for IBlog interface.
        IBlogArticle blog = (IBlogArticle) ProxyUtils.createBeanAccessProviderProxy(new ServiceBeanAccessProviderImpl(blogService), IBlogArticle.class);
        
        // Now, play with the proxied bean
        
        blog.setTitle("Wonderful HST2!");
        assertEquals("The title is not equal.", "Wonderful HST2!", blog.getTitle());
        assertEquals("The title is not equal.", "Wonderful HST2!", blogService.getProperties().get("myproject:title"));
        
        blog.setContent("HST2 is two thumbs up!");
        assertEquals("The content is not equal.", "HST2 is two thumbs up!", blog.getContent());
        assertEquals("The content is not equal.", "HST2 is two thumbs up!", blogService.getProperties().get("myproject:content"));
        
        String [] comments = new String [] { "good article!", "thanks!" };
        blog.setComments(comments);
        assertEquals("The comment is not equal.", "good article!", blog.getComments()[0]);
        assertEquals("The comment is not equal.", "thanks!", blog.getComments()[1]);
        String [] commentsFromProps = (String []) blogService.getProperties().get("myproject:comments");
        assertEquals("The comment is not equal.", "good article!", commentsFromProps[0]);
        assertEquals("The comment is not equal.", "thanks!", commentsFromProps[1]);
        
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
