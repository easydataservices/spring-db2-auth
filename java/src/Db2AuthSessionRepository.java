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

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.logging.Logger;
import com.easydataservices.open.auth.AuthControlDao;
import com.easydataservices.open.auth.AuthSessionDao;
import com.easydataservices.open.auth.util.Mask;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

/**
 * Spring SessionRepository implementation.
 *
 * @author jeremy.rickard@easydataservices.com
 */
public final class Db2AuthSessionRepository implements SessionRepository<Db2AuthSessionRepository.Db2AuthSession> {
  private static final String className = Db2AuthSessionRepository.class.getName();
  private static final Logger logger = Logger.getLogger(className);
  private AuthControlDao authControlDao;
  private AuthSessionDao authSessionDao;
  
  /**
   * Constructor.
   * @param connection {@link Connection} to session repository database.
   * @param schemaName Schema name for session repository.
   */
  public Db2AuthSessionRepository(Connection connection, String schemaName) {
    logger.finer(() -> String.format("ENTRY %s %s %s", this, connection, schemaName));
    authControlDao = new AuthControlDao(connection, schemaName);
    authSessionDao = new AuthSessionDao(connection, schemaName);
    logger.finer(() -> String.format("RETURN %s", this));
  }

  @Override
	public Db2AuthSession createSession() {
		Db2AuthSession session = new Db2AuthSession();
		return session;
  }

	@Override
	public void deleteById(String id) {
    final String maskedSessionId = Mask.last(id, 4);

    logger.finer(() -> String.format("ENTRY %s %s", this, maskedSessionId));
    try {
      authControlDao.removeSession(id);
    }
    catch (SQLException exception) {
      logger.warning(() -> String.format("RETURN %s", this, exception.getMessage()));
      throw new RuntimeException(exception.getMessage());
    }
    logger.finer(() -> String.format("RETURN %s", this));
	}

  @Override
	public Db2AuthSession findById(String id) {
    final String maskedSessionId = Mask.last(id, 4);
		Db2AuthSession session = new Db2AuthSession();

    logger.finer(() -> String.format("ENTRY %s %s", this, maskedSessionId));
    try {
      com.easydataservices.open.auth.Session authSession = authSessionDao.getSession(id);
      //session.setId(authSession.getSessionId());
      session.setLastAccessedTime(authSession.getLastAccessedTime());
      session.setMaxInactiveInterval(Duration.ofMinutes(authSession.getMaxIdleMinutes()));
    }
    catch (SQLException exception) {
      logger.warning(() -> String.format("RETURN %s", this, exception.getMessage()));
      throw new RuntimeException(exception.getMessage());
    }
    logger.finer(() -> String.format("RETURN %s", this));
		return session;
	}

  @Override
	public void save(Db2AuthSession session) {
	}

  class Db2AuthSession implements Session {
    private MapSession cached;

    @Override
    public String changeSessionId() {
      return null;
    }
  
    @Override
    public <T> T getAttribute(String attributeName) {
      return null;
    }
  
    @Override
    public Set<String> getAttributeNames() {
      return null;
    }
    
    @Override
    public Instant getCreationTime() {
      return null;
    }
  
    @Override
    public String getId() {
      return null;
    }
  
    @Override
    public Instant getLastAccessedTime() {
      return null;
    }
  
    @Override
    public Duration getMaxInactiveInterval() {
      return null;
    }
  
    @Override
    public boolean isExpired() {
      return false;
    }
  
    @Override
    public void removeAttribute(String attributeName) {
    }
  
    @Override
    public void setAttribute(String attributeName, Object attributeValue) {
    }
  
    @Override
    public void setMaxInactiveInterval(Duration interval) {
    }
  
    @Override
    public void setLastAccessedTime(Instant lastAccessedTime) {
    }
  
    public void setCreationTime(Instant creationTime) {
    }    
  }
}
