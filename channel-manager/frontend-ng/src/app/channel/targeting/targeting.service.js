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

const DEFAULT_SUCCESS_RESPONSE = {
  reloadRequired: false,
  success: true,
};

const DEFAULT_FAILURE_RESPONSE = {
  reloadRequired: false,
  success: false,
};

class TargetingService {
  constructor(
    $http,
    $q,
    $window,
    ChannelService,
    ConfigService,
    HstService,
    PageStructureService,
    PathService,
  ) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;
    this.$window = $window;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.HstService = HstService;
    this.PageStructureService = PageStructureService;
    this.PathService = PathService;

    this.init();
  }

  init() {
    const iframe = this.$window.parent;

    if (!iframe.Hippo) {
      throw new Error('Failed to retrieve Hippo object from global scope');
    }

    const { Targeting = { HttpProxy: {} } } = iframe.Hippo;

    this._apiUrl = Targeting.HttpProxy.REST_URL || '/_targeting';
    this._collectors = Targeting.CollectorPlugins || {};
    this._characteristicsConfig = Targeting.CharacteristicsConfig || {};
  }

  getCharacteristicConfig(id) {
    const configKey = this._getCharacteristicConfigKey(id);
    return this._characteristicsConfig[configKey] || null;
  }

  _getCharacteristicConfigKey(id) {
    switch (id) {
      case 'city':
        return 'Hippo.Targeting.CityCharacteristicPlugin';
      case 'country':
        return 'Hippo.Targeting.CountryCharacteristicPlugin';
      case 'continent':
        return 'Hippo.Targeting.ContinentCharacteristicPlugin';
      case 'dayofweek':
        return 'Hippo.Targeting.DayOfWeekCharacteristicPlugin';
      case 'documenttypes':
        return 'Hippo.Targeting.DocumentTypeCharacteristicPlugin';
      case 'groups':
        return 'Hippo.Targeting.GroupsCharacteristicPlugin';
      case 'pageviews':
        return 'Hippo.Targeting.PageViewsCharacteristicPlugin';
      case 'referrer':
        return 'Hippo.Targeting.ReferrerCharacteristicPlugin';
      case 'returningvisitor':
        return 'Hippo.Targeting.ReturningVisitorCharacteristicPlugin';
      case 'tracking':
        return 'Hippo.Targeting.TrackingCookieCharacteristicPlugin';
      default:
        throw new Error(`Characteristic with id "${id}" is not supported`);
    }
  }

  async getVariantIDs(containerItemId) {
    try {
      const result = await this.HstService.doGet(containerItemId);
      return this._success(`Successfully loaded variant ids for container-item "${containerItemId}"`, result);
    } catch (e) {
      return this._failure(`Failed to load variant ids for container-item "${containerItemId}"`, e);
    }
  }

  async getVariants(containerItemId) {
    const response = await this.getVariantIDs(containerItemId);
    if (!response.success) {
      return response;
    }

    try {
      const variantIDs = response.data;
      const { locale, variantsUuid } = this.ConfigService;
      const result = await this.HstService
        .doPostWithParams(variantIDs, variantsUuid, { locale }, 'componentvariants');
      return this._success(`Successfully loaded variants for container-item "${containerItemId}"`, result);
    } catch (e) {
      return this._failure(`Failed to load variants for container-item "${containerItemId}"`, e);
    }
  }

  /**
   * Add a new variant
   *
   * @param {*} componentId the id of the component
   * @param {*} formData the form-data of the variant
   * @param {string} personaId optional personaId
   * @param {Array<{ [characteristicId]: targetGroupValue }>} array of characteristic objects
   */
  async addVariant(componentId, formData, personaId, characteristics) {
    const page = this.PageStructureService.getPage();
    const component = page.getComponentById(componentId);
    const variantId = 'hippo-new-configuration';

    const newVariantId = this._createVariantId(personaId, characteristics);
    const headers = {
      lastModifiedTimestamp: component.lastModified,
      'Move-To': newVariantId,
    };

    try {
      const result = await this.HstService.doPutFormWithHeaders(formData, componentId, headers, variantId);
      return this._success(`Successfully created a new variant for component "${componentId}"`, result);
    } catch (e) {
      return this._failure(`Failed to create new variant for component "${componentId}"`, e);
    } finally {
      this.ChannelService.checkChanges();
    }
  }

  async updateVariant(componentId, formData, variant, personaId, characteristics) {
    const page = this.PageStructureService.getPage();
    const component = page.getComponentById(componentId);

    const encodedVariantId = encodeURIComponent(variant.id);
    const newVariantId = encodedVariantId === 'hippo-default'
      ? encodedVariantId
      : this._createVariantId(personaId, characteristics, variant.abvariantId);

    const headers = {
      lastModifiedTimestamp: component.lastModified,
      'Move-To': newVariantId,
    };

    try {
      const result = await this.HstService.doPutFormWithHeaders(formData, componentId, headers, encodedVariantId);
      result.data.newVariantId = newVariantId;
      return this._success(`Successfully updated variant "${variant.id}" for component "${componentId}"`, result);
    } catch (e) {
      return this._failure('Failed to update', e);
    } finally {
      this.ChannelService.checkChanges();
    }
  }

  _createVariantId(personaId = '', characteristics = [], ABVariantId) {
    if (!ABVariantId) {
      ABVariantId = Math.floor(new Date().getTime() / 1000);
    }

    const encodedCharacteristics = this._encodeCharacteristics(characteristics);
    const personaVariant = `${personaId}@${ABVariantId}`;
    const personaRules = encodedCharacteristics ? `$${encodedCharacteristics}` : '';
    return personaVariant + personaRules;
  }

  _encodeCharacteristics(rules) {
    if (!rules || rules.length === 0) {
      return '';
    }

    return JSON.stringify(['and'].concat(rules));
  }

  async deleteVariant(componentId, variantId) {
    const page = this.PageStructureService.getPage();
    const component = page.getComponentById(componentId);
    const encodedVariantId = encodeURIComponent(variantId);
    const headers = { lastModifiedTimestamp: component.lastModified };
    try {
      const result = await this.HstService.doDeleteWithHeaders(componentId, headers, encodedVariantId);
      return this._success(`Successfully removed variant "${variantId}" from container-item "${componentId}"`, result);
    } catch (e) {
      return this._failure(`Failed to remove variant "${variantId}" from container-item "${componentId}"`, e);
    } finally {
      this.ChannelService.checkChanges();
    }
  }

  async getPersonas() {
    const params = {
      collectors: Object.keys(this._collectors).join(','),
      ...this._getDefaultParams(),
    };

    try {
      const result = await this._execute('GET', ['personas'], null, params);
      return this._success('Personas loaded successfully', result);
    } catch (e) {
      return this._failure('Failed to load personas', e);
    }
  }

  async getCharacteristicsIDs() {
    try {
      const result = await this._execute('GET', ['characteristics'], null, this._getDefaultParams());
      return this._success('Characteristics IDs loaded successfully', result);
    } catch (e) {
      return this._failure('Failed to load characteristics IDs', e);
    }
  }

  async getCharacteristics() {
    const response = await this.getCharacteristicsIDs();
    if (!response.success) {
      return response;
    }

    try {
      const characteristics = await this.$q.all(response.data.items
        .map(item => item.id)
        .map(id => this.getCharacteristic(id)));

      return this._success('Characteristics loaded successfully', characteristics.map(ch => ch.data));
    } catch (e) {
      return this._failure('Failed to load characteristics');
    }
  }

  async getCharacteristic(id) {
    try {
      const result = await this._execute('GET', ['characteristics', id], null, this._getDefaultParams());
      return this._success(`Characteristic "${id}" loaded successfully`, result);
    } catch (e) {
      return this._failure(`Failed to load characteristic "${id}"`, e);
    }
  }

  async getExperiment(componentId) {
    try {
      const { locale } = this.ConfigService;
      const params = { ...this._getDefaultParams(), locale };
      const result = await this._execute('GET', ['experiments', 'component', componentId], null, params);
      return this._success(`Experiment loaded successfully for component "${componentId}"`, result);
    } catch (e) {
      return this._failure(`Failed to load experiment for component "${componentId}"`, e);
    }
  }

  async saveExperiment(componentId, goalId, variantId) {
    const description = `component "${componentId}" with goal "${goalId}" and variant "${variantId}"`;
    try {
      const result = await this._execute('POST', ['experiments', 'saveExperiment'], {
        componentId,
        goalId,
        variantId,
      }, this._getDefaultParams());
      await this.ChannelService.checkChanges();
      return this._success(`Experiment saved for ${description}`, result);
    } catch (e) {
      return this._failure(`Failed to save experiment for ${description}`, e);
    }
  }

  async completeExperiment(componentId, keepOnlyVariantId) {
    try {
      const params = { ...this._getDefaultParams(), keepOnlyVariantId };
      const result = await this._execute('POST', ['experiments', 'complete'], componentId, params);
      await this.ChannelService.checkChanges();
      return this._success(`Experiment completed for component "${componentId}"`, result);
    } catch (e) {
      return this._failure(`Failed to complete experiment for component "${componentId}"`, e);
    }
  }

  async getExperimentStatus(experimentId) {
    try {
      const params = this._getDefaultParams();
      const result = await this._execute('GET', ['experiments', 'serving', experimentId], null, params);
      return this._success(`Successfully loaded state of experiment "${experimentId}"`, result);
    } catch (e) {
      return this._failure(`Failed to load state of experiment "${experimentId}"`, e);
    }
  }

  async getGoals() {
    try {
      const result = await this._execute('GET', ['goals'], null, this._getDefaultParams());
      return this._success('Successfully loaded goals', result);
    } catch (e) {
      return this._failure('Failed to load goals', e);
    }
  }

  _getDefaultParams() {
    return {
      'Force-Client-Host': true,
      antiCache: Date.now(),
    };
  }

  _success(message, response) {
    return this._parseResponse(message, response, DEFAULT_SUCCESS_RESPONSE);
  }

  _failure(message, response) {
    return this._parseResponse(message, response, DEFAULT_FAILURE_RESPONSE);
  }

  _parseResponse(message, response, defaultProperties) {
    message = response.message || message;

    return response.hasOwnProperty('data')
      ? {
        ...defaultProperties,
        ...response,
        message,
      }
      : {
        ...defaultProperties,
        data: response,
        message,
      };
  }

  _execute(method, pathElements, data, params, headers = {}) {
    const url = this._createApiUrl(pathElements, params);
    const canceller = this.$q.defer();
    const promise = this.$q((resolve, reject) => {
      this.$http({
        method,
        url,
        headers,
        data,
        timeout: canceller.promise,
      })
        .then(response => resolve(response.data))
        .catch(error => reject(error.data));
    });

    promise.cancel = () => {
      canceller.resolve();

      return promise;
    };

    return promise;
  }

  _createApiUrl(pathElements = [], params) {
    pathElements.unshift(this._apiUrl);

    let apiUrl = this.PathService.concatPaths(...pathElements);

    if (params) {
      apiUrl += `?${this._serializeParams(params)}`;
    }

    return apiUrl;
  }

  _serializeParams(params) {
    const str = Object.keys(params)
      .map(param => `${encodeURIComponent(param)}=${encodeURIComponent(params[param])}`);
    return str.join('&');
  }
}

export default TargetingService;
