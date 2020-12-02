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

xdescribe('TargetingService', () => {
  let $httpBackend;
  let $rootScope;
  let $window;
  let ChannelService;
  let ConfigService;
  let HstService;
  let PageStructureService;
  let TargetingService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$httpBackend_,
      _$rootScope_,
      _$window_,
      _ChannelService_,
      _ConfigService_,
      _HstService_,
      _PageStructureService_,
      _TargetingService_,
    ) => {
      $httpBackend = _$httpBackend_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      ChannelService = _ChannelService_;
      ConfigService = _ConfigService_;
      HstService = _HstService_;
      PageStructureService = _PageStructureService_;
      TargetingService = _TargetingService_;
    });

    $window.parent.Hippo.Targeting.HttpProxy.REST_URL = 'targeting-rest-url';

    ConfigService.locale = 'locale';
    ConfigService.variantsUuid = 'variantsUuid';

    spyOn(ChannelService, 'checkChanges');

    TargetingService.init();
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  function expectHttp(backend, message, when, expects = [], data = {}) {
    backend.respond((method, url, _data, headers, params) => {
      expects.forEach(exp => exp(params));

      return [200, {
        data,
        message: null,
        success: true,
      }];
    });

    const promiseSpy = jasmine.createSpy('promiseSpy');
    when().then(promiseSpy).catch(fail);
    $httpBackend.flush();

    expect(promiseSpy).toHaveBeenCalledWith({
      data,
      message,
      success: true,
      reloadRequired: false,
    });
  }

  function expectGet(url, message, when, expects, responseData) {
    expectHttp($httpBackend.expectGET(url), message, when, expects, responseData);
  }

  function expectHttpError(responseData, message, when) {
    const promiseSpy = jasmine.createSpy('promiseSpy');
    when().then(promiseSpy).catch(fail);
    $httpBackend.flush();

    expect(promiseSpy).toHaveBeenCalledWith({
      data: responseData,
      message,
      reloadRequired: false,
      success: false,
    });
  }

  function expectGetError(url, message, when) {
    const responseData = {};
    $httpBackend.expectGET(url).respond(500, responseData);
    expectHttpError(responseData, message, when);
  }

  function expectPostError(url, message, when) {
    const responseData = {};
    $httpBackend.expectPOST(url).respond(500, responseData);
    expectHttpError(responseData, message, when);
  }

  function expectDefaultParams(params) {
    expect(params['Force-Client-Host']).toBe('true');
    expect(params.antiCache).toBeTruthy();
  }

  describe('init', () => {
    it('should throw an error if it can not find the global "Hippo" object', () => {
      delete $window.parent.Hippo;
      expect(() => TargetingService.init()).toThrowError('Failed to retrieve Hippo object from global scope');
    });
  });

  describe('getVariantIDs', () => {
    it('should return a list of variant IDs', () => {
      expectGet(
        '/test/container-item-id./',
        'Successfully loaded variant ids for container-item "container-item-id"',
        () => TargetingService.getVariantIDs('container-item-id'),
      );
    });
  });

  describe('getVariants', () => {
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

      const url = '/test/variantsUuid./componentvariants?locale=locale';
      expectHttp(
        $httpBackend.expectPOST(url, ['variant-1', 'variant-2']),
        'Successfully loaded variants for container-item "container-item-id"',
        () => TargetingService.getVariants('container-item-id'),
      );
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

      expectPostError('', 'Failed to load variants for container-item "container-item-id"',
        () => TargetingService.getVariants('container-item-id'));
    });
  });

  describe('add and update Variant', () => {
    const data = {};

    beforeEach(() => {
      spyOn(HstService, 'doPutFormWithHeaders').and.returnValue({ data });

      const page = jasmine.createSpyObj('page', ['getComponentById']);
      page.getComponentById.and.returnValue({
        lastModified: 'last-modified',
      });
      spyOn(PageStructureService, 'getPage').and.returnValue(page);
    });

    describe('addVariant', () => {
      it('should execute a PUT request on the HST', () => {
        TargetingService.addVariant('component-id', data);
        $rootScope.$digest();

        expect(HstService.doPutFormWithHeaders).toHaveBeenCalledWith(
          data,
          'component-id',
          jasmine.objectContaining({
            lastModifiedTimestamp: 'last-modified',
          }),
          'hippo-new-configuration',
        );
      });

      it('should return success response', (done) => {
        TargetingService.addVariant('component-id', data).then((result) => {
          expect(result).toEqual({
            message: 'Succesfully created a new variant for component "component-id"',
            reloadRequired: false,
            success: true,
            data,
          });
          done();
        });

        $rootScope.$digest();
      });

      it('should check for changes', () => {
        TargetingService.addVariant('component-id', data);
        $rootScope.$digest();

        expect(ChannelService.checkChanges).toHaveBeenCalled();
      });
    });

    describe('updateVariant', () => {
      it('should execute a PUT request on the HST with an URI-Encoded variant ID', () => {
        TargetingService.updateVariant('component-id', data, 'variant&id');
        $rootScope.$digest();

        expect(HstService.doPutFormWithHeaders).toHaveBeenCalledWith(
          data,
          'component-id',
          jasmine.objectContaining({
            lastModifiedTimestamp: 'last-modified',
          }),
          'variant%26id',
        );
      });

      it('should return success response with the newVariantId', (done) => {
        TargetingService.updateVariant('component-id', data, 'variant-id').then((result) => {
          expect(result).toEqual({
            message: 'Succesfully updated variant "variant-id" for component "component-id"',
            reloadRequired: false,
            success: true,
            data,
          });
          expect(result.data.newVariantId).toBeDefined();
          done();
        });

        $rootScope.$digest();
      });

      it('should check for changes', () => {
        TargetingService.updateVariant('component-id', data, 'variant-id');
        $rootScope.$digest();

        expect(ChannelService.checkChanges).toHaveBeenCalled();
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
      expectGet(
        urlRegex,
        'Personas loaded successfully',
        () => TargetingService.getPersonas(),
        [
          expectDefaultParams,
          params => expect(params.collectors).toBe('collector1,collector2'),
        ],
        {
          items: ['woot'],
          count: 1,
        },
      );
    });

    it('should not choke if there are no collectors defined', () => {
      $window.parent.Hippo.Targeting.CollectorPlugins = null;
      TargetingService.init();

      expectGet(urlRegex, 'Personas loaded successfully', () => TargetingService.getPersonas());
    });

    it('should resolve with an error response if the backend fails', () => {
      expectGetError(urlRegex, 'Failed to load personas', () => TargetingService.getPersonas());
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
        data: ['characteristic-c1', 'characteristic-c2'],
        message: 'Characteristics loaded successfully',
        reloadRequired: false,
        success: true,
      });
    });
  });

  describe('getCharacteristicsIDs', () => {
    const urlRegex = /targeting-rest-url\/characteristics.*/;

    it('should return an array of characteristic IDs', () => {
      expectGet(
        urlRegex,
        'Characteristics IDs loaded successfully',
        () => TargetingService.getCharacteristicsIDs(),
        [expectDefaultParams],
      );
    });

    it('should resolve with an error response if the backend fails', () => {
      expectGetError(urlRegex, 'Failed to load characteristics IDs',
        () => TargetingService.getCharacteristicsIDs());
    });
  });

  describe('getCharacteristic', () => {
    const urlRegex = /targeting-rest-url\/characteristics\/(.+).*/;

    it('should retrieve a characteristic by id', () => {
      expectHttp(
        $httpBackend.expectGET(urlRegex, { Accept: 'application/json, text/plain, */*' }, ['characterId']),
        'Characteristic "dayofweek" loaded successfully',
        () => TargetingService.getCharacteristic('dayofweek'),
        [
          expectDefaultParams,
          params => expect(params.characterId).toBe('dayofweek'),
        ],
      );
    });

    it('should resolve with an error response if the backend fails', () => {
      expectGetError(urlRegex, 'Failed to load characteristic "dayofweek"',
        () => TargetingService.getCharacteristic('dayofweek'));
    });
  });

  describe('getExperiment', () => {
    const urlRegex = /targeting-rest-url\/experiments\/component\/(.+).*/;

    it('should retrieve the experiment for the specified component', () => {
      expectGet(
        urlRegex,
        'Experiment loaded successfully for component "componentId"',
        () => TargetingService.getExperiment('componentId'),
        [expectDefaultParams],
        params => expect(params.locale).toBe('locale'),
      );
    });

    it('should resolve with an error response if the backend fails', () => {
      expectGetError(urlRegex, 'Failed to load experiment for component "componentId"',
        () => TargetingService.getExperiment('componentId'));
    });
  });

  describe('saveExperiment', () => {
    const urlRegex = /targeting-rest-url\/experiments\/saveExperiment(.+).*/;

    it('should save the experiment for the specified component, goal and variant', () => {
      const payload = {
        componentId: 'componentId',
        goalId: 'goalId',
        variantId: 'variantId',
      };

      expectHttp(
        $httpBackend.expectPOST(urlRegex, payload),
        'Experiment saved for component "componentId" with goal "goalId" and variant "variantId"',
        () => TargetingService.saveExperiment('componentId', 'goalId', 'variantId'),
        [expectDefaultParams],
      );
      expect(ChannelService.checkChanges).toHaveBeenCalled();
    });

    it('should resolve with an error response if the backend fails', () => {
      expectPostError(
        urlRegex,
        'Failed to save experiment for component "componentId" with goal "goalId" and variant "variantId"',
        () => TargetingService.saveExperiment('componentId', 'goalId', 'variantId'),
      );
    });
  });

  describe('completeExperiment', () => {
    const urlRegex = /targeting-rest-url\/experiments\/complete(.+).*/;

    it('should complete the experiment of the specified component', () => {
      expectHttp(
        $httpBackend.expectPOST(urlRegex, 'componentId'),
        'Experiment completed for component "componentId"',
        () => TargetingService.completeExperiment('componentId', 'keepVariantId'),
        [expectDefaultParams],
        params => expect(params.keepOnlyVariantId).toBe('keepVariantId'),
      );
      expect(ChannelService.checkChanges).toHaveBeenCalled();
    });

    it('should resolve with an error response if the backend fails', () => {
      expectPostError(
        urlRegex,
        'Failed to complete experiment for component "componentId"',
        () => TargetingService.completeExperiment('componentId'),
      );
    });
  });

  describe('getExperimentStatus', () => {
    const urlRegex = /targeting-rest-url\/experiments\/serving(.+).*/;

    it('should return the state of the specified experiment', () => {
      expectGet(
        urlRegex,
        'Succesfully loaded state of experiment "experimentId"',
        () => TargetingService.getExperimentStatus('experimentId'),
        [expectDefaultParams],
      );
    });

    it('should resolve with an error response if the backend fails', () => {
      expectGetError(
        urlRegex,
        'Failed to load state of experiment "experimentId"',
        () => TargetingService.getExperimentStatus('experimentId'),
      );
    });
  });

  describe('getGoals', () => {
    const urlRegex = /targeting-rest-url\/goals.*/;

    it('should return an array of all available goals', () => {
      expectGet(
        urlRegex,
        'Succesfully loaded goals',
        () => TargetingService.getGoals(),
        [expectDefaultParams],
      );
    });

    it('should resolve with an error response if the backend fails', () => {
      expectGetError(urlRegex, 'Failed to load goals',
        () => TargetingService.getGoals());
    });
  });
});
