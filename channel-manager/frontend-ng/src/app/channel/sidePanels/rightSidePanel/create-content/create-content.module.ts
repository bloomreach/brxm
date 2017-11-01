import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { HintsComponent } from '../../../../shared/components/hints/hints.component';
import { CreateContentComponent } from './step-1/step-1.component';
import { CreateContentService } from './create-content.service';
import { SharedModule } from '../../../../shared/shared.module';
import { NameUrlFieldsComponent } from './name-url-fields/name-url-fields.component';

@NgModule({
  imports: [
    SharedModule,
    FormsModule
  ],
  declarations: [
    CreateContentComponent,
    HintsComponent,
    NameUrlFieldsComponent
  ],
  entryComponents: [
    CreateContentComponent,
    HintsComponent,
    NameUrlFieldsComponent
  ],
  providers: [
    CreateContentService
  ]
})
export class CreateContentModule {}
