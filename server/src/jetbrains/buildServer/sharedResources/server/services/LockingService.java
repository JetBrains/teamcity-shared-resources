package jetbrains.buildServer.sharedResources.server.services;

import jetbrains.buildServer.sharedResources.model.LockType;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 04.10.12
 * Time: 20:00
 *
 * @author Oleg Rybak
 */
public interface LockingService {

  /**
   * Locks given resource by given build
   * @param buildId build that lock resource
   * @param locks resources to lock (map that has format {@code ResourceName => LockType})
   */
  public void lock(String buildId, Map<String, LockType> locks);

  /**
   * Removes lock, held by given build
   * @param buildId id of the build
   * @param resource resource to remove lock from
   */
  public void unlock(String buildId, String resource);

  /**
   * Returns all resources that are locked by given build
   * @param buildId ide of the build
   * @return map that has format {@code resourceName => LockType}
   */
  public Map<String, LockType> getLockedResourcesForBuild(String buildId);

  /**
   * Returns ids of builds that have locked shared resource
   *
   * @param sharedResource name of the shared resource
   * @return map that has format {@code buildId => LockType}
   */
  public Map<String, LockType> getLockingBuildsForSharedResource(String sharedResource);
}
