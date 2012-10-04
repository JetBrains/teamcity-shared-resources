package jetbrains.buildServer.sharedResources.server.services;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.sharedResources.model.LockType;

import java.util.Collections;
import java.util.Map;

/**
 * Service for locking and unlocking shared resources
 *
 * @author Oleg Rybak
 */
public class InMemoryLockingService implements LockingService {

  private static final Logger LOG = Logger.getInstance(InMemoryLockingService.class.getName());

  @Override
  public void lock(String buildId, Map<String, LockType> locks) {
    LOG.info("InMemoryLockingService.lock()");
  }

  @Override
  public void unlock(String buildId, String resource) {
    LOG.info("InMemoryLockingService.unlock()");
  }

  @Override
  public Map<String, LockType> getLockedResourcesForBuild(String buildId) {
    LOG.info("InMemoryLockingService.getLockedResourcesForBuild()");
    return Collections.emptyMap();
  }

  @Override
  public Map<String, LockType> getLockingBuildsForSharedResource(String sharedResource) {
    LOG.info("InMemoryLockingService.getLockingBuildsForSharedResource()");
    return Collections.emptyMap();
  }

}
