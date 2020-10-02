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

import { ExperimentVariant } from '../models/experiment-variant.model';
import { Experiment } from '../models/experiment.model';

const sameSegmentAndCharacteristicsRegEx = /(.*)-[A-Z]$/;

@Pipe({name: 'experimentName'})
export class ExperimentNamePipe implements PipeTransform {
  transform(value: Experiment | undefined): string {
    if (!value || !value.variants || value.variants.length === 0) {
      return '';
    }

    const {
      comparedAgainstDefaultVariantName,
      groupedVariantsName,
      groupedVariantsCounter,
    } = this.groupVariantNames(value.variants);

    if (comparedAgainstDefaultVariantName) {
      return comparedAgainstDefaultVariantName;
    }

    return `${groupedVariantsName} (${groupedVariantsCounter} variants)`;
  }

  // For simplification the algorithm takes into account the behaviour of variants:
  // - it's possible to either run one variant against the default variant or
  // - run many variants with the same segment and characteristics against each other
  //   so each variant has a suffix in this care (-A, -B, -C and etc, the whole alphabet)
  private groupVariantNames(variants: ExperimentVariant[]): {
    comparedAgainstDefaultVariantName: string,
    groupedVariantsName: string,
    groupedVariantsCounter: number,
  } {
    let comparedAgainstDefaultVariantName = '';
    let groupedVariantsName = '';
    let groupedVariantsCounter = 0;

    for (const variant of variants) {
      const found = variant.variantName.match(sameSegmentAndCharacteristicsRegEx);

      if (!found) {
        comparedAgainstDefaultVariantName = variant.variantName;

        continue;
      }

      [, groupedVariantsName] = found;
      groupedVariantsCounter++;
    }

    return {
      comparedAgainstDefaultVariantName,
      groupedVariantsName,
      groupedVariantsCounter,
    } as const;
  }
}
