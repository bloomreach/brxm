/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export default {
  success: true,
  message: null,
  reloadRequired: false,
  errorCode: null,
  data: {
    id: 'experiment-1',
    state: 'RUNNING',
    startTime: 1600868196063,
    winnerVariant: null,
    variants: [
      {
        variantName: 'Default',
        confidence: 0.46135386461353867,
        variantId: 'hippo-default',
        mean: 0.005696835789030305,
        variance: 2.6351955466651E-5,
        visitorSegment: 'Default',
      },
      {
        variantName: 'Dutch',
        confidence: 0.5386461353864613,
        variantId: 'dirk-1440145443062@1440146285',
        mean: 0.006271339704953255,
        variance: 2.7988419340842734E-5,
        visitorSegment: 'Dutch',
      },
    ],
    goal: {
      id: 'goal-2',
      name: 'Goal 2',
      type: 'PAGE',
      readOnly: true,
      targetPage: '/events',
      mountId: '1a1a1a1a-e880-4629-9e63-bc8ed8399d2a',
    },
    type: 'PAGE',
  },
};
