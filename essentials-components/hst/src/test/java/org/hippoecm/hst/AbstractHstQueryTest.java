/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.repository.testutils.RepositoryTestCase;

import javax.jcr.Node;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

public class AbstractHstQueryTest extends AbstractBeanTestCase {

  protected HstQueryManager queryManager;
  protected Node baseContentNode;
  protected Node galleryContentNode;
  protected Node assetsContentNode;
  protected HippoBean baseContentBean;
  protected HippoBean galleryContentBean;
  protected HippoBean assetsContentBean;
  private MockHstRequestContext requestContext;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
        RepositoryTestCase.setUpClass();
    }

  @Before
  public void setUp() throws Exception {
      super.setUp();
      ObjectConverter objectConverter = getObjectConverter();
      queryManager = new HstQueryManagerImpl(session, objectConverter, null);
      requestContext = new MockHstRequestContext() {
          @Override
          public boolean isPreview() {
              return false;
          }
      };
      requestContext.setDefaultHstQueryManager(queryManager);
      Map<Session, HstQueryManager> nonDefaultHstQueryManagers = new HashMap<>();
      nonDefaultHstQueryManagers.put(session, queryManager);
      requestContext.setNonDefaultHstQueryManagers(nonDefaultHstQueryManagers);
      requestContext.setSession(session);
      baseContentNode = session.getNode("/unittestcontent");
      galleryContentNode = session.getNode("/unittestcontent/gallery");
      assetsContentNode = session.getNode("/unittestcontent/assets");
      baseContentBean = (HippoBean)objectConverter.getObject(baseContentNode);
      galleryContentBean = (HippoBean)objectConverter.getObject(galleryContentNode);
      assetsContentBean = (HippoBean)objectConverter.getObject(assetsContentNode);
      requestContext.setSiteContentBaseBean(baseContentBean);
      ModifiableRequestContextProvider.set(requestContext);
  }
}
