package org.hippoecm.hst;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Before;

import javax.jcr.*;
import javax.jcr.Node;
import java.util.HashMap;
import java.util.Map;

public class AbstractHstQueryTest extends AbstractBeanTestCase {
  protected HstQueryManager queryManager;
  protected javax.jcr.Node baseContentNode;
  protected Node galleryContentNode;
  protected Node assetsContentNode;
  protected HippoBean baseContentBean;
  protected HippoBean galleryContentBean;
  protected HippoBean assetsContentBean;
  private MockHstRequestContext requestContext;

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
