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

describe('DialogService', () => {
  let $mdDialog;
  let DialogService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-api');

    inject((_$mdDialog_, _DialogService_) => {
      $mdDialog = _$mdDialog_;
      DialogService = _DialogService_;
    });
  });

  it('uses $mdDialog to create a confirmation dialog', () => {
    const mockDialogPreset = {};
    spyOn($mdDialog, 'confirm').and.returnValue(mockDialogPreset);
    expect(DialogService.confirm()).toEqual(mockDialogPreset);
  });

  it('uses $mdDialog to create a alert dialog', () => {
    const mockDialogPreset = {};
    spyOn($mdDialog, 'alert').and.returnValue(mockDialogPreset);
    expect(DialogService.alert()).toEqual(mockDialogPreset);
  });

  it('uses $mdDialog to show a confirmation dialog', () => {
    spyOn($mdDialog, 'show');
    const confirmationDialog = DialogService.confirm();
    DialogService.show(confirmationDialog);
    expect($mdDialog.show).toHaveBeenCalledWith(confirmationDialog);
  });

  it('sends a show-mask event to the CMS when a dialog is shown', () => {
    spyOn(window.APP_TO_CMS, 'publish');
    const confirmationDialog = DialogService.confirm();
    DialogService.show(confirmationDialog);
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('show-mask');
  });

  it('sends a remove-mask event to the CMS when a dialog is being removed', () => {
    const confirmationDialog = DialogService.confirm();
    spyOn(window.APP_TO_CMS, 'publish');
    confirmationDialog._options.onRemoving();
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('remove-mask');
  });

  it('sends a remove-mask event to the CMS when showing a custom dialog', () => {
    const dialogConfig = {};
    spyOn(window.APP_TO_CMS, 'publish');
    DialogService.show(dialogConfig);
    expect(dialogConfig.onRemoving).toBeDefined();
    dialogConfig.onRemoving();
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('remove-mask');
  });

  it('sends a remove-mask event to the CMS when showing a custom dialog with custom onRemoving option', () => {
    const dialogConfig = { onRemoving: () => {} };
    spyOn(window.APP_TO_CMS, 'publish');
    const onRemovingSpy = spyOn(dialogConfig, 'onRemoving');
    DialogService.show(dialogConfig);
    dialogConfig.onRemoving();
    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('remove-mask');
    expect(onRemovingSpy).toHaveBeenCalled();
  });

  it('uses $mdDialog to hide an existing dialog', () => {
    spyOn($mdDialog, 'hide');
    DialogService.hide();
    expect($mdDialog.hide).toHaveBeenCalled();
  });
});
