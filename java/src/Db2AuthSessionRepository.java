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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import com.easydataservices.open.auth.AuthAttributesDao;
import com.easydataservices.open.auth.AuthControlDao;
import com.easydataservices.open.auth.AuthSessionDao;
import com.easydataservices.open.auth.StoreAttribute;
import com.easydataservices.open.auth.StoreSession;
import com.easydataservices.open.auth.util.Mask;
import org.springframework.session.SessionRepository;
import org.springframework.session.db2auth.Change;
import org.springframework.session.db2auth.Db2AuthSession;

/**
 * Spring SessionRepository implementation.
 *
 * @author jeremy.rickard@easydataservices.com
 */
public final class Db2AuthSessionRepository implements SessionRepository<Db2AuthSession> {
  private static final String className = Db2AuthSessionRepository.class.getName();
  private static final Logger logger = Logger.getLogger(className);
  private ConcurrentHashMap<String, Db2AuthSession> sessionCache;
  private DataSource dataSource;
  private String schemaName;

  /**
   * Constructor.
   * @param connection {@link Connection} to session repository database.
   * @param schemaName Schema name for session repository.
   */
  public Db2AuthSessionRepository(DataSource dataSource, String schemaName) {
    logger.finer(() -> String.format("ENTRY %s %s %s", this, dataSource, schemaName));
    sessionCache = new ConcurrentHashMap<String, Db2AuthSession>();
    this.dataSource = dataSource;
    this.schemaName = schemaName;
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
    try (Connection connection = dataSource.getConnection()) {
      AuthControlDao authControlDao = new AuthControlDao(connection, schemaName);
      authControlDao.removeSession(id);
      sessionCache.remove(id);
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
    Db2AuthSession cacheSession = null;

    logger.finer(() -> String.format("ENTRY %s %s", this, maskedSessionId));
    try (Connection connection = dataSource.getConnection()) {
      AuthSessionDao authSessionDao = new AuthSessionDao(connection, schemaName);
      StoreSession storeSession = authSessionDao.getSession(id);
      if (storeSession == null) {
        // Session is not in store, so ensure it is removed from cache.
        sessionCache.remove(id);
      }
      else {
        // Look up session in cache, creating a new session if not found.
        cacheSession = sessionCache.get(id);
        if (cacheSession == null) {
          cacheSession = new Db2AuthSession();
          cacheSession.delegate.setId(id);
          cacheSession.attributeGenerationId = 0;
          cacheSession.delegate.setCreationTime(storeSession.getCreatedTime());
        }

        // Delta load session attributes.
        if (cacheSession.attributeGenerationId < storeSession.getAttributeGenerationId()) {
          AuthAttributesDao authAttributesDao = new AuthAttributesDao(connection, schemaName);
          List<StoreAttribute> storeAttributeList = authAttributesDao.getAttributes(id, cacheSession.attributeGenerationId);
          Set<String> storeAttributeNameSet = new HashSet<String>();
          for (StoreAttribute storeAttribute : storeAttributeList) {
            storeAttributeNameSet.add(storeAttribute.getAttributeName());
            if (storeAttribute.getObject() != null) {
              cacheSession.setAttribute(storeAttribute.getAttributeName(), storeAttribute.getObject());
            }
          }
          for (String attributeName : cacheSession.getAttributeNames()) {
            if (!storeAttributeNameSet.contains(attributeName)) {
              cacheSession.delegate.removeAttribute(attributeName);
            }
          }
        }

        // Update session properties that may have changed.
        cacheSession.attributeGenerationId = storeSession.getAttributeGenerationId();
        cacheSession.setLastAccessedTime(storeSession.getLastAccessedTime());
        cacheSession.setMaxInactiveInterval(Duration.ofMinutes(storeSession.getMaxIdleMinutes()));

        // Save the session to cache.
        sessionCache.put(id, cacheSession);
      }
    }
    catch (SQLException exception) {
      logger.warning(() -> String.format("RETURN %s", this, exception.getMessage()));
      throw new RuntimeException(exception.getMessage());
    }
    logger.finer(() -> String.format("RETURN %s", this));
    return cacheSession;
  }

  @Override
  public void save(Db2AuthSession session) {
    // TBC - this method needs to make relevant DAO calls for all pending changes.
  }
}
