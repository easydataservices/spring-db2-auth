/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.session.db2auth;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.db2auth.Change;

/**
 * Spring SessionRepository implementation.
 *
 * @author jeremy.rickard@easydataservices.com
 */
public final class Db2AuthSession implements Session {
  protected MapSession delegate;
  protected EnumSet<Change> changes;
  protected int attributeGenerationId;

  Db2AuthSession() {
    delegate = new MapSession();
    attributeGenerationId = 0;
    changes.add(Change.NEW_SESSION);
  }

  @Override
  public String changeSessionId() {
    delegate.changeSessionId();
    changes.add(Change.SESSION_ID);
    return delegate.getId();
  }

  @Override
  public <T> T getAttribute(String attributeName) {
    return null; // TBC
  }

  @Override
  public Set<String> getAttributeNames() {
    return null; // TBC
  }
  
  @Override
  public Instant getCreationTime() {
    if (changes.contains(Change.NEW_SESSION)) {
      return null;
    }
    return delegate.getCreationTime();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public Instant getLastAccessedTime() {
    return delegate.getLastAccessedTime();
  }

  @Override
  public Duration getMaxInactiveInterval() {
    return delegate.getMaxInactiveInterval();
  }

  @Override
  public boolean isExpired() {
    return false; // TBC
  }
  @Override

  public void removeAttribute(String attributeName) {
    delegate.removeAttribute(attributeName);
    changes.add(Change.ATTRIBUTES);
  }

  @Override
  public void setAttribute(String attributeName, Object attributeValue) {
    delegate.setAttribute(attributeName, attributeValue);
    changes.add(Change.ATTRIBUTES);
  }

  @Override
  public void setMaxInactiveInterval(Duration interval) {
    delegate.setMaxInactiveInterval(interval);
    changes.add(Change.SESSION_CHANGE);
  }

  @Override
  public void setLastAccessedTime(Instant lastAccessedTime) {
    delegate.setLastAccessedTime(lastAccessedTime);
    changes.add(Change.ACCESS);
  }
}
