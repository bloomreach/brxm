package org.hippoecm.hst.content.beans;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.assertj.core.api.Assertions;
import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.campaign.DocumentCampaignService;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.repository.campaign.Campaign;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSIONS_META;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;

public class TestCampaignBeanSupport extends AbstractBeanTestCase {


    @Before
    public void setUp() throws Exception {
        super.setUp();

        MockHstRequestContext mockHstRequestContext = new MockHstRequestContext();
        mockHstRequestContext.setSession(session);
        mockHstRequestContext.setContentTypes(HippoServiceRegistry.getService(ContentTypeService.class).getContentTypes());
        ModifiableRequestContextProvider.set(mockHstRequestContext);

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        ModifiableRequestContextProvider.clear();
    }

    @Test
    public void testSimpleObjectGetting() throws Exception {

        ObjectConverter objectConverter = createObjectConverter();

        final Node handle = session.getNode("/unittestcontent/documents/unittestproject/common/homepage");

        final DocumentCampaignServiceDummy documentCampaignServiceDummy = new DocumentCampaignServiceDummy();
        try {
            HippoServiceRegistry.register(documentCampaignServiceDummy, DocumentCampaignService.class);

            handle.addMixin(NT_HIPPO_VERSION_INFO);
            handle.setProperty(HIPPO_VERSIONS_META, "my-uuid");

            // although the object converter will fail to get the object for frozen node UUID equal to 'my-uudi', the
            // purpose of this test is to show that the object converter uses the DocumentCampaignService to find which
            // version to render
            try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(ObjectConverterImpl.class).build()) {
                objectConverter.getObject(session, "/unittestcontent/documents/unittestproject/common/homepage");

                final List<String> collect = interceptor.messages().collect(Collectors.toList());
                Assertions.assertThat(collect)
                        .as("Expected that an attempt was made to render a campaign version")
                        .containsExactly("Found frozenNode uuid 'my-uuid' to render for '/unittestcontent/documents/unittestproject/common/homepage'",
                                "Failed to return a frozen node version for active campaign 'my-uuid' for handle '/unittestcontent/documents/unittestproject/common/homepage'. Fallback to serve the right branch.");

            }

        } finally {
            HippoServiceRegistry.unregister(documentCampaignServiceDummy, DocumentCampaignService.class);
            handle.refresh(false);
        }
    }

    static class DocumentCampaignServiceDummy implements DocumentCampaignService {
        @Override
        public Optional<Campaign> findActiveCampaign(final Node handle, final String branchId) {
            try {
                return Optional.of(new Campaign(handle.getProperty(HIPPO_VERSIONS_META).getString(), Calendar.getInstance(), Calendar.getInstance()));
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
