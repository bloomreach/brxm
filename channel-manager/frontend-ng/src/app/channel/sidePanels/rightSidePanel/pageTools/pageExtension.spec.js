/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('pageExtension', () => {
  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($componentController) => {
      const $uiRouterGlobals = {
        params: {
          extensionId: 'test',
          pageUrl: 'testPageUrl',
        },
      };

      const PageMetaDataService = {
        getChannelId: () => 'testChannelId',
        getPageId: () => 'testPageId',
        getPathInfo: () => '/page/subpage.html',
        getSiteMapItemId: () => 'testSitemapItemId',
      };

      const ChannelService = {
        getChannel: () => ({
          contextPath: '/site',
          mountPath: '/subMount',
        }),
      };

      $ctrl = $componentController('pageExtension', {
        $uiRouterGlobals,
        ChannelService,
        PageMetaDataService,
      });
    });
  });

  describe('$onInit', () => {
    beforeEach(() => {
      $ctrl.$onInit();
    });

    it('initializes the page extension', () => {
      expect($ctrl.extensionId).toEqual('test');
      expect($ctrl.pageContext).toEqual({
        channel: {
          id: 'testChannelId',
          mountPath: '/subMount',
          contextPath: '/site',
        },
        id: 'testPageId',
        url: 'testPageUrl',
        path: '/page/subpage.html',
        siteMapItem: {
          id: 'testSitemapItemId',
        },
      });
    });

    describe('uiOnParamsChanged', () => {
      it('updates the page context when the pageUrl parameter changed', () => {
        $ctrl.uiOnParamsChanged({
          pageUrl: 'newPageUrl',
        });
        expect($ctrl.pageContext).toEqual({
          channel: {
            id: 'testChannelId',
            mountPath: '/subMount',
            contextPath: '/site',
          },
          id: 'testPageId',
          url: 'newPageUrl',
          path: '/page/subpage.html',
          siteMapItem: {
            id: 'testSitemapItemId',
          },
        });
      });

      it('does not update the page context when the pageUrl parameter did not change', () => {
        $ctrl.uiOnParamsChanged({});
        expect($ctrl.pageContext).toEqual({
          channel: {
            id: 'testChannelId',
            mountPath: '/subMount',
            contextPath: '/site',
          },
          id: 'testPageId',
          url: 'testPageUrl',
          path: '/page/subpage.html',
          siteMapItem: {
            id: 'testSitemapItemId',
          },
        });
      });
    });
  });
});
