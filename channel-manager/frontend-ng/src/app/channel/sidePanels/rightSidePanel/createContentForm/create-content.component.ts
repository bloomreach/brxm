import { Component, OnInit, EventEmitter, Input, Output, AfterViewInit } from '@angular/core';
import './create-content.scss';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'hippo-create-content',
  templateUrl: './create-content.html'
})
export class CreateContentComponent implements AfterViewInit {
  @Input() document: any;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();

  docTypes: Array<string> = [];

  ngAfterViewInit() {
    // this.urlInputSubscription = Observable.fromEvent(this.input.nativeElement, 'keyup')
    //   .debounceTime(1000)
    //   .subscribe(e => this.validateUrl(this.input.nativeElement.value));
    this.docTypes = ['Product', 'Event'];
  }

  close() {
    this.onClose.emit();
  }

  submit(form: NgForm) {
    console.log(form);
    // this.onContinue.emit(form.value);
  }
}
