package com.example.vertx.hocon.test;

public enum HoconConfigEnvVarEnum {
    HTTP_PORT("8081"),
    RESPONSE_TITLE("Response title"),
    RESPONSE_BODY("Response body"),
    ROUTES("/json");

    private String testValue;

    HoconConfigEnvVarEnum(String value) {
        this.testValue = value;
    }

    public String getTestValue() {
        return this.testValue;
    }

}
