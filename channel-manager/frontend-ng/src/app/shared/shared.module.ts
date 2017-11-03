import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MaterialModule } from './material/material.module';

import { FeedbackServiceProvider } from './services/feedback.service.provider';
import { ContentServiceProvider } from './services/content.service.provider';
import { FieldServiceProvider } from './services/field.service.provider';
import { DialogServiceProvider } from './services/dialog.service.provider';

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    MaterialModule,
    TranslateModule
  ],
  exports: [
    BrowserModule,
    BrowserAnimationsModule,
    MaterialModule,
    TranslateModule
  ],
  providers: [
    ContentServiceProvider,
    FeedbackServiceProvider,
    FieldServiceProvider,
    DialogServiceProvider,
  ]
})
export class SharedModule {}
