package org.onehippo.cms7.crisp.api.query;

public class QuerySpecBuilder {

    private QuerySpecBuilder() {
    }

    public static QuerySpecBuilder create() {
        return new QuerySpecBuilder();
    }

    private long offset;
    private Long limit;

    public QuerySpecBuilder offset(long offset) {
        this.offset = offset;
        return this;
    }

    public QuerySpecBuilder limit(Long limit) {
        this.limit = limit;
        return this;
    }

    public QuerySpec build() {
        QuerySpec spec = new QuerySpec();
        spec.setOffset(offset);
        spec.setLimit(limit);
        return spec;
    }
}
