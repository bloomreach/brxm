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

describe('ChangesMenuCtrl', () => {
  let ChannelService;
  let DialogService;
  let HippoIframeService;
  let $q;
  let ChangesMenuCtrl;
  let $rootScope;

  beforeEach(() => {
    module('hippo-cm');

    inject((
      $controller,
      _$rootScope_,
      _$q_,
      _ChannelService_,
      _DialogService_,
      _HippoIframeService_
    ) => {
      $q = _$q_;
      ChannelService = _ChannelService_;
      DialogService = _DialogService_;
      HippoIframeService = _HippoIframeService_;
      $rootScope = _$rootScope_;

      ChangesMenuCtrl = $controller('ChangesMenuCtrl', {
        scope: $rootScope.$new(),
      });
    });

    spyOn(ChannelService, 'publishOwnChanges').and.returnValue($q.resolve());
    spyOn(ChannelService, 'discardOwnChanges').and.returnValue($q.resolve());
    spyOn(HippoIframeService, 'reload');
    spyOn(DialogService, 'confirm').and.callThrough();
  });

  it('publishes changes', () => {
    ChangesMenuCtrl.publish();
    $rootScope.$digest();

    expect(ChannelService.publishOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('discards changes', () => {
    spyOn(DialogService, 'show').and.returnValue($q.resolve());

    ChangesMenuCtrl.discard();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('does not discard changes if not confirmed', () => {
    spyOn(DialogService, 'show').and.returnValue($q.reject());

    ChangesMenuCtrl.discard();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).not.toHaveBeenCalled();
  });
});
