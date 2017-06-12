describe('field service', () => {
  let $timeout;
  let FieldService;
  let ContentService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$timeout_, _FieldService_, _ContentService_) => {
      $timeout = _$timeout_;
      FieldService = _FieldService_;
      ContentService = _ContentService_;
    });
  });

  it('should start draft timer for field', () => {
    spyOn(FieldService, '_clearFieldTimer');
    spyOn(FieldService, 'draftField');
    FieldService.setDocumentId('mockDocumentId');

    expect(FieldService.activeDraftTimers).toEqual({});

    FieldService.startDraftTimer('mockFieldName', 'mockValue');

    expect(FieldService._clearFieldTimer).toHaveBeenCalled();

    $timeout.flush();
    expect(FieldService.draftField).toHaveBeenCalled();
  });

  it('should draft field', () => {
    spyOn(ContentService, 'draftField');
    spyOn(FieldService, '_clearFieldTimer');
    spyOn(FieldService, '_cleanupTimers');
    FieldService.setDocumentId('mockDocumentId');

    FieldService.draftField('mockFieldName', 'mockValue');

    expect(FieldService._clearFieldTimer).toHaveBeenCalled();
    expect(FieldService._cleanupTimers).toHaveBeenCalled();
    expect(ContentService.draftField).toHaveBeenCalledWith('mockDocumentId', 'mockFieldName', 'mockValue');
  });

  it('should clear field timer if exists', () => {
    FieldService.activeDraftTimers.mockDocumentId = {};
    FieldService.activeDraftTimers.mockDocumentId.mockFieldName = () => { angular.noop(); };

    spyOn($timeout, 'cancel');

    FieldService._clearFieldTimer('mockDocumentId', 'mockFieldName');

    expect($timeout.cancel).toHaveBeenCalled();
    expect(FieldService.activeDraftTimers.mockDocumentId).toEqual({});
  });

  it('should clean up timers', () => {
    FieldService.activeDraftTimers.mockDocumentId = {};
    FieldService._cleanupTimers('mockDocumentId');
    expect(FieldService.activeDraftTimers.mockDocumentId).not.toBeDefined();
  });

  it('should set document id', () => {
    expect(FieldService.documentId).toEqual(null);
    FieldService.setDocumentId('mockDocumentId');
    expect(FieldService.documentId).toEqual('mockDocumentId');
  });

  it('should return document id', () => {
    expect(FieldService.getDocumentId()).toEqual(null);
    FieldService.setDocumentId('mockDocumentId');
    expect(FieldService.getDocumentId()).toEqual('mockDocumentId');
  });
});
