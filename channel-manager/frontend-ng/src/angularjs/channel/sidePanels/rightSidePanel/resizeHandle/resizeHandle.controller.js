class resizeHandleController {
  constructor($element, $document) {
    'ngInject';

    this.$document = $document;
    this.handle = $element;
  }

  $onInit() {
    this._registerEvents(this.element);
  }

  _registerEvents() {
    this.handle.mousedown((e) => {
      const hippoIframe = $('hippo-iframe').find('iframe');
      const initialWidth = this.element.width();
      const initialX = e.clientX;
      console.log(`down. x: ${initialX}`);

      hippoIframe.css('pointer-events', 'none');

      this.$document.mousemove((e) => {
        console.log(`up. x: ${e.pageX}`);
        const diff = initialX - e.pageX;
        console.log(diff);

        const newWidth = initialWidth + diff;

        this.element.css('width', newWidth);
        this.element.css('max-width', newWidth);

        this.$document.mouseup(() => {
          this.$document.unbind('mousemove');
          hippoIframe.css('pointer-events', 'auto');
        });
      });
    });
  }
}

export default resizeHandleController;
