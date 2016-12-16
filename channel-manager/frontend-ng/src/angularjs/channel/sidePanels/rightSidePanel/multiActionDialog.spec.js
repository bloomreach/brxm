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

import angular from 'angular';
import 'angular-mocks';

describe('hippoIframeCtrl', () => {
  let $mdDialog;
  let MultiActionDialogCtrl;
  
  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$mdDialog_) => {
      $mdDialog = _$mdDialog_;
      MultiActionDialogCtrl = $controller('MultiActionDialogCtrl');
    });

    spyOn($mdDialog, 'hide');
    spyOn($mdDialog, 'cancel');
  });

  it('hides the dialog with the given action', () => {
    MultiActionDialogCtrl.action('transparent');

    expect($mdDialog.hide).toHaveBeenCalledWith('transparent');
  });

  it('cancels the dialog', () => {
    MultiActionDialogCtrl.cancel();

    expect($mdDialog.cancel).toHaveBeenCalled();
  });
});
