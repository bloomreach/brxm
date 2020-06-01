/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('RejectPromptCtrl', () => {
  let $mdDialog;
  let RejectPromptCtrl;
  let translationData;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    $mdDialog = jasmine.createSpyObj('$mdDialog', ['hide', 'cancel']);
    translationData = {
      channelName: 'test',
    };

    inject((
      $controller,
    ) => {
      RejectPromptCtrl = $controller('RejectPromptCtrl', {
        $mdDialog,
        translationData,
      });
    });
  });

  it('should have translationData', () => {
    expect(RejectPromptCtrl.translationData.channelName).toEqual('test');
  });

  it('should close dialog', () => {
    const message = 'testMessage';
    RejectPromptCtrl.close(message);
    expect($mdDialog.hide).toHaveBeenCalledWith(message);
  });

  it('should cancel', () => {
    RejectPromptCtrl.cancel();
    expect($mdDialog.cancel).toHaveBeenCalled();
  });
});
