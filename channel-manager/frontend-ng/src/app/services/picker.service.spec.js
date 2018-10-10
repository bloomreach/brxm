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

describe('PickerService', () => {
  let $rootScope;
  let CmsService;
  let PickerService;

  beforeEach(() => {
    CmsService = jasmine.createSpyObj('CmsService', ['publish']);

    angular.mock.module(($provide) => {
      $provide.value('CmsService', CmsService);
    });

    inject((_$rootScope_, _PickerService_) => {
      $rootScope = _$rootScope_;
      PickerService = _PickerService_;
    });
  });

  describe('pickPath', () => {
    it('opens the picker dialog', () => {
      const pickerConfig = {};
      const currentPath = '/test';

      PickerService.pickPath(pickerConfig, currentPath);
      expect(CmsService.publish).toHaveBeenCalledWith('show-path-picker', pickerConfig, currentPath, jasmine.any(Function), jasmine.any(Function));
    });

    it('resolves the returned promise with the picked path', (done) => {
      const promise = PickerService.pickPath({}, '/test');
      const onSuccess = CmsService.publish.calls.mostRecent().args[3];
      onSuccess({ path: '/pickedPath', displayName: 'Picked Path' });

      promise.then(({ path, displayName }) => {
        expect(path).toBe('/pickedPath');
        expect(displayName).toBe('Picked Path');
        done();
      });
      $rootScope.$digest();
    });

    it('rejects the returns promise when the dialog is canceled', (done) => {
      const promise = PickerService.pickPath({}, '/test');
      const onCancel = CmsService.publish.calls.mostRecent().args[4];
      onCancel();

      promise.catch(done);
      $rootScope.$digest();
    });
  });

  describe('pickLink', () => {
    it('opens the picker dialog', () => {
      const pickerConfig = {};
      const currentLink = { uuid: '1' };

      PickerService.pickLink(pickerConfig, currentLink);
      expect(CmsService.publish).toHaveBeenCalledWith('show-link-picker', pickerConfig, currentLink, jasmine.any(Function), jasmine.any(Function));
    });

    it('resolves the returned promise with the picked link', (done) => {
      const promise = PickerService.pickLink({}, { uuid: '1' });
      const onSuccess = CmsService.publish.calls.mostRecent().args[3];
      onSuccess({ uuid: '2' });

      promise.then((pickedLink) => {
        expect(pickedLink).toEqual({ uuid: '2' });
        done();
      });
      $rootScope.$digest();
    });

    it('rejects the returns promise when the dialog is canceled', (done) => {
      const promise = PickerService.pickLink({}, { uuid: '1' });
      const onCancel = CmsService.publish.calls.mostRecent().args[4];
      onCancel();

      promise.catch(done);
      $rootScope.$digest();
    });
  });

  describe('pickImage', () => {
    it('opens the picker dialog', () => {
      const pickerConfig = {};
      const currentImage = { uuid: '1' };

      PickerService.pickImage(pickerConfig, currentImage);
      expect(CmsService.publish).toHaveBeenCalledWith('show-image-picker', pickerConfig, currentImage, jasmine.any(Function), jasmine.any(Function));
    });

    it('resolves the returned promise with the picked image', (done) => {
      const promise = PickerService.pickImage({}, { uuid: '1' });
      const onSuccess = CmsService.publish.calls.mostRecent().args[3];
      onSuccess({ uuid: '2' });

      promise.then((pickedImage) => {
        expect(pickedImage).toEqual({ uuid: '2' });
        done();
      });
      $rootScope.$digest();
    });

    it('rejects the returns promise when the dialog is canceled', (done) => {
      const promise = PickerService.pickImage({}, { uuid: '1' });
      const onCancel = CmsService.publish.calls.mostRecent().args[4];
      onCancel();

      promise.catch(done);
      $rootScope.$digest();
    });
  });
});
