/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.jinsight.modules.log4j;

import static ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker.APPENDS_BASE_NAME;
import static ai.apptuit.metrics.jinsight.modules.logback.LogEventTracker.THROWABLES_BASE_NAME;
import static org.junit.Assert.assertEquals;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Rajiv Shivane
 */
public class Log4JInstrumentationTest {

  private MetricRegistry registry;
  private Map<String, Meter> meters;
  private Logger logger;
  private Level origLevel;

  @Before
  public void setUp() throws Exception {
    registry = RegistryService.getMetricRegistry();

    meters = new HashMap<>();
    meters.put("total", getMeter(APPENDS_BASE_NAME.submetric("total")));
    meters.put("trace", getMeter(APPENDS_BASE_NAME.withTags("level", "trace")));
    meters.put("debug", getMeter(APPENDS_BASE_NAME.withTags("level", "debug")));
    meters.put("info", getMeter(APPENDS_BASE_NAME.withTags("level", "info")));
    meters.put("warn", getMeter(APPENDS_BASE_NAME.withTags("level", "warn")));
    meters.put("error", getMeter(APPENDS_BASE_NAME.withTags("level", "error")));
    meters.put("fatal", getMeter(APPENDS_BASE_NAME.withTags("level", "fatal")));
    meters.put("throwCount", getMeter(THROWABLES_BASE_NAME.submetric("total")));
    meters.put("throw[RuntimeException]", getMeter(
        THROWABLES_BASE_NAME
            .withTags("class", RuntimeException.class.getName())
    ));

    logger = Logger.getLogger(Log4JInstrumentationTest.class.getName());
    origLevel = logger.getLevel();
    logger.setLevel(Level.ALL);
  }

  @After
  public void tearDown() throws Exception {
    logger.setLevel(origLevel);
  }

  @Test
  public void testThrowable() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);
    expectedCounts.compute("throwCount", (s, aLong) -> aLong + 1);
    expectedCounts.compute("throw[RuntimeException]", (s, aLong) -> aLong + 1);

    RuntimeException exception = new RuntimeException();
    exception.setStackTrace(new StackTraceElement[0]);
    logger.error("Error with throwable", exception);

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLogTrace() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("trace", (s, aLong) -> aLong + 1);

    logger.trace("TRACE!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogDebug() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("debug", (s, aLong) -> aLong + 1);

    logger.debug("DEBUG!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogInfo() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("info", (s, aLong) -> aLong + 1);

    logger.info("INFO!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogWarn() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("warn", (s, aLong) -> aLong + 1);

    logger.warn("WARN!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLogError() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);

    logger.error("ERROR!");

    assertEquals(expectedCounts, getCurrentCounts());
  }


  @Test
  public void testLogFatal() throws Exception {
    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("fatal", (s, aLong) -> aLong + 1);

    logger.fatal("FATAL!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLogLevel() throws Exception {
    logger.setLevel(Level.ERROR);

    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 2);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);
    expectedCounts.compute("fatal", (s, aLong) -> aLong + 1);

    logger.trace("trace!");
    logger.debug("debug!");
    logger.info("info!");
    logger.warn("warn!");
    logger.error("error!");
    logger.fatal("fatal!");

    assertEquals(expectedCounts, getCurrentCounts());
  }

  @Test
  public void testLoggerReconfiguration() throws Exception {
    logger.setLevel(Level.ERROR);

    Map<String, Long> expectedCounts = getCurrentCounts();
    expectedCounts.compute("total", (s, aLong) -> aLong + 1);
    expectedCounts.compute("error", (s, aLong) -> aLong + 1);

    Properties properties = new Properties();
    properties.setProperty("log4j.rootCategory", "INFO,TestLog");
    properties.setProperty("log4j.appender.TestLog", "org.apache.log4j.ConsoleAppender");
    properties.setProperty("log4j.appender.TestLog.layout", "org.apache.log4j.PatternLayout");
    PropertyConfigurator.configure(properties);

    logger.error("error!");
    assertEquals(expectedCounts, getCurrentCounts());

  }

  private Meter getMeter(TagEncodedMetricName name) {
    return registry.meter(name.toString());
  }

  private Map<String, Long> getCurrentCounts() {
    Map<String, Long> currentValues = new HashMap<>(meters.size());
    meters.forEach((k, meter) -> {
      currentValues.put(k, meter.getCount());
    });
    return currentValues;
  }
}
