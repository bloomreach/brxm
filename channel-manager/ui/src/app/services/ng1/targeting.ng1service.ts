/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { InjectionToken } from '@angular/core';

import { ApiResponseBody } from '../../models/api-response-body';
import { Variant, VariantCharacteristicData } from '../../variants/models/variant.model';

export interface Ng1TargetingService {
  getCharacteristics(): Promise<ApiResponseBody<void>>;
  getVariants(containerItemId: string): Promise<ApiResponseBody<Variant[]>>;
  addVariant(
    componentId: string,
    formData: any,
    personaId?: string,
    characteristics?: VariantCharacteristicData[],
  ): Promise<ApiResponseBody<any>>;
}

export const NG1_TARGETING_SERVICE = new InjectionToken<Ng1TargetingService>('NG1_TARGETING_SERVICE');
