package com.nitramite.courier.ups;

public class UpsTokenPair {

    private String X_CSRF_TOKEN = null;
    private String X_XSRF_TOKEN_ST = null;

    public UpsTokenPair(String X_CSRF_TOKEN_, String X_XSRF_TOKEN_ST_) {
        X_CSRF_TOKEN = X_CSRF_TOKEN_;
        X_XSRF_TOKEN_ST = X_XSRF_TOKEN_ST_;
    }

    public String getX_CSRF_TOKEN() {
        return X_CSRF_TOKEN != null ? "X-CSRF-TOKEN=" + X_CSRF_TOKEN : null;
    }

    public String getX_XSRF_TOKEN_ST() {
        return X_XSRF_TOKEN_ST;
    }

}
