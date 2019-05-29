import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { OverlayComponent } from './components/overlay/overlay.component';
import { MaterialModule } from './material/material.module';

@NgModule({
  imports: [
    CommonModule,
  ],
  declarations: [
    OverlayComponent,
  ],
  exports: [
    MaterialModule,
    OverlayComponent,
  ],
})
export class SharedModule {}
