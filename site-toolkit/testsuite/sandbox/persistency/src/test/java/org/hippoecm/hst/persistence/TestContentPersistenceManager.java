/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.hippoecm.hst.mock.MockContentPersistenceManager;
import org.junit.Before;
import org.junit.Test;

public class TestContentPersistenceManager {
    
    private ContentPersistenceManager cpm;
    private Comment comment1;
    private Comment comment2;
    
    @Before
    public void setUp() throws Exception {
        cpm = new MockContentPersistenceManager();
        
        comment1 = new Comment("/content/blog/comments/comment1", "comment1 - title", "comment1 - content");
        ((MockContentPersistenceManager) cpm).setObject(comment1.getPath(), comment1);
        comment2 = new Comment("/content/blog/comments/comment2", "comment2 - title", "comment2 - content");
        ((MockContentPersistenceManager) cpm).setObject(comment2.getPath(), comment2);
    }

    @Test
    public void testBasicUsage() throws Exception {
        Comment testComment1 = (Comment) cpm.getObject("/content/blog/comments/comment1");
        assertEquals(comment1, testComment1);
        Comment testComment2 = (Comment) cpm.getObject("/content/blog/comments/comment2");
        assertEquals(comment2, testComment2);
        
        testComment1.setTitle("testcomment1 - title");
        testComment1.setContent("testcomment1 - content");
        
        cpm.update(testComment1);
        
        Comment testComment12 = (Comment) cpm.getObject("/content/blog/comments/comment1");
        assertFalse(comment1.equals(testComment12));
        assertEquals(testComment1, testComment12);
        
        cpm.remove(testComment1);
        Comment testComment13 = (Comment) cpm.getObject("/content/blog/comments/comment1");
        assertTrue(testComment13 == null);
        
        cpm.save();
    }
    
    public static class Comment implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private String path;
        private String title;
        private String content;
        
        public Comment(String path, String title, String content) {
            this.path = path;
            this.title = title;
            this.content = content;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getContent() {
            return content;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other instanceof Comment) {
                if (this.title != null && this.content != null) {
                    return (this.title.equals(((Comment) other).title) && this.content.equals(((Comment) other).content));
                }
            }
            return false;
        }
        
        @Override
        public String toString() {
            return "Title: " + title + ", Content: " + content;
        }
        
        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    }
    
}
