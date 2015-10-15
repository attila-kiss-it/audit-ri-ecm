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
public final class EmbeddedAuditCompnentAttrPriority {

  public static final int P01_SERVICE_DESCRIPTION = 1;

  public static final int P02_EMBEDDED_AUDIT_APPLICATION_NAME = 2;

  public static final int P03_AUDIT_APPLICATION_MANAGER = 3;

  public static final int P04_INTERNAL_AUDIT_EVENT_TYPE_MANAGER = 4;

  public static final int P05_INTERNAL_LOGGING_SERVICE = 5;

  public static final int P06_AUTHENTICATION_PROPAGATOR = 6;

  public static final int P07_PERMISSION_CHECKER = 7;

  private EmbeddedAuditCompnentAttrPriority() {
  }

}
