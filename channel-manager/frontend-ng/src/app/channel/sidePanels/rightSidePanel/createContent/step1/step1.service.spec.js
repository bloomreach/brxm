/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('Step1Service', () => {
  let ChannelService;
  let ContentService;
  let Step1Service;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    ChannelService = jasmine.createSpyObj('ChannelService', ['initialize', 'getChannel']);
    ChannelService.getChannel.and.returnValue({
      contentRoot: '/channel/content',
    });
    ContentService = jasmine.createSpyObj('ContentService', ['_send']);
    ContentService._send.and.returnValue(Promise.resolve());

    angular.mock.module(($provide) => {
      $provide.value('ChannelService', ChannelService);
      $provide.value('ContentService', ContentService);
    });

    inject((_ChannelService_, _ContentService_, _Step1Service_) => {
      ChannelService = _ChannelService_;
      ContentService = _ContentService_;
      Step1Service = _Step1Service_;
    });
  });

  describe('Step1 service', () => {
    it('is created with a clean state', () => {
      expect(Step1Service.name).toBeUndefined();
      expect(Step1Service.url).toBeUndefined();
      expect(Step1Service.locale).toBeUndefined();
      expect(Step1Service.rootPath).toBeUndefined();
      expect(Step1Service.templateQuery).toBeUndefined();
    });

    describe('open', () => {
      describe('parsing the rootPath', () => {
        it('defaults to the channel root if not set', () => {
          Step1Service.open('tpl-query');
          expect(Step1Service.rootPath).toBe('/channel/content');

          Step1Service.open('tpl-query', '');
          expect(Step1Service.rootPath).toBe('/channel/content');
        });

        it('overrides the channel root path if absolute', () => {
          Step1Service.open('tpl-query', '/root/path');
          expect(Step1Service.rootPath).toBe('/root/path');
        });

        it('is concatenated wth the channel\'s root path if relative', () => {
          Step1Service.open('tpl-query', 'some/path');
          expect(Step1Service.rootPath).toBe('/channel/content/some/path');
        });

        it('never ends with a slash', () => {
          Step1Service.open('tpl-query', '/root/path/');
          expect(Step1Service.rootPath).toBe('/root/path');

          Step1Service.open('tpl-query', 'some/path/');
          expect(Step1Service.rootPath).toBe('/channel/content/some/path');
        });
      });
    });
  });
});
