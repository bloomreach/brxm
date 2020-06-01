/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

describe('translateLoader', () => {
  let $q;
  let $rootScope;
  let translateLoader;
  let CommunicationService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    CommunicationService = jasmine.createSpyObj('CommunicationService', ['getTranslations']);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _translateLoader_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      translateLoader = _translateLoader_;
    });
  });

  it('should get translations via the communication service', (done) => {
    const translations = {};
    CommunicationService.getTranslations.and.returnValue(translations);

    translateLoader({ key: 'en' })
      .then((result) => {
        expect(CommunicationService.getTranslations).toHaveBeenCalledWith('en');
        expect(result).toBe(translations);
      })
      .then(done);

    $rootScope.$digest();
  });

  it('should throw key on error', (done) => {
    CommunicationService.getTranslations.and.returnValue($q.reject());

    translateLoader({ key: 'en' })
      .catch((error) => {
        expect(error).toBe('en');
      })
      .then(done);

    $rootScope.$digest();
  });
});
