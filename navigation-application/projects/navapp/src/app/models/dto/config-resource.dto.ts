/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

/**
 * ConfigResource is a resource where navitems / login / logout urls are provided
 * they can be of different types, for type IFRAME an iframe is created and a connection is made
 * with the navapp communication library to retrieve the necessary information.
 */
export interface ConfigResource {
  /**
   * The type of resource
   */
  resourceType: 'IFRAME' | 'REST' | 'INTERNAL_REST';

  /**
   * The URL of the resource
   */
  url: string;
}
