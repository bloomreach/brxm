/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

'use strict';

describe('The hippo-cm.channel module', function () {

  'use strict';

  var $state;
  var $rootScope;
  var $q;
  var ChannelService;

  beforeEach(function () {
    spyOn(window.APP_TO_CMS, 'publish').and.callThrough();
    spyOn(window.CMS_TO_APP, 'subscribe').and.callThrough();

    module('hippo-cm', function ($provide, ChannelServiceProvider) {
      var channelService = ChannelServiceProvider.$get();
      $provide.value('$state', jasmine.createSpyObj('$state', ['go']));
      spyOn(channelService, 'load');
      $provide.value('ChannelService', channelService);
    });

    inject(function (_$rootScope_, _$state_, _$q_, _ChannelService_) {
      $rootScope = _$rootScope_;
      $state = _$state_;
      $q = _$q_;
      ChannelService = _ChannelService_;
    });
  });

  it('loads the channel published by the host', function () {
    var testChannel = { id: 'testChannelId' };

    expect(window.CMS_TO_APP.subscribe).toHaveBeenCalledWith('load-channel', jasmine.any(Function));

    ChannelService.load.and.returnValue($q.resolve(testChannel));

    window.CMS_TO_APP.publish('load-channel', testChannel);
    expect(ChannelService.load).toHaveBeenCalledWith(testChannel);

    $rootScope.$apply();

    expect($state.go).toHaveBeenCalledWith('hippo-cm.channel', {
      channelId: testChannel.id,
    }, {
      reload: true,
    });
  });

  it('publishes a reload-channel event to the host', function () {
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('reload-channel');
  });
});
