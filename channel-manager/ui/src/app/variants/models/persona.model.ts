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

export interface PersonaAlterEgoDataPoint {
  altered: boolean;
  colectorId: string;
  data: any;
}

export enum PersonaRuleType {
  AND_RULE = 'AND_RULE',
  OR_RULE = 'OR_RULE',
}

export interface PersonaRule {
  characteristic: string;
  targetGroupId: string;
  targetGroupName: string;
  type: PersonaRuleType;
}

export interface Persona {
  alterEgoData: PersonaAlterEgoDataPoint[];
  avatar: string;
  description: string;
  id: string;
  name: string;
  quote: string;
  rules: PersonaRule[];
  segmentDescription: string;
  segmentName: string;
}
