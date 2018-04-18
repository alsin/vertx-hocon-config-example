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
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class HoconConfigTest {

    private static final JsonObject EXPECTED_DEFAULT_CONFIG = new JsonObject()
            .put("a", new JsonObject()
                    .put("b", 1)
                    .put("c", new JsonObject()
                            .put("d", "d-Value")
                            .put("e", "e-Value")
                            .put("f", new JsonArray()
                                    .add("f1")
                                    .add("f2"))));

    private static final JsonObject EXPECTED_REWRITTEN_CONFIG = new JsonObject()
            .put("a", new JsonObject()
                    .put("b", "2")
                    .put("c", new JsonObject()
                            .put("d", "D")
                            .put("e", "E")
                            .put("f", new JsonArray()
                                    .add("f1")
                                    .add("f2")
                                    .add("f3"))));

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

    private String getHoconConfigPath() throws URISyntaxException {
        return Paths.get(HoconConfigTest.class.getResource("test.conf").toURI())
                .toFile()
                .getAbsolutePath();
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