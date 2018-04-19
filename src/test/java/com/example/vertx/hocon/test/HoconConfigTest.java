package com.example.vertx.hocon.test;

import com.typesafe.config.impl.ConfigImpl;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class HoconConfigTest {

    private static final JsonObject EXPECTED_DEFAULT_CONFIG = new JsonObject()
            .put("httpServer", new JsonObject()
                    .put("port", 8080)
                    .put("answer", new JsonObject()
                            .put("title", "Some title")
                            .put("body", "Some body")
                            .put("routes", new JsonArray()
                                    .add("/")
                                    .add("/all"))));

    private static final JsonObject EXPECTED_REWRITTEN_CONFIG = new JsonObject()
            .put("httpServer", new JsonObject()
                    .put("port", "8081")
                    .put("answer", new JsonObject()
                            .put("title", "Response title")
                            .put("body", "Response body")
                            .put("routes", new JsonArray()
                                    .add("/")
                                    .add("/all")
                                    .add("/json"))));

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void testDefaultHoconConfig(TestContext context) {
        Vertx vertx = Vertx.vertx();
        testConfigImpl(context, vertx, EXPECTED_DEFAULT_CONFIG);
    }

    @Test
    @PrepareForTest({ System.class, ConfigImpl.class })
    public void testHoconConfigWithMockedEnvironmentVariables(TestContext context) {
        mockStatic(System.class);

        Map<String, String> envProps = new HashMap<>();
        Arrays.stream(HoconConfigEnvVarEnum.values()).forEach(confEnvVarName -> envProps.put(confEnvVarName.name(),
                confEnvVarName.getTestValue()));

        when(System.getenv()).thenReturn(envProps);

        Vertx vertx = Vertx.vertx();
        testConfigImpl(context, vertx, EXPECTED_REWRITTEN_CONFIG);
    }

    @Test
    @PrepareForTest({ System.class, ConfigImpl.class })
    public void testHoconConfigWithInjectedEnvironmentVariables(TestContext context) throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> envProps = getModifiableEnvPropMap();
        Arrays.stream(HoconConfigEnvVarEnum.values()).forEach(confEnvVarName -> envProps.put(confEnvVarName.name(),
                confEnvVarName.getTestValue()));

        Vertx vertx = Vertx.vertx();
        testConfigImpl(context, vertx, EXPECTED_REWRITTEN_CONFIG);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getModifiableEnvPropMap() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> unmodifiableEnvPropMap = System.getenv();
        Class<?> clazz = unmodifiableEnvPropMap.getClass();
        Field unmodifiableMapField = clazz.getDeclaredField("m");
        unmodifiableMapField.setAccessible(true);
        return (Map<String, String>) unmodifiableMapField.get(unmodifiableEnvPropMap);
    }

    private void testConfigImpl(TestContext context, Vertx vertx, JsonObject expectedJson) {
        ConfigLoadTestVerticle verticle = new ConfigLoadTestVerticle();
        context.assertNull(verticle.getConfiguration());

        vertx.deployVerticle(verticle, new DeploymentOptions(), context.asyncAssertSuccess(id -> {
            JsonObject configuration = verticle.getConfiguration();

            context.assertNotNull(configuration);
            context.assertEquals(expectedJson, configuration);

        }));
    }

}