package org.springframework.session.db2auth;

/**
 * Enum of session changes to persist. Each value represents a change type that the repository needs to manage in a
 * specific way.
 *
 * @author jeremy.rickard@easydataservices.com
 */
public enum Change {
  NEW_SESSION,         // New session, yet to be persistedd in store
  ACCESS,              // User access to session (updated last accessed time to persist)
  SESSION_ID,          // Change of session identifier
  SESSION_AUTH,        // Session (re)authentication
  SESSION_CHANGE,      // Other session update
  SESSION_REMOVED,     // Session removed
  ATTRIBUTES           // One or more changes to session attributes
}
