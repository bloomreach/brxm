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

export class ViewAsCtrl {
  constructor($scope, $translate, ConfigService, SessionService, HstService) {
    'ngInject';

    this.$translate = $translate;
    this.ConfigService = ConfigService;
    this.HstService = HstService;

    this.globalVariants = [];

    this._retrieveGlobalVariants();

    // In order to have a way to trigger the reloading of the global variants, we tie the reloading
    // to a successful SessionService.initialize call, which happens upon channel switching.
    SessionService.registerInitCallback('reloadGlobalVariants', () => this._retrieveGlobalVariants());
    $scope.$on('$destroy', () => SessionService.unregisterInitCallback('reloadGlobalVariants'));
  }

  _retrieveGlobalVariants() {
    if (this.ConfigService.variantsUuid) {
      const params = {
        locale: this.ConfigService.locale,
      };
      this.HstService.doGetWithParams(this.ConfigService.variantsUuid, params, 'globalvariants').then((response) => {
        if (response && response.data) {
          this.globalVariants = response.data;
          this._updateSelectedVariant();
        }
      });
      // TODO: show a toast upon error?
    }
  }

  _updateSelectedVariant() {
    const oldSelectedVariantId = this.selectedVariant && this.selectedVariant.id;

    if (oldSelectedVariantId) {
      this.selectedVariant = this.globalVariants.find((variant) => (variant.id === oldSelectedVariantId));
    }

    // fallback to "Default" which is sorted first by the backend.
    if (!this.selectedVariant) {
      this.selectedVariant = this.globalVariants[0];
    }
  }

  makeDisplayName(variant) {
    if (variant.group) {
      return `${variant.name}${this.$translate.instant('TOOLBAR_VIEW_AS_INFIX')}${variant.group}`;
    }

    return variant.name;
  }
}
