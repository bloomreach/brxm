import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import './create-content.scss';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'hippo-create-content',
  templateUrl: './create-content.html'
})
export class CreateContentComponent implements OnInit {
  docTypes: any;
  @Input() document: any;
  @Output() onClose: EventEmitter<any> = new EventEmitter();
  @Output() onContinue: EventEmitter<any> = new EventEmitter();

  constructor() {}

  ngOnInit() {
    this.docTypes = ['Product', 'Event'];
  }

  close() {
    this.onClose.emit();
  }

  submit(form: NgForm) {
    this.onContinue.emit(form.value);
  }
}
