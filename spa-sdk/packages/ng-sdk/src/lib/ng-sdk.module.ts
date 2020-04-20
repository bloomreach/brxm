/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';

import { BrContainerItemUndefinedComponent } from './br-container-item-undefined/br-container-item-undefined.component';
import { BrComponentDirective } from './br-component.directive';
import { BrNodeTypePipe } from './br-node-type.pipe';
import { BrNodeComponentDirective } from './br-node-component.directive';
import { BrNodeContainerDirective } from './br-node-container.directive';
import { BrNodeContainerItemDirective } from './br-node-container-item.directive';
import { BrNodeDirective } from './br-node.directive';
import { BrPageComponent } from './br-page/br-page.component';

@NgModule({
  declarations: [
    BrComponentDirective,
    BrContainerItemUndefinedComponent,
    BrNodeComponentDirective,
    BrNodeContainerDirective,
    BrNodeContainerItemDirective,
    BrNodeDirective,
    BrNodeTypePipe,
    BrPageComponent,
  ],
  entryComponents: [
    BrContainerItemUndefinedComponent,
  ],
  exports: [
    BrComponentDirective,
    BrPageComponent,
  ],
  imports: [ CommonModule, HttpClientModule ],
})
export class NgSdkModule { }
