import { NgModule } from '@angular/core';
import { ImageLinkComponent } from './imageLink/imageLink.component';
import { MaterialModule } from '../../../../material/material.module';

@NgModule({
  imports: [
    MaterialModule
  ],
  declarations: [
    ImageLinkComponent
  ],
  entryComponents: [
    ImageLinkComponent
  ]
})
export class FieldsModule { }
