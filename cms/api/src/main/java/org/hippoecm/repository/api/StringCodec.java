package org.hippoecm.repository.api;

public interface StringCodec {
    public String encode(String plain);
    public String decode(String encoded);
}
