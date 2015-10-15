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
package org.everit.audit.ri.ecm.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.everit.audit.AuditEventTypeManager;
import org.everit.audit.LoggingService;
import org.everit.audit.ri.AuditApplicationManager;
import org.everit.audit.ri.EmbeddedAuditService;
import org.everit.audit.ri.InternalAuditEventTypeManager;
import org.everit.audit.ri.InternalLoggingService;
import org.everit.audit.ri.ecm.AuditRiComponentConstants;
import org.everit.authentication.context.AuthenticationPropagator;
import org.everit.authorization.PermissionChecker;
import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.ManualService;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * The embedded implementation of the {@link AuditEventTypeManager} and the {@link LoggingService}.
 */
@Component(componentId = AuditRiComponentConstants.EMBEDDED_SERVICE_FACTORY_PID,
    configurationPolicy = ConfigurationPolicy.FACTORY,
    label = "Everit Audit (Embedded) RI",
    description = "Component for audit logging and management for embedded components.")
@ProvideCapability(ns = ECMExtenderConstants.CAPABILITY_NS_COMPONENT,
    value = ECMExtenderConstants.CAPABILITY_ATTR_CLASS + "=${@class}")
@StringAttributes({
    @StringAttribute(
        attributeId = Constants.SERVICE_DESCRIPTION,
        defaultValue = AuditRiComponentConstants.EMBEDDED_DEFAULT_SERVICE_DESCRIPTION,
        priority = EmbeddedAuditCompnentAttrPriority.P01_SERVICE_DESCRIPTION,
        label = "Service Description",
        description = "The description of this component configuration. It is used to easily "
            + "identify the service registered by this component.") })
@ManualService({ AuditEventTypeManager.class, LoggingService.class })
public class EmbeddedAuditCompnent {

  private AuditApplicationManager auditApplicationManager;

  private AuthenticationPropagator authenticationPropagator;

  private String embeddedAuditApplicationName;

  private InternalAuditEventTypeManager internalAuditEventTypeManager;

  private InternalLoggingService internalLoggingService;

  private PermissionChecker permissionChecker;

  private ServiceRegistration<?> serviceRegistration;

  /**
   * Registers the OSGi services.
   */
  @Activate
  public void activate(final ComponentContext<EmbeddedAuditCompnent> componentContext) {

    Dictionary<String, Object> serviceProperties =
        new Hashtable<>(componentContext.getProperties());

    EmbeddedAuditService embeddedAuditService = new EmbeddedAuditService(auditApplicationManager,
        internalAuditEventTypeManager, internalLoggingService, authenticationPropagator,
        permissionChecker, embeddedAuditApplicationName);
    serviceRegistration =
        componentContext.registerService(
            new String[] { AuditEventTypeManager.class.getName(), LoggingService.class.getName() },
            embeddedAuditService,
            serviceProperties);
  }

  /**
   * Unregisters the registered OSGi service.
   */
  @Deactivate
  public void deactivate() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  @ServiceRef(attributeId = AuditRiComponentConstants.ATTR_AUDIT_APPLICATION_MANAGER,
      defaultValue = "",
      attributePriority = EmbeddedAuditCompnentAttrPriority.P03_AUDIT_APPLICATION_MANAGER,
      label = "Audit Application Manager",
      description = "OSGi service filter to identify the AuditApplicationManager service.")
  public void setAuditApplicationManager(final AuditApplicationManager auditApplicationManager) {
    this.auditApplicationManager = auditApplicationManager;
  }

  @ServiceRef(attributeId = AuditRiComponentConstants.ATTR_AUTHENTICATION_PROPAGATOR,
      defaultValue = "",
      attributePriority = EmbeddedAuditCompnentAttrPriority.P06_AUTHENTICATION_PROPAGATOR,
      label = "Authentication Propagator",
      description = "OSGi service filter to identify the AuthenticationPropagator service.")
  public void setAuthenticationPropagator(final AuthenticationPropagator authenticationPropagator) {
    this.authenticationPropagator = authenticationPropagator;
  }

  @StringAttribute(attributeId = AuditRiComponentConstants.ATTR_EMBEDDED_AUDIT_APPLICATION_NAME,
      defaultValue = "",
      priority = EmbeddedAuditCompnentAttrPriority.P02_EMBEDDED_AUDIT_APPLICATION_NAME,
      label = "Embedded Audit Application Name",
      description = "The name of the Audit Application that will be used by embedded components.")
  public void setEmbeddedAuditApplicationName(final String embeddedAuditApplicationName) {
    this.embeddedAuditApplicationName = embeddedAuditApplicationName;
  }

  @ServiceRef(attributeId = AuditRiComponentConstants.ATTR_INTERNAL_AUDIT_EVENT_TYPE_MANAGER,
      defaultValue = "",
      attributePriority = EmbeddedAuditCompnentAttrPriority.P04_INTERNAL_AUDIT_EVENT_TYPE_MANAGER,
      label = "Internal Audit Event Type Manager",
      description = "OSGi service filter to identify the InternalAuditEventTypeManager service.")
  public void setInternalAuditEventTypeManager(
      final InternalAuditEventTypeManager internalAuditEventTypeManager) {
    this.internalAuditEventTypeManager = internalAuditEventTypeManager;
  }

  @ServiceRef(attributeId = AuditRiComponentConstants.ATTR_INTERNAL_LOGGING_SERVICE,
      defaultValue = "",
      attributePriority = EmbeddedAuditCompnentAttrPriority.P05_INTERNAL_LOGGING_SERVICE,
      label = "Internal Logging Service",
      description = "OSGi service filter to identify the InternalLoggingService service.")
  public void setInternalLoggingService(final InternalLoggingService internalLoggingService) {
    this.internalLoggingService = internalLoggingService;
  }

  @ServiceRef(attributeId = AuditRiComponentConstants.ATTR_PERMISSION_CHECKER,
      defaultValue = "",
      attributePriority = EmbeddedAuditCompnentAttrPriority.P07_PERMISSION_CHECKER,
      label = "Permission Checker",
      description = "OSGi service filter to identify the PermissionChecker service.")
  public void setPermissionChecker(final PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;
  }

}
