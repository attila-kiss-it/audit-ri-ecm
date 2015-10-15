/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.audit.ri.ecm.tests;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.everit.audit.AuditEventTypeManager;
import org.everit.audit.LoggingService;
import org.everit.audit.dto.AuditEvent;
import org.everit.audit.dto.EventData;
import org.everit.audit.dto.EventData.Builder;
import org.everit.audit.dto.EventDataType;
import org.everit.audit.ri.AuditApplicationManager;
import org.everit.audit.ri.InternalAuditEventTypeManager;
import org.everit.audit.ri.InternalLoggingService;
import org.everit.audit.ri.UnknownAuditApplicationException;
import org.everit.audit.ri.authorization.AuditRiAuthorizationManager;
import org.everit.audit.ri.authorization.AuditRiPermissionChecker;
import org.everit.audit.ri.authorization.AuditRiPermissionConstants;
import org.everit.audit.ri.dto.AuditApplication;
import org.everit.audit.ri.ecm.AuditRiComponentConstants;
import org.everit.audit.ri.props.AuditRiPropertyConstants;
import org.everit.audit.ri.schema.qdsl.QApplication;
import org.everit.audit.ri.schema.qdsl.QEvent;
import org.everit.audit.ri.schema.qdsl.QEventData;
import org.everit.audit.ri.schema.qdsl.QEventType;
import org.everit.authentication.context.AuthenticationPropagator;
import org.everit.authnr.permissionchecker.UnauthorizedException;
import org.everit.authorization.PermissionChecker;
import org.everit.authorization.ri.schema.qdsl.QPermission;
import org.everit.authorization.ri.schema.qdsl.QPermissionInheritance;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.component.ServiceHolder;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.everit.persistence.querydsl.support.QuerydslSupport;
import org.everit.props.PropertyManager;
import org.everit.props.ri.schema.qdsl.QProperty;
import org.everit.resource.ResourceService;
import org.everit.resource.ri.schema.qdsl.QResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.LogService;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.sql.dml.SQLDeleteClause;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * Tests for the audit-ri and ECM components.
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL)
@ProvideCapability(ns = ECMExtenderConstants.CAPABILITY_NS_COMPONENT,
    value = ECMExtenderConstants.CAPABILITY_ATTR_CLASS + "=${@class}")
@StringAttributes({
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE,
        defaultValue = "junit4"),
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID,
        defaultValue = "AuthorizationBasicTest") })
@Service(AuditComponentTest.class)
@TestDuringDevelopment
public class AuditComponentTest {

  private static final int EXPECTED_EVENT_DATA_LIST_SIZE = 4;

  private static final int HUNDRED = 100;

  private static final int NUMBER_INDEX = 2;

  private static final String NUMBER_N = "number";

  private static final double NUMBER_V = 10.75;

  private static final int STRING_INDEX = 0;

  private static final String STRING_N = "string";

  private static final String STRING_V = "string-value";

  private static final int TEN_THOUSAND = 10000;

  private static final int TEXT_INDEX = 1;

  private static final String TEXT_N = "text";

  private static final String TEXT_V = "text-value";

  private static final int TIMESTAMP_INDEX = 3;

  private static final String TIMESTAMP_N = "timestamp";

  private static final Instant TIMESTAMP_V = Instant.now();

  private Map<?, ?> auditApplicationCache;

  private AuditApplicationManager auditApplicationManager; // check

  private Map<?, ?> auditEventTypeCache;

  private AuditEventTypeManager auditEventTypeManager; // check

  private AuditRiAuthorizationManager auditRiAuthorizationManager; // check

  private AuditRiPermissionChecker auditRiPermissionChecker; // check

  private AuthenticationPropagator authenticationPropagator;

  private String embeddedAuditApplicationName;

  private InternalAuditEventTypeManager internalAuditEventTypeManager; // check

  private InternalLoggingService internalLoggingService;

  private LoggingService loggingService; // check

  private LogService logService;

  private PermissionChecker permissionChecker;

  private PropertyManager propertyManager;

  private QuerydslSupport querydslSupport;

  private ResourceService resourceService;

  /**
   * Cleans up the database and caches after a test.
   */
  @After
  public void after() {
    clearAllAuditEventTypes();
    clearPermissions();
    clearAuditCaches();
  }

  private void assertAuditApplicationExists(final String expectedApplicationName) {

    String actualApplicationName = querydslSupport.execute((connection, configuration) -> {

      QApplication qApplication = QApplication.application;

      return new SQLQuery(connection, configuration)
          .from(qApplication)
          .where(qApplication.applicationName.eq(expectedApplicationName))
          .uniqueResult(qApplication.applicationName);

    });

    Assert.assertEquals(expectedApplicationName, actualApplicationName);
  }

  private void assertAuditEventTypesExist(final String applicationName,
      final String... expectedEventTypeNames) {

    List<String> actualEventTypeNames = querydslSupport.execute((connection, configuration) -> {

      QEventType qEventType = QEventType.eventType;
      QApplication qApplication = QApplication.application;

      return new SQLQuery(connection, configuration)
          .from(qEventType)
          .innerJoin(qApplication).on(qApplication.applicationId.eq(qEventType.applicationId))
          .where(qEventType.eventTypeName.in(expectedEventTypeNames)
              .and(qApplication.applicationName.eq(applicationName)))
          .list(qEventType.eventTypeName);

    });

    Assert.assertArrayEquals(expectedEventTypeNames,
        actualEventTypeNames.toArray(new String[actualEventTypeNames.size()]));
  }

  private void assertEvent(final String eventTypeName) {
    List<EventData> eventDataList = querydslSupport.execute((connection, configuration) -> {

      QEvent qEvent = QEvent.event;
      QEventType qEventType = QEventType.eventType;
      QEventData qEventData = QEventData.eventData;

      List<Tuple> tuples = new SQLQuery(connection, configuration)
          .from(qEvent)
          .innerJoin(qEventType)
          .on(qEventType.eventTypeId.eq(qEvent.eventTypeId))
          .innerJoin(qEventData)
          .on(qEventData.eventId.eq(qEvent.eventId))
          .where(qEventType.eventTypeName.eq(eventTypeName))
          .orderBy(qEventData.eventDataId.asc())
          .list(qEventData.eventDataName, qEventData.eventDataType,
              qEventData.stringValue,
              qEventData.textValue,
              qEventData.numberValue,
              qEventData.timestampValue);

      List<EventData> rval = new ArrayList<>();
      for (Tuple tuple : tuples) {
        Builder eventDataBuilder = new EventData.Builder(tuple.get(qEventData.eventDataName));
        String eventDataTypeString = tuple.get(qEventData.eventDataType);
        EventDataType eventDataType = EventDataType.valueOf(eventDataTypeString);
        switch (eventDataType) {
          case STRING:
            String stringValue = tuple.get(qEventData.stringValue);
            rval.add(eventDataBuilder.buildStringValue(stringValue));
            break;
          case TEXT:
            String textValue = tuple.get(qEventData.textValue);
            rval.add(eventDataBuilder.buildTextValue(false, textValue));
            break;
          case NUMBER:
            double numberValue = tuple.get(qEventData.numberValue);
            rval.add(eventDataBuilder.buildNumberValue(numberValue));
            break;
          case TIMESTAMP:
            Instant timestampValue = tuple.get(qEventData.timestampValue).toInstant();
            rval.add(eventDataBuilder.buildTimestampValue(timestampValue));
            break;
          default:
            throw new IllegalStateException("unsupported eventDataType [" + eventDataType + "]");
        }
      }

      return rval;
    });

    Assert.assertEquals(EXPECTED_EVENT_DATA_LIST_SIZE, eventDataList.size());
    Assert.assertEquals(new EventData.Builder(STRING_N).buildStringValue(STRING_V),
        eventDataList.get(STRING_INDEX));
    Assert.assertEquals(new EventData.Builder(TEXT_N).buildTextValue(false, TEXT_V),
        eventDataList.get(TEXT_INDEX));
    Assert.assertEquals(new EventData.Builder(NUMBER_N).buildNumberValue(NUMBER_V),
        eventDataList.get(NUMBER_INDEX));
    Assert.assertEquals(new EventData.Builder(TIMESTAMP_N).buildTimestampValue(TIMESTAMP_V),
        eventDataList.get(TIMESTAMP_INDEX));
  }

  @Before
  public void before() {
    clearAuditCaches();
  }

  private void clearAllAuditEventTypes() {
    querydslSupport.execute((connection, configuration) -> {

      new SQLDeleteClause(connection, configuration, QEventData.eventData).execute();
      new SQLDeleteClause(connection, configuration, QEvent.event).execute();
      new SQLDeleteClause(connection, configuration, QEventType.eventType).execute();

      return null;
    });
  }

  private void clearAuditApplication(final String applicationName) {
    querydslSupport.execute((connection, configuration) -> {

      QEventData qEventData = QEventData.eventData;
      QEvent qEvent = QEvent.event;
      QEventType qEventType = QEventType.eventType;
      QApplication qApplication = QApplication.application;

      new SQLDeleteClause(connection, configuration, qEventData)
          .where(new SQLSubQuery()
              .from(qApplication)
              .innerJoin(qEventType).on(qEventType.applicationId.eq(qApplication.applicationId))
              .innerJoin(qEvent).on(qEventType.eventTypeId.eq(qEvent.eventTypeId))
              .where(qApplication.applicationName.eq(applicationName)
                  .and(qEvent.eventId.eq(qEventData.eventId)))
              .exists())
          .execute();

      new SQLDeleteClause(connection, configuration, qEvent)
          .where(new SQLSubQuery()
              .from(qApplication)
              .innerJoin(qEventType).on(qEventType.applicationId.eq(qApplication.applicationId))
              .where(qApplication.applicationName.eq(applicationName)
                  .and(qEventType.eventTypeId.eq(qEvent.eventTypeId)))
              .exists())
          .execute();

      new SQLDeleteClause(connection, configuration, qEventType)
          .where(new SQLSubQuery()
              .from(qApplication)
              .where(qApplication.applicationId.eq(qEventType.applicationId)
                  .and(qApplication.applicationName.eq(applicationName)))
              .exists())
          .execute();

      new SQLDeleteClause(connection, configuration, qApplication)
          .where(qApplication.applicationName.eq(applicationName))
          .execute();

      return null;
    });
  }

  private void clearAuditCaches() {
    auditApplicationCache.clear();
    auditEventTypeCache.clear();
  }

  private void clearAuditEventData(final String eventTypeName) {
    querydslSupport.execute((connection, configuration) -> {

      QEventData qEventData = QEventData.eventData;
      QEvent qEvent = QEvent.event;
      QEventType qEventType = QEventType.eventType;

      new SQLDeleteClause(connection, configuration, qEventData)
          .where(new SQLSubQuery()
              .from(qEventType)
              .innerJoin(qEvent).on(qEventType.eventTypeId.eq(qEvent.eventTypeId))
              .where(qEventType.eventTypeName.eq(eventTypeName)
                  .and(qEvent.eventId.eq(qEventData.eventId)))
              .exists())
          .execute();

      return null;
    });
  }

  private void clearPermissions() {
    querydslSupport.execute((connection, configuration) -> {

      new SQLDeleteClause(connection, configuration, QPermission.permission).execute();
      new SQLDeleteClause(connection, configuration, QPermissionInheritance.permissionInheritance)
          .execute();

      return null;
    });
  }

  private AuditEvent createTestEvent(final String eventTypeName) {
    return new AuditEvent.Builder().eventTypeName(eventTypeName)
        .addStringEventData(STRING_N, STRING_V)
        .addTextEventData(TEXT_N, false, TEXT_V)
        .addNumberEventData(NUMBER_N, NUMBER_V)
        .addTimestampEventData(TIMESTAMP_N, TIMESTAMP_V)
        .build();
  }

  /**
   * Clears the database and the caches.
   */
  @Deactivate
  public void deactivate() {
    querydslSupport.execute((connection, configuration) -> {

      new SQLDeleteClause(connection, configuration, QProperty.property).execute();

      new SQLDeleteClause(connection, configuration, QPermission.permission).execute();
      new SQLDeleteClause(connection, configuration, QPermissionInheritance.permissionInheritance)
          .execute();

      new SQLDeleteClause(connection, configuration, QEventData.eventData).execute();
      new SQLDeleteClause(connection, configuration, QEvent.event).execute();
      new SQLDeleteClause(connection, configuration, QEventType.eventType).execute();
      new SQLDeleteClause(connection, configuration, QApplication.application).execute();

      new SQLDeleteClause(connection, configuration, QResource.resource).execute();

      return null;
    });
    clearAuditCaches();
  }

  @ServiceRef(defaultValue = "(service.description=audit-application-cache)")
  public void setAuditApplicationCache(final Map<String, AuditApplication> auditApplicationCache) {
    this.auditApplicationCache = auditApplicationCache;
  }

  @ServiceRef(defaultValue = "")
  public void setAuditApplicationManager(final AuditApplicationManager auditApplicationManager) {
    this.auditApplicationManager = auditApplicationManager;
  }

  @ServiceRef(defaultValue = "(service.description=audit-event-type-cache)")
  public void setAuditEventTypeCache(final Map<?, ?> auditEventTypeCache) {
    this.auditEventTypeCache = auditEventTypeCache;
  }

  /**
   * Sets the {@link #auditEventTypeManager} and the {@link #embeddedAuditApplicationName}.
   */
  @ServiceRef(defaultValue = "")
  public void setAuditEventTypeManager(
      final ServiceHolder<AuditEventTypeManager> serviceHolder) {
    auditEventTypeManager = serviceHolder.getService();
    embeddedAuditApplicationName = (String) serviceHolder.getReference().getProperty(
        AuditRiComponentConstants.ATTR_EMBEDDED_AUDIT_APPLICATION_NAME);
  }

  @ServiceRef(defaultValue = "")
  public void setAuditRiAuthorizationManager(
      final AuditRiAuthorizationManager auditRiAuthorizationManager) {
    this.auditRiAuthorizationManager = auditRiAuthorizationManager;
  }

  @ServiceRef(defaultValue = "")
  public void setAuditRiPermissionChecker(final AuditRiPermissionChecker auditRiPermissionChecker) {
    this.auditRiPermissionChecker = auditRiPermissionChecker;
  }

  @ServiceRef(defaultValue = "")
  public void setAuthenticationPropagator(final AuthenticationPropagator authenticationPropagator) {
    this.authenticationPropagator = authenticationPropagator;
  }

  @ServiceRef(defaultValue = "")
  public void setInternalAuditEventTypeManager(
      final InternalAuditEventTypeManager internalAuditEventTypeManager) {
    this.internalAuditEventTypeManager = internalAuditEventTypeManager;
  }

  @ServiceRef(defaultValue = "")
  public void setInternalLoggingService(final InternalLoggingService internalLoggingService) {
    this.internalLoggingService = internalLoggingService;
  }

  @ServiceRef(defaultValue = "")
  public void setLoggingService(final LoggingService loggingService) {
    this.loggingService = loggingService;
  }

  @ServiceRef(defaultValue = "")
  public void setLogService(final LogService logService) {
    this.logService = logService;
  }

  @ServiceRef(defaultValue = "")
  public void setPermissionChecker(final PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;
  }

  @ServiceRef(defaultValue = "")
  public void setPropertyManager(final PropertyManager propertyManager) {
    this.propertyManager = propertyManager;
  }

  @ServiceRef(defaultValue = "")
  public void setQuerydslSupport(final QuerydslSupport querydslSupport) {
    this.querydslSupport = querydslSupport;
  }

  @ServiceRef(defaultValue = "")
  public void setResourceService(final ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @Test
  public void testGetAuditApplicationTypeTargetResourceId() {

    long expectedAuditApplicationTypeTargetResourceId = Long.parseLong(
        propertyManager
            .getProperty(AuditRiPropertyConstants.AUDIT_APPLICATION_TYPE_TARGET_RESOURCE_ID));

    long actualAuditApplicationTypeTargetResourceId =
        auditRiPermissionChecker.getAuditApplicationTypeTargetResourceId();

    Assert.assertEquals(expectedAuditApplicationTypeTargetResourceId,
        actualAuditApplicationTypeTargetResourceId);
  }

  @Test
  public void testInitAuditApplication() {

    String applicationName = UUID.randomUUID().toString();

    // add permission
    long authorizedResourceId = resourceService.createResource();
    auditRiAuthorizationManager.addPermissionToInitAuditApplication(authorizedResourceId);

    // test with permission
    authenticationPropagator.runAs(authorizedResourceId, () -> {

      Assert.assertTrue(auditRiPermissionChecker.hasPermissionToInitAuditApplication());

      // insert and cache
      auditApplicationManager.initAuditApplication(applicationName);
      assertAuditApplicationExists(applicationName);

      // load from cache
      auditApplicationManager.initAuditApplication(applicationName);
      assertAuditApplicationExists(applicationName);

      return null;
    });

    // remove permission
    auditRiAuthorizationManager.removePermissionInitAuditApplication(authorizedResourceId);

    // test without permission
    authenticationPropagator.runAs(authorizedResourceId, () -> {

      Assert.assertFalse(auditRiPermissionChecker.hasPermissionToInitAuditApplication());

      try {
        auditApplicationManager.initAuditApplication(applicationName);
        Assert.fail();
      } catch (UnauthorizedException e) {
        Assert.assertEquals(1, e.actions.length);
        Assert.assertEquals(AuditRiPermissionConstants.INIT_AUDIT_APPLICATION, e.actions[0]);
        Assert.assertEquals(1, e.authorizationScope.length);
        Assert.assertEquals(authorizedResourceId, e.authorizationScope[0]);
      }

      return null;
    });

  }

  @Test
  public void testInitAuditApplicationFail() {
    try {
      auditApplicationManager.initAuditApplication(null);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertTrue(e.getMessage().equals("applicationName cannot be null"));
    }
  }

  @Test
  public void testInitAuditEventTypes() {

    auditEventTypeManager.initAuditEventTypes();
    assertAuditEventTypesExist(embeddedAuditApplicationName);

    auditEventTypeManager.initAuditEventTypes("et0", "et1");
    assertAuditEventTypesExist(embeddedAuditApplicationName, "et0", "et1");

    auditEventTypeManager.initAuditEventTypes("et0", "et1", "et2", "et3");
    assertAuditEventTypesExist(embeddedAuditApplicationName, "et0", "et1", "et2", "et3");

    clearAuditCaches();

    auditEventTypeManager.initAuditEventTypes("et0", "et1", "et2", "et3");
    assertAuditEventTypesExist(embeddedAuditApplicationName, "et0", "et1", "et2", "et3");
  }

  @Test
  public void testInitAuditEventTypesFail() {

    try {
      auditEventTypeManager.initAuditEventTypes((String[]) null);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertEquals("eventTypeNames cannot be null", e.getMessage());
    }

    try {
      String[] eventTypeNames = new String[] { null };
      auditEventTypeManager.initAuditEventTypes(eventTypeNames);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertEquals("eventTypeNames cannot contain null value", e.getMessage());
    }
  }

  @Test
  public void testInitAuditEventTypesStress() {

    int count = TEN_THOUSAND;
    String[] eventTypeNames = new String[count];
    for (int i = 0; i < count; i++) {
      eventTypeNames[i] = "e" + i;
    }

    logService.log(LogService.LOG_INFO, ">>> init " + count
        + " event types in one transaction started");

    long startAt = Instant.now().getEpochSecond();

    auditEventTypeManager.initAuditEventTypes(eventTypeNames);

    long duration = Instant.now().getEpochSecond() - startAt;
    logService.log(LogService.LOG_INFO, ">>> " + count + " event types initialized in " + duration
        + " seconds");
  }

  @Test
  public void testInternalInitAuditEventTypes() {
    String nonExistentApplicationName = "non-existent-application-name";
    String existentApplicationName = "existent-application-name";

    clearAuditApplication(nonExistentApplicationName);
    clearAuditApplication(existentApplicationName);

    // initialize application
    authenticationPropagator.runAs(permissionChecker.getSystemResourceId(), () -> {

      auditApplicationManager.initAuditApplication(existentApplicationName);
      return null;
    });

    long authorizedResourceId = resourceService.createResource();

    // add permission
    try {
      auditRiAuthorizationManager.addPermissionToLogToAuditApplication(
          authorizedResourceId, nonExistentApplicationName);
      Assert.fail();
    } catch (UnknownAuditApplicationException e) {
      Assert.assertEquals(nonExistentApplicationName, e.applicationName);
    }

    authenticationPropagator.runAs(authorizedResourceId, () -> {

      try {
        auditRiPermissionChecker.hasPermissionToLogToAuditApplication(nonExistentApplicationName);
        Assert.fail();
      } catch (UnknownAuditApplicationException e) {
        Assert.assertEquals(nonExistentApplicationName, e.applicationName);
      }
      return null;
    });

    auditRiAuthorizationManager.addPermissionToLogToAuditApplication(
        authorizedResourceId, existentApplicationName);

    // test with permission
    authenticationPropagator.runAs(authorizedResourceId, () -> {

      Assert.assertTrue(auditRiPermissionChecker
          .hasPermissionToLogToAuditApplication(existentApplicationName));

      try {
        internalAuditEventTypeManager.initAuditEventTypes(nonExistentApplicationName, "et1");
        Assert.fail();
      } catch (UnknownAuditApplicationException e) {
        Assert.assertEquals(nonExistentApplicationName, e.applicationName);
      }

      // insert and cache
      internalAuditEventTypeManager.initAuditEventTypes(existentApplicationName, "et1");
      assertAuditEventTypesExist(existentApplicationName, "et1");

      // load from cache
      internalAuditEventTypeManager.initAuditEventTypes(existentApplicationName, "et1");
      assertAuditEventTypesExist(existentApplicationName, "et1");

      return null;
    });

    // remove permission
    auditRiAuthorizationManager.removePermissionLogToAuditApplication(
        authorizedResourceId, existentApplicationName);

    // test without permission
    authenticationPropagator.runAs(authorizedResourceId,
        () -> {

          Assert.assertFalse(auditRiPermissionChecker
              .hasPermissionToLogToAuditApplication(existentApplicationName));

          try {
            internalAuditEventTypeManager.initAuditEventTypes(existentApplicationName, "et1");
            Assert.fail();
          } catch (UnauthorizedException e) {
            Assert.assertEquals(1, e.actions.length);
            Assert.assertEquals(AuditRiPermissionConstants.LOG_TO_AUDIT_APPLICATION,
                e.actions[0]);
            Assert.assertEquals(1, e.authorizationScope.length);
            Assert.assertEquals(authorizedResourceId, e.authorizationScope[0]);
          }

          return null;
        });

    clearAuditApplication(nonExistentApplicationName);
    clearAuditApplication(existentApplicationName);
  }

  @Test
  public void testInternalInitAuditEventTypesFail() {

    try {
      internalAuditEventTypeManager.initAuditEventTypes(null);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertEquals("applicationName cannot be null", e.getMessage());
    }

    String applicationName = "non-null-application-name";
    try {
      internalAuditEventTypeManager.initAuditEventTypes(applicationName, (String[]) null);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertEquals("eventTypeNames cannot be null", e.getMessage());
    }

    try {
      String[] eventTypeNames = new String[] { null };
      internalAuditEventTypeManager.initAuditEventTypes(applicationName, eventTypeNames);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertEquals("eventTypeNames cannot contain null value", e.getMessage());
    }

  }

  @Test
  public void testInternalLogEvent() {

    String nonExistentApplicationName = "non-existent-application-name";
    String existentApplicationName = "existent-application-name";

    clearAuditApplication(nonExistentApplicationName);
    clearAuditApplication(existentApplicationName);

    // initialize application
    authenticationPropagator.runAs(permissionChecker.getSystemResourceId(), () -> {

      auditApplicationManager.initAuditApplication(existentApplicationName);
      return null;
    });

    long authorizedResourceId = resourceService.createResource();

    // add permission
    auditRiAuthorizationManager.addPermissionToLogToAuditApplication(
        authorizedResourceId, existentApplicationName);

    // test with permission
    authenticationPropagator.runAs(authorizedResourceId, () -> {

      try {
        internalLoggingService.logEvent(nonExistentApplicationName, createTestEvent("et0"));
        Assert.fail();
      } catch (UnknownAuditApplicationException e) {
        Assert.assertEquals(nonExistentApplicationName, e.applicationName);
      }

      internalLoggingService.logEvent(existentApplicationName, createTestEvent("et0"));
      assertEvent("et0");

      return null;
    });

    // remove permission
    auditRiAuthorizationManager.removePermissionLogToAuditApplication(
        authorizedResourceId, existentApplicationName);

    // test without permission
    authenticationPropagator.runAs(authorizedResourceId,
        () -> {

          try {
            internalLoggingService.logEvent(existentApplicationName, createTestEvent("et0"));
            Assert.fail();
          } catch (UnauthorizedException e) {
            Assert.assertEquals(1, e.actions.length);
            Assert.assertEquals(AuditRiPermissionConstants.LOG_TO_AUDIT_APPLICATION,
                e.actions[0]);
            Assert.assertEquals(1, e.authorizationScope.length);
            Assert.assertEquals(authorizedResourceId, e.authorizationScope[0]);
          }

          return null;
        });

    clearAuditApplication(nonExistentApplicationName);
    clearAuditApplication(existentApplicationName);
  }

  @Test
  public void testInternalLogEventFail() {

    try {
      internalLoggingService.logEvent(null, null);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertEquals("applicationName cannot be null", e.getMessage());
    }

    try {
      internalLoggingService.logEvent("non-null-application-name", null);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertEquals("auditEvent cannot be null", e.getMessage());
    }
  }

  @Test
  public void testLogEvent() {

    String eventTypeName = "et0";

    // lazy create event type
    loggingService.logEvent(createTestEvent(eventTypeName));
    assertEvent(eventTypeName);

    clearAuditEventData(eventTypeName);

    // load event type from cache
    loggingService.logEvent(createTestEvent(eventTypeName));
    assertEvent(eventTypeName);
  }

  @Test
  public void testLogEventFail() {
    try {
      loggingService.logEvent(null);
      Assert.fail();
    } catch (NullPointerException e) {
      Assert.assertTrue(e.getMessage().equals("auditEvent cannot be null"));
    }
  }

  @Test
  public void testLogEventStress() {

    int eventTypeCount = HUNDRED;
    int eventsPerEventType = HUNDRED;

    String[] eventTypeNames = new String[eventTypeCount];
    List<AuditEvent> auditEvents = new ArrayList<>();

    for (int i = 0; i < eventTypeCount; i++) {
      String eventTypeName = "e" + i;
      eventTypeNames[i] = eventTypeName;
      for (int j = 0; j < eventsPerEventType; j++) {
        auditEvents.add(createTestEvent(eventTypeName));
      }
    }
    Collections.shuffle(auditEvents);

    auditEventTypeManager.initAuditEventTypes(eventTypeNames);

    int count = eventTypeCount * eventsPerEventType;

    logService.log(LogService.LOG_INFO, ">>> log " + count
        + " events (containing 4 event data) started");

    long startAt = Instant.now().getEpochSecond();

    for (AuditEvent auditEvent : auditEvents) {
      loggingService.logEvent(auditEvent);
    }

    long duration = Instant.now().getEpochSecond() - startAt;
    logService.log(LogService.LOG_INFO, ">>> " + count + " events logged in " + duration
        + " seconds");
  }
}
