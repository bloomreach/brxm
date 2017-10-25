import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MaterialModule } from '../material/material.module';

import { FeedbackServiceProvider } from './services/feedback.service.provider';
import { ContentServiceProvider } from './services/content.service.provider';

@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    MaterialModule
  ],
  exports: [
    BrowserModule,
    BrowserAnimationsModule,
    MaterialModule,
  ],
  providers: [
    ContentServiceProvider,
    FeedbackServiceProvider
  ]
})
export class SharedModule {}
