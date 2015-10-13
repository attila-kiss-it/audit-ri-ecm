package org.everit.audit.ri.ecm.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.everit.audit.dto.AuditEventType;
import org.everit.audit.ri.AuditApplicationManager;
import org.everit.audit.ri.AuditRequiredServices;
import org.everit.audit.ri.CachedEventTypeKey;
import org.everit.audit.ri.InternalAuditEventTypeManager;
import org.everit.audit.ri.InternalAuditService;
import org.everit.audit.ri.InternalLoggingService;
import org.everit.audit.ri.authorization.AuditRiAuthorizationManager;
import org.everit.audit.ri.authorization.AuditRiPermissionChecker;
import org.everit.audit.ri.dto.AuditApplication;
import org.everit.audit.ri.ecm.AuditRiComponentConstants;
import org.everit.authnr.permissionchecker.AuthnrPermissionChecker;
import org.everit.authorization.AuthorizationManager;
import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Deactivate;
import org.everit.osgi.ecm.annotation.ManualService;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.component.ComponentContext;
import org.everit.persistence.querydsl.support.QuerydslSupport;
import org.everit.props.PropertyManager;
import org.everit.resource.ResourceService;
import org.everit.transaction.propagator.TransactionPropagator;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * The internal implementation of the audit component.
 */
@Component(componentId = AuditRiComponentConstants.INTERNAL_SERVICE_FACTORY_PID,
    configurationPolicy = ConfigurationPolicy.FACTORY,
    label = "Everit Audit (Internal) RI",
    description = "Component for audit logging and management for internal usage.")
@StringAttributes({
    @StringAttribute(attributeId = Constants.SERVICE_DESCRIPTION,
        defaultValue = AuditRiComponentConstants.INTERNAL_DEFAULT_SERVICE_DESCRIPTION,
        priority = InternalAuditComponentAttrPriority.P01_SERVICE_DESCRIPTION,
        label = "Service Description",
        description = "The description of this component configuration. It is used to easily "
            + "identify the service registered by this component.") })
@ManualService({
    AuditApplicationManager.class,
    InternalAuditEventTypeManager.class,
    InternalLoggingService.class,
    AuditRiAuthorizationManager.class,
    AuditRiPermissionChecker.class })
public class InternalAuditComponent {

  private Map<String, AuditApplication> auditApplicationCache;

  private Map<CachedEventTypeKey, AuditEventType> auditEventTypeCache;

  private AuthnrPermissionChecker authnrPermissionChecker;

  private AuthorizationManager authorizationManager;

  private PropertyManager propertyManager;

  private QuerydslSupport querydslSupport;

  private ResourceService resourceService;

  private ServiceRegistration<?> serviceRegistration;

  private TransactionPropagator transactionPropagator;

  /**
   * Registers the OSGi services.
   */
  @Activate
  public void activate(final ComponentContext<InternalAuditComponent> componentContext) {
    AuditRequiredServices auditRequiredServices = new AuditRequiredServices(
        authnrPermissionChecker, authorizationManager, propertyManager, resourceService,
        querydslSupport, transactionPropagator);
    InternalAuditService internalAuditService = new InternalAuditService(
        auditApplicationCache, auditEventTypeCache, auditRequiredServices);

    Dictionary<String, Object> serviceProperties =
        new Hashtable<>(componentContext.getProperties());

    serviceRegistration =
        componentContext.registerService(
            new String[] {
                AuditApplicationManager.class.getName(),
                InternalAuditEventTypeManager.class.getName(),
                InternalLoggingService.class.getName(),
                AuditRiAuthorizationManager.class.getName(),
                AuditRiPermissionChecker.class.getName() },
            internalAuditService,
            serviceProperties);
  }

  /**
   * Unregisters the registered OSGi services.
   */
  @Deactivate
  public void deactivate() {
    if (serviceRegistration != null) {
      serviceRegistration.unregister();
    }
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_AUDIT_APPLICATION_CACHE,
      defaultValue = AuditRiComponentConstants.DEFAULT_CACHE_TARGET,
      attributePriority = InternalAuditComponentAttrPriority.P02_AUDIT_APPLICATION_CACHE,
      label = "Application cache",
      description = "OSGi service filter to identify the cache (Map or ConcurrentMap) service.")
  public void setAuditApplicationCache(final Map<String, AuditApplication> auditApplicationCache) {
    this.auditApplicationCache = auditApplicationCache;
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_AUDIT_EVENT_TYPE_CACHE,
      defaultValue = AuditRiComponentConstants.DEFAULT_CACHE_TARGET,
      attributePriority = InternalAuditComponentAttrPriority.P03_AUDIT_EVENT_TYPE_CACHE,
      label = "Event Type cache",
      description = "OSGi service filter to identify the cache (Map or ConcurrentMap) service.")
  public void setAuditEventTypeCache(
      final Map<CachedEventTypeKey, AuditEventType> auditEventTypeCache) {
    this.auditEventTypeCache = auditEventTypeCache;
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_AUTHNR_PERMISSION_CHECKER,
      defaultValue = "",
      attributePriority = InternalAuditComponentAttrPriority.P04_AUTHNR_PERMISSION_CHECKER,
      label = "Authnr Permission Checker",
      description = "OSGi service filter to identify AuthnrPermissionChecker service.")
  public void setAuthnrPermissionChecker(final AuthnrPermissionChecker authnrPermissionChecker) {
    this.authnrPermissionChecker = authnrPermissionChecker;
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_AUTHORIZATION_MANAGER,
      defaultValue = "",
      attributePriority = InternalAuditComponentAttrPriority.P05_AUTHORIZATION_MANAGER,
      label = "Authorization Manager",
      description = "OSGi service filter to identify AuthorizationManger service.")
  public void setAuthorizationManager(final AuthorizationManager authorizationManager) {
    this.authorizationManager = authorizationManager;
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_PROPERTY_MANAGER,
      defaultValue = "",
      attributePriority = InternalAuditComponentAttrPriority.P06_PROPERTY_MANAGER,
      label = "Property Manager",
      description = "OSGi service filter to identify PropertyManager service.")
  public void setPropertyManager(final PropertyManager propertyManager) {
    this.propertyManager = propertyManager;
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_QUERYDSL_SUPPORT,
      defaultValue = "",
      attributePriority = InternalAuditComponentAttrPriority.P07_QUERYDSL_SUPPORT,
      label = "Querydsl Support",
      description = "OSGi service filter to identify the QuerydslSupport service.")
  public void setQuerydslSupport(final QuerydslSupport querydslSupport) {
    this.querydslSupport = querydslSupport;
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_RESOURCE_SERVICE,
      defaultValue = "",
      attributePriority = InternalAuditComponentAttrPriority.P08_RESOURCE_SERVICE,
      label = "Resource Service",
      description = "OSGi service filter to identify ResourceService service.")
  public void setResourceService(final ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @ServiceRef(
      attributeId = AuditRiComponentConstants.ATTR_TRASACTION_PROPAGATOR,
      defaultValue = "",
      attributePriority = InternalAuditComponentAttrPriority.P09_TRASACTION_PROPAGATOR,
      label = "Transaction Helper",
      description = "OSGi service filter to identify the TransacitonHelper service.")
  public void setTransactionPropagator(final TransactionPropagator transactionPropagator) {
    this.transactionPropagator = transactionPropagator;
  }

}
