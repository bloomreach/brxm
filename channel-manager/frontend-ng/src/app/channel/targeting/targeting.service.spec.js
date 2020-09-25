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
  let $rootScope;
  let $window;
  let ConfigService;
  let TargetingService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.targeting');

    inject((_$httpBackend_, _$rootScope_, _$window_, _ConfigService_, _TargetingService_) => {
      $httpBackend = _$httpBackend_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      ConfigService = _ConfigService_;
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

  describe('getVariantIDs', () => {
    it('should return a list of variant IDs', () => {
      const responseData = {
        message: 'Available variants: ',
        data: ['variant1', 'hippo-default'],
      };
      $httpBackend
        .expectGET('/test/container-item-id./')
        .respond(200, responseData);

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getVariantIDs('container-item-id').then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith(responseData);
    });
  });

  describe('getVariants', () => {
    beforeEach(() => {
      ConfigService.variantsUuid = 'variantsUuid';
      ConfigService.locale = 'locale';
    });

    it('should request the variant IDs', () => {
      spyOn(TargetingService, 'getVariantIDs');

      TargetingService.getVariants('container-item-id');
      expect(TargetingService.getVariantIDs).toHaveBeenCalled();
    });

    it('should return a list of variants', () => {
      spyOn(TargetingService, 'getVariantIDs').and.returnValue({
        data: ['variant-1', 'variant-2'],
        success: true,
      });

      const responseData = {
        data: [{ id: 'hippo-default' }],
        message: 'Component personas loaded successfully',
        success: true,
      };
      $httpBackend
        .expectPOST('/test/variantsUuid./componentvariants?locale=locale', ['variant-1', 'variant-2'])
        .respond(200, responseData);

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getVariants('container-item-id').then(promiseSpy).catch(fail);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith(responseData);
    });

    it('should resolve with an error response if the request for variant ids fails', () => {
      const errorResponse = { success: false };
      spyOn(TargetingService, 'getVariantIDs').and.returnValue(errorResponse);

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getVariants('container-item-id').then(promiseSpy);

      $rootScope.$digest();
      expect(promiseSpy).toHaveBeenCalledWith(errorResponse);
    });

    it('should resolve with an error response if the backend fails', () => {
      spyOn(TargetingService, 'getVariantIDs').and.returnValue({ success: true });

      const errorResponse = { success: false };
      $httpBackend
        .expectPOST()
        .respond(400, errorResponse);

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getVariants('container-item-id').then(promiseSpy).catch(fail);

      $httpBackend.flush();
      expect(promiseSpy).toHaveBeenCalledWith({
        data: errorResponse,
        message: 'Failed to load variants for container-item "container-item-id"',
        reloadRequired: false,
        success: false,
      });
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
      $httpBackend.expectGET(urlRegex).respond(500, responseData);

      const promiseSpy = jasmine.createSpy('promiseSpy');
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
    it('should return a list of characteristics', () => {
      spyOn(TargetingService, 'getCharacteristicsIDs').and.returnValue({
        data: { items: [{ id: 'c1' }, { id: 'c2' }] },
        success: true,
      });
      spyOn(TargetingService, 'getCharacteristic')
        .and.callFake(id => ({ data: `characteristic-${id}`, success: true }));

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getCharacteristics().then(promiseSpy);
      $rootScope.$digest();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: { items: ['characteristic-c1', 'characteristic-c2'] },
        message: 'Characteristics loaded successfully',
        reloadRequired: false,
        success: true,
      });
    });
  });

  describe('getCharacteristicsIDs', () => {
    const urlRegex = /targeting-rest-url\/characteristics.*/;

    it('should return an array of characteristic IDs', () => {
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
            success: true,
          }];
        });

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getCharacteristicsIDs().then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        message: 'Characteristics IDs loaded successfully',
        success: true,
      });
    });

    it('should resolve with an error response if the backend fails', () => {
      const responseData = {};
      $httpBackend.expectGET(urlRegex).respond(500, responseData);

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getCharacteristicsIDs().then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        message: 'Failed to load characteristics IDs',
        reloadRequired: false,
        success: false,
      });
    });
  });

  describe('getCharacteristic', () => {
    const urlRegex = /targeting-rest-url\/characteristics\/(.+).*/;

    it('should retrieve a characteristic by id', () => {
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
            message: null,
            success: true,
          }];
        });

      const promiseSpy = jasmine.createSpy('promiseSpy');
      TargetingService.getCharacteristic('dayofweek').then(promiseSpy);
      $httpBackend.flush();

      expect(promiseSpy).toHaveBeenCalledWith({
        data: responseData,
        message: 'Characteristic "dayofweek" loaded successfully',
        success: true,
      });
    });

    it('should resolve with an error response if the backend fails', () => {
      const responseData = {};
      $httpBackend.expectGET(urlRegex).respond(500, responseData);

      const promiseSpy = jasmine.createSpy('promiseSpy');
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
