package jetbrains.buildServer.sharedResources.server;

import com.google.common.base.Strings;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.serverSide.buildDistribution.RunningBuildInfo;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.LOCK_PREFIX;

/**
 * Class {@code SharedResourcesUtils}
 *
 * @author Oleg Rybak
 */
final class SharedResourcesUtils {

  private static final int PREFIX_OFFSET = LOCK_PREFIX.length();

  private static final Logger LOG = Logger.getInstance(SharedResourcesUtils.class.getName());

  /**
   * todo: javadoc
   * todo: tests
   * todo: refine matching
   *
   *
   * @param serializedParam parameter stored in feature
   * @return
   */
  @NotNull
  public static Map<String, String> featureParamToBuildParams(String serializedParam) {
    final Pattern p = Pattern.compile("([A-za-z0-9]+)\\s+([A-za-z0-9]+)");
    Map<String, String> result;
    if (Strings.isNullOrEmpty(serializedParam)) {
      result = Collections.emptyMap();
    } else {
      result = new HashMap<String, String> ();
      String[] strings = serializedParam.split("\n");
      for(String str: strings) {
        Matcher m = p.matcher(str);
        if (m.matches()) {
          // group2 - lock type
          // group1 - lock name
          result.put(SharedResourcesPluginConstants.LOCK_PREFIX + m.group(2) + "." + m.group(1), "");
        }
      }
    }
    return result;
  }

  /**
   * Extracts lock from build parameter name
   *
   * @param paramName name of the build parameter
   * @return {@code Lock} of appropriate type, if parsing was successful, {@code null} otherwise
   */
  @Nullable
  static Lock getLockFromBuildParam(@NotNull String paramName) {
    Lock result = null;
    if (paramName.startsWith(LOCK_PREFIX)) {
      String lockString = paramName.substring(PREFIX_OFFSET);
      LockType lockType = null;
      for (LockType type : LockType.values()) {
        if (lockString.startsWith(type.getName())) {
          lockType = type;
          break;
        }
      }

      if (lockType == null) {
        LOG.warn("Error parsing lock type of '" + paramName + "'. Supported values are " + Arrays.toString(LockType.values()));
        return null;
      }

      try {
        String lockName = lockString.substring(lockType.getName().length() + 1);
        if (lockName.length() == 0) {
          LOG.warn("Error parsing lock name of '" + paramName + "'. Supported format is 'teamcity.locks.[read|write]Lock.<lock name>'");
          return null;
        }
        result = new Lock(lockName, lockType);
      } catch (IndexOutOfBoundsException e) {
        LOG.warn("Error parsing lock name of '" + paramName + "'. Supported format is 'teamcity.locks.[read|write]Lock.<lock name>'");
        return null;
      }
    }
    return result;
  }

  /**
   * Extracts lock names and types from given build promotion
   *
   * @param buildPromotionInfo build promotion
   * @return collection of locks, that correspond to given promotion
   */
  @NotNull
  static Collection<Lock> extractLocksFromPromotion(@NotNull BuildPromotionInfo buildPromotionInfo) {
    Collection<Lock> result = new HashSet<Lock>();
    Map<String, String> buildParameters = buildPromotionInfo.getParameters();
    for (Map.Entry<String, String> param : buildParameters.entrySet()) {
      Lock lock = getLockFromBuildParam(param.getKey());
      if (lock != null) {
        result.add(lock);
      }
    }
    return result;
  }

  /**
   * Extracts build promotions from build server state, represented by running and queued builds
   *
   * @param runningBuilds running builds
   * @param queuedBuilds  queued builds
   * @return collection of build promotions, that correspond to given builds
   */
  @NotNull
  static Collection<BuildPromotionInfo> getBuildPromotions(@NotNull Collection<RunningBuildInfo> runningBuilds, @NotNull Collection<QueuedBuildInfo> queuedBuilds) {
    ArrayList<BuildPromotionInfo> result = new ArrayList<BuildPromotionInfo>(runningBuilds.size() + queuedBuilds.size());
    for (RunningBuildInfo runningBuildInfo : runningBuilds) {
      result.add(runningBuildInfo.getBuildPromotionInfo());
    }
    for (QueuedBuildInfo queuedBuildInfo : queuedBuilds) {
      result.add(queuedBuildInfo.getBuildPromotionInfo());
    }
    return result;
  }

  /**
   * Finds locks that are unavailable (taken) at the moment
   *
   * @param locksToTake     locks, requested by the build
   * @param buildPromotions other builds (running and queued)
   * @return collection of unavailable locks
   */
  @NotNull
  static Collection<Lock> getUnavailableLocks(@NotNull Collection<Lock> locksToTake, @NotNull Collection<BuildPromotionInfo> buildPromotions) {
    List<Lock> result = new ArrayList<Lock>();
    Map<String, TakenLockInfo> takenLocks = collectTakenLocks(buildPromotions);
    for (Lock lock : locksToTake) {
      TakenLockInfo takenLock = takenLocks.get(lock.getName());
      switch (lock.getType()) {
        case READ:
          if (takenLock != null && !takenLock.writeLocks.isEmpty()) {
            result.add(lock);
          }
          break;
        case WRITE:
          if (takenLock != null && (!takenLock.readLocks.isEmpty() || !takenLock.writeLocks.isEmpty())) {
            result.add(lock);
          }
          break;
      }
    }
    return result;
  }

  /**
   * Collects taken locks from build promotions (representing running and queued builds)
   *
   * @param buildPromotions build promotions (representing running and queued builds)
   * @return collection of locks that are already taken
   */
  @NotNull
  private static Map<String, TakenLockInfo> collectTakenLocks(@NotNull Collection<BuildPromotionInfo> buildPromotions) {
    Map<String, TakenLockInfo> takenLocks = new HashMap<String, TakenLockInfo>();
    for (BuildPromotionInfo info : buildPromotions) {
      Collection<Lock> locks = extractLocksFromPromotion(info);
      for (Lock lock : locks) {
        TakenLockInfo takenLock = takenLocks.get(lock.getName());
        if (takenLock == null) {
          takenLock = new TakenLockInfo();
          takenLocks.put(lock.getName(), takenLock);
        }
        switch (lock.getType()) {
          case READ:
            takenLock.readLocks.add(info);
            break;
          case WRITE:
            takenLock.writeLocks.add(info);
            break;
        }
      }
    }
    return takenLocks;
  }

  /**
   * Class {@code TakenLockInfo}
   * <p/>
   * Helper class for taken locks representation
   */
  static final class TakenLockInfo {

    /**
     * Builds that have acquired read lock
     */
    final Set<BuildPromotionInfo> readLocks = new HashSet<BuildPromotionInfo>();

    /**
     * Builds that have acquired write lock
     */
    final Set<BuildPromotionInfo> writeLocks = new HashSet<BuildPromotionInfo>();
  }
}
