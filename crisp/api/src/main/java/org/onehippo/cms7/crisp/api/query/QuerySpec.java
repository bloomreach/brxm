package org.onehippo.cms7.crisp.api.query;

public class QuerySpec {

    private Long limit;

    private long offset;

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

}
