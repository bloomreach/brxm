/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

/**
 * The marker interface for all beans that can be indexed (thus also compounds): This includes beans that are completely
 * independent of jcr, The beans implementing this {@link ContentBean} don't need a {@link IdentifiableContentBean#getIdentifier()}
 * identifier. Beans that should be possible to be indexed in something like an inversed index, need to implement {@link IdentifiableContentBean}.
 * Typically classes that implement this {@link ContentBean} but not {@link IdentifiableContentBean} are compounds of an {@link IdentifiableContentBean}
 *
 * For example:
 *
 * <pre>
 * <code>
 *     public class NewsBean implements IdentifiableContentBean {
 *
 *
 *         public  String getPath() {
 *             // return path
 *         }
 *
 *        public  void setPath(String path) {
 *            // set path
 *        }
 *
 *        public Author getAuthor() {
 *            // return author
 *        }
 *
 *     }
 *
 *     public class Author implements ContentBean {
 *
 *         public String getName() {
 *             // return name
 *         }
 *
 *     }
 *
 * </code>
 * </pre>
 *
 */
public interface ContentBean {

}
