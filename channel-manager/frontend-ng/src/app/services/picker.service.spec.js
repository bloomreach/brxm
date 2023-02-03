/*
 * Copyright 2018-2023 Bloomreach (https://www.bloomreach.com)
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
  let $state;
  let CmsService;
  let PickerService;

  beforeEach(() => {
    CmsService = jasmine.createSpyObj('CmsService', ['publish']);
    $state = { params: {} };

    angular.mock.module(($provide) => {
      $provide.value('$state', $state);
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
      const pickerContext = {};
      const currentPath = '/test';

      PickerService.pickPath(currentPath, pickerConfig, pickerContext);
      expect(CmsService.publish).toHaveBeenCalledWith(
        'show-path-picker',
        { config: pickerConfig, context: pickerContext },
        currentPath,
        jasmine.any(Function),
        jasmine.any(Function),
      );
    });

    it('resolves the returned promise with the picked path', (done) => {
      const promise = PickerService.pickPath('/test', {},);
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
      const promise = PickerService.pickPath('/test', {});
      const onCancel = CmsService.publish.calls.mostRecent().args[4];
      onCancel();

      promise.catch(done);
      $rootScope.$digest();
    });
  });

  describe('pickLink', () => {
    it('opens the picker dialog', () => {
      const pickerConfig = {};
      const pickerContext = {};
      const currentLink = { uuid: '1' };

      PickerService.pickLink(currentLink, pickerConfig, pickerContext);
      expect(CmsService.publish).toHaveBeenCalledWith(
        'show-link-picker',
        { config: pickerConfig, context: pickerContext },
        currentLink,
        jasmine.any(Function),
        jasmine.any(Function),
      );
    });

    it('resolves the returned promise with the picked link', (done) => {
      const promise = PickerService.pickLink({ uuid: '1' }, {});
      const onSuccess = CmsService.publish.calls.mostRecent().args[3];
      onSuccess({ uuid: '2' });

      promise.then((pickedLink) => {
        expect(pickedLink).toEqual({ uuid: '2' });
        done();
      });
      $rootScope.$digest();
    });

    it('rejects the returns promise when the dialog is canceled', (done) => {
      const promise = PickerService.pickLink({ uuid: '1' }, {});
      const onCancel = CmsService.publish.calls.mostRecent().args[4];
      onCancel();

      promise.catch(done);
      $rootScope.$digest();
    });
  });

  describe('pickImage', () => {
    it('opens the picker dialog', () => {
      const pickerConfig = {};
      const pickerContext = {};

      const currentImage = { uuid: '1' };

      PickerService.pickImage(currentImage, pickerConfig);
      expect(CmsService.publish).toHaveBeenCalledWith(
        'show-image-picker',
        { config: pickerConfig, context: pickerContext },
        currentImage,
        jasmine.any(Function),
        jasmine.any(Function),
      );
    });

    it('resolves the returned promise with the picked image', (done) => {
      const promise = PickerService.pickImage({ uuid: '1' }, {});
      const onSuccess = CmsService.publish.calls.mostRecent().args[3];
      onSuccess({ uuid: '2' });

      promise.then((pickedImage) => {
        expect(pickedImage).toEqual({ uuid: '2' });
        done();
      });
      $rootScope.$digest();
    });

    it('rejects the returns promise when the dialog is canceled', (done) => {
      const promise = PickerService.pickImage({ uuid: '1' }, {});
      const onCancel = CmsService.publish.calls.mostRecent().args[4];
      onCancel();

      promise.catch(done);
      $rootScope.$digest();
    });
  });

  describe('creates a picker context from the $state', () => {
    it('merges the input context with the $state context and filters out empty values', () => {
      $state.params = {
        'empty': '',
        'state-a': 'a',
        'state-b': 'b',
      };

      const pickerConfig = {};
      const pickerContext = { 'state-b': 'bb' };
      const currentPath = '/test';

      PickerService.pickPath(currentPath, pickerConfig, pickerContext);
      expect(CmsService.publish).toHaveBeenCalledWith(
        'show-path-picker',
        { config: pickerConfig, context: {
          'state-a': 'a',
          'state-b': 'bb',
        } },
        currentPath,
        jasmine.any(Function),
        jasmine.any(Function),
      );
    });
  });
});
