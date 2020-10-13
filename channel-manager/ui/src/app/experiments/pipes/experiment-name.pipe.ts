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

import { Pipe, PipeTransform } from '@angular/core';

import { VariantsService } from '../../variants/services/variants.service';
import { Experiment } from '../models/experiment.model';

@Pipe({name: 'experimentName'})
export class ExperimentNamePipe implements PipeTransform {
  constructor(private readonly variantsService: VariantsService) {}

  transform(value: Experiment | undefined): string {
    if (!value || !value.variants || value.variants.length === 0) {
      return '';
    }

    const preparedVariants = value.variants.map(v => ({ id: v.variantId, name: v.variantName }));
    const [variant] = this.variantsService.groupVariants(preparedVariants);

    return variant.name;
  }
}
