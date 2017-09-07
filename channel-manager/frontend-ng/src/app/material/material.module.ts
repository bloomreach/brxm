import { NgModule } from '@angular/core';
import {
  NoConflictStyleCompatibilityMode,
  MdListModule,
  MdButtonModule,
} from '@angular/material';
import './material.scss';

@NgModule({
  exports: [
    NoConflictStyleCompatibilityMode,
    MdListModule,
    MdButtonModule,
  ]
})
export class MaterialModule { }
