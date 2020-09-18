/*
 * Copyright 2020 Bloomreach
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

describe('TargetingService', () => {
  let $httpBackend;
  let $window;
  let TargetingService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.targeting');

    inject((_$httpBackend_, _$window_, _TargetingService_) => {
      $httpBackend = _$httpBackend_;
      $window = _$window_;
      TargetingService = _TargetingService_;
    });

    $window.parent.Hippo.Targeting.HttpProxy.REST_URL = 'targeting-rest-url';
    TargetingService.init();
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  describe('init', () => {
    it('should throw an error if it can not find the global "Hippo" object', () => {
      delete $window.parent.Hippo;
      expect(() => TargetingService.init()).toThrowError('Failed to retrieve Hippo object from global scope');
    });

    it('should throw an error if it can not find the global "Hippo.Targeting" object', () => {
      delete $window.parent.Hippo.Targeting;
      expect(() => TargetingService.init())
        .toThrowError('Failed to retrieve targeting configuration from global scope, is relevance enabled?');
    });
  });

  describe('getPersonas', () => {
    const urlRegex = /targeting-rest-url\/personas.*/;

    beforeEach(() => {
      $window.parent.Hippo.Targeting.CollectorPlugins = {
        collector1: {},
        collector2: {},
      };
      TargetingService.init();
    });

    it('should retrieve a list of personas', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const responseData = {
        items: [
          { id: 'persona1' },
          { id: 'persona2' },
        ],
        count: 2,
      };

      $httpBackend
        .expectGET(urlRegex)
        .respond((method, url, data, headers, params) => {
          expect(params.collectors).toBe('collector1,collector2');
          expect(params['Force-Client-Host']).toBe('true');
          expect(params.antiCache).toBeTruthy();

          return [200, responseData];
        });

      TargetingService.getPersonas().then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        message: 'Personas loaded successfully',
        reloadRequired: false,
        success: true,
      });
    });

    it('should not choke if there are no collectors defined', () => {
      $window.parent.Hippo.Targeting.CollectorPlugins = null;
      TargetingService.init();

      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(urlRegex).respond(200);

      TargetingService.getPersonas().then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalled();
    });

    it('should resolve with an error response if the backend fails', () => {
      const responseData = {};
      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(urlRegex).respond(500, responseData);

      TargetingService.getPersonas().then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        message: 'Failed to load personas',
        reloadRequired: false,
        success: false,
      });
    });
  });

  describe('getCharacteristics', () => {
    const urlRegex = /targeting-rest-url\/characteristics.*/;

    it('should retrieve a list of characteristic ids', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const responseData = {
        items: [
          { id: 'city' },
          { id: 'country' },
        ],
        count: 2,
      };

      $httpBackend
        .expectGET(urlRegex)
        .respond((method, url, data, headers, params) => {
          expect(params['Force-Client-Host']).toBe('true');
          expect(params.antiCache).toBeTruthy();

          return [200, {
            data: responseData,
            errorCode: null,
            message: null,
            reloadRequired: false,
            success: true,
          }];
        });

      TargetingService.getCharacteristics().then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        errorCode: null,
        message: 'Characteristics loaded successfully',
        reloadRequired: false,
        success: true,
      });
    });

    it('should resolve with an error response if the backend fails', () => {
      const responseData = {};
      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(urlRegex).respond(500, responseData);

      TargetingService.getCharacteristics().then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        message: 'Failed to load characteristics',
        reloadRequired: false,
        success: false,
      });
    });
  });

  describe('getCharacteristic', () => {
    const urlRegex = /targeting-rest-url\/characteristics\/(.+).*/;

    it('should retrieve a characteristic by id', () => {
      const promiseSpy = jasmine.createSpy('promiseSpy');
      const responseData = {
        id: 'dayofweek',
        targetGroups: [],
        success: true,
        message: 'OK',
      };

      $httpBackend
        .expectGET(urlRegex, { Accept: 'application/json, text/plain, */*' }, ['characterId'])
        .respond((method, url, data, headers, params) => {
          expect(params.characterId).toBe('dayofweek');
          expect(params['Force-Client-Host']).toBe('true');
          expect(params.antiCache).toBeTruthy();

          return [200, {
            data: responseData,
            errorCode: null,
            message: null,
            reloadRequired: false,
            success: true,
          }];
        });

      TargetingService.getCharacteristic('dayofweek').then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        errorCode: null,
        message: 'Characteristic "dayofweek" loaded successfully',
        reloadRequired: false,
        success: true,
      });
    });

    it('should resolve with an error response if the backend fails', () => {
      const responseData = {};
      const promiseSpy = jasmine.createSpy('promiseSpy');
      $httpBackend.expectGET(urlRegex).respond(500, responseData);

      TargetingService.getCharacteristic('dayofweek').then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        message: 'Failed to load characteristic "dayofweek"',
        reloadRequired: false,
        success: false,
      });
    });
  });
});
