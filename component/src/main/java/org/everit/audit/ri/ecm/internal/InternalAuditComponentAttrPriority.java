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

/**
 * Constants for component attribute priorities.
 */
public final class InternalAuditComponentAttrPriority {

  public static final int P01_SERVICE_DESCRIPTION = 1;

  public static final int P02_AUDIT_APPLICATION_CACHE = 2;

  public static final int P03_AUDIT_EVENT_TYPE_CACHE = 3;

  public static final int P04_AUTHNR_PERMISSION_CHECKER = 4;

  public static final int P05_AUTHORIZATION_MANAGER = 5;

  public static final int P06_PROPERTY_MANAGER = 6;

  public static final int P07_QUERYDSL_SUPPORT = 7;

  public static final int P08_RESOURCE_SERVICE = 8;

  public static final int P09_TRASACTION_PROPAGATOR = 9;

  private InternalAuditComponentAttrPriority() {
  }

}
