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

describe('ChannelCtrl', function () {
  'use strict';

  var ChannelCtrl;
  var ChannelService;

  beforeEach(function () {
    module('hippo-cm');

    inject(function ($controller, $rootScope, _ChannelService_) {
      var scope = $rootScope.$new();
      ChannelService = _ChannelService_;
      spyOn(ChannelService, 'switchToChannel');
      spyOn(ChannelService, 'getUrl').and.returnValue('/test/url');
      spyOn(ChannelService, 'getId').and.returnValue('test-id');
      ChannelCtrl = $controller('ChannelCtrl', {
        ChannelService: ChannelService,
        $scope: scope,
      });

    });
  });

  it('gets the iframe URL from the channel service', function () {
    expect(ChannelCtrl.iframeUrl).toEqual('/test/url');
  });

  it('is not in edit mode by default', function () {
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

  it('enables and disables edit mode when toggling it', function () {
    ChannelCtrl.toggleEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(true);
    ChannelCtrl.toggleEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

});
