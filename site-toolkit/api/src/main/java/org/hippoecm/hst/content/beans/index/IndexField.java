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

/**
 * <p>
 * Annotation that can be used on a public getter method to indicate that its return value or object
 * should be indexed. If a name is specified, the value of the name will be used as the indexing field.
 * If name is missing, the name of the getter method is used without the 'get' or 'is' part and the first letter
 * lowercased. Thus for example
 * </p>
 * <pre>
 * <code>
 *     public class NewsBean extends BaseBean{
 *
 *         @IndexField
 *         public  String getAuthor() {
 *             // return author
 *         }
 *     }
 * </code>
 * </pre>
 *
 * Would result in an index field 'author'
 *
 * </p>
 * <pre>
 * <code>
 *     public class NewsBean extends BaseBean{
 *
 *         @IndexField(name="writer")
 *         public  String getAuthor() {
 *             // return author
 *         }
 *     }
 * </code>
 * </pre>
 *
 * would result in a index field 'writer'
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface IndexField {

    public static final String DEFAULT = "#default";

    /**
     * @return Returns the field name.
     */
    String name() default DEFAULT;
}
