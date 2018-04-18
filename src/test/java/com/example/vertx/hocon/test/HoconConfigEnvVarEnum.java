package com.example.vertx.hocon.test;

public enum HoconConfigEnvVarEnum {
    ENV_VAR_B("2"),
    ENV_VAR_D("D"),
    ENV_VAR_E("E"),
    ENV_VAR_F("f3");

    private String testValue;

    HoconConfigEnvVarEnum(String value) {
        this.testValue = value;
    }

    public String getTestValue() {
        return this.testValue;
    }

}
