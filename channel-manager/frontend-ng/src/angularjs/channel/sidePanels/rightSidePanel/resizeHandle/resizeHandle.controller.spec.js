fdescribe('resizeHandle component', () => {
  let $componentController;
  let $ctrl;

  let mockHandleElement;
  let mockSidePanelElement;
  let mockDocumentElement;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    jasmine.getFixtures().load('channel/sidePanels/rightSidePanel/resizeHandle/resizeHandle.controller.fixture.html');
    mockHandleElement = $j('#resizeHandle');
    mockSidePanelElement = $j('#sidePanel');
    mockDocumentElement = $j('<div></div>');

    $ctrl = $componentController('resizeHandle', {
      $element: mockHandleElement,
      $document: mockDocumentElement,
    }, {
      element: mockSidePanelElement,
      onResize: () => { },
    });

    spyOn($ctrl, '_blinkFullWidthToggle');
    spyOn($ctrl, 'onResize');
  });

  it('should initialize the component', () => {
    spyOn($ctrl, '_registerEvents');
    $ctrl.$onInit();
    expect($ctrl._registerEvents).toHaveBeenCalled();
  });

  it('should register events', () => {
    $ctrl._registerEvents(mockSidePanelElement);

    const eMouseDown = new $j.Event('mousedown');
    eMouseDown.pageX = 500;
    eMouseDown.pageY = 100;

    mockHandleElement.trigger(eMouseDown);

    mockSidePanelElement.width('440px');
    expect(mockSidePanelElement.width()).toEqual(440);

    const eMouseMove = new $j.Event('mousemove');
    eMouseMove.pageX = 800;
    eMouseMove.pageY = 100;

    $ctrl.$document.trigger(eMouseMove);

    $ctrl.$document.trigger('mouseup');
    expect($ctrl.onResize).toHaveBeenCalled();
    expect(mockSidePanelElement.width() > 440).toEqual(true);
    expect($ctrl._blinkFullWidthToggle).not.toHaveBeenCalled();
  });

  it('should not allow a too small or too big sidepanel and should show a visual blinking hint', () => {
    $ctrl._registerEvents(mockSidePanelElement);

    const eMouseDown = new $j.Event('mousedown');
    eMouseDown.pageX = 500;
    eMouseDown.pageY = 100;

    mockHandleElement.trigger(eMouseDown);

    mockSidePanelElement.width('440px');
    expect(mockSidePanelElement.width()).toEqual(440);

    spyOn(mockSidePanelElement, 'css');

    const eMouseMove = new $j.Event('mousemove');
    eMouseMove.pageX = 3000;
    eMouseMove.pageY = 100;

    $ctrl.$document.trigger(eMouseMove);

    $ctrl.$document.trigger('mouseup');
    expect($ctrl.onResize).toHaveBeenCalled();
    expect(mockSidePanelElement.css).not.toHaveBeenCalled();
    expect($ctrl._blinkFullWidthToggle).toHaveBeenCalled();
  });
});
