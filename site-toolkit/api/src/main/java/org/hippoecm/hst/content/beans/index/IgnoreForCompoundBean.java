/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.content.beans.index;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented

/**
 * <b>Expert</b>
 * <p>
 * Makes it possible to annotate a method that should not be indexed for compound {@link org.hippoecm.hst.content.beans.standard.ContentBean}s. 
 * For example when you have the bean structure below, you do not need the getPath, canonicalUUID, etc etc from the Author to be indexed. 
 * By adding this {@link IgnoreForCompoundBean} annotation, the method will be skipped when indexing a compound bean into its container bean
 * </p>
 * <pre>
 * <code>
 *     public class NewsBean implements IdendifiableContentBean {
 *
 *         @IndexField
 *         public  String getPath() {
 *             // return path
 *         }
 *
 *        public  void setPath(String path) {
 *            // set path
 *        }
 *        @IndexField
 *        public Author getAuthor() {
 *            // return author
 *        }
 *
 *     }
 *
 *     // the Compound
 *     public class Author implements IdendifiableContentBean {
 *
 *         public String getName() {
 *             // return name
 *         }
 *
 *     }
 *
 *     // and the HippoBean has something like
 *     public interface IdendifiableContentBean {
 *
 *        @IgnoreForCompoundBean
 *        @IndexField
 *        public String getPath();
 *
 *        public void setPath(String path);
 *
 *     }
 *
 * </code>
 * </pre>
 * 
 * 
 * 
 */
public @interface IgnoreForCompoundBean {

}
