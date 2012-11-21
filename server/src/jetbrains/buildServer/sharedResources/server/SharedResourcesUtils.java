package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.serverSide.buildDistribution.RunningBuildInfo;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
   * Parses build feature parameters. Exposes them to the build
   *
   * todo: javadoc
   * todo: tests
   *
   * @see SharedResourcesPluginConstants#LOCK_PREFIX
   * @see SharedResourcesPluginConstants#LOCKS_FEATURE_PARAM_KEY
   *
   * @param serializedParam parameter stored in build feature
   * @return map representation of build feature parameter
   */
  @NotNull
  public static Map<String, String> featureParamToBuildParams(String serializedParam) {
    final List<Lock> locks = getLocks(serializedParam);
    final Map<String, String> result = new HashMap<String, String>();
    for (Lock lock: locks) {
      result.put(lockAsBuildParam(lock), "");
    }
    return result;
  }

  private static String lockAsBuildParam(Lock lock) {
    final StringBuilder sb = new StringBuilder(LOCK_PREFIX);
    sb.append(lock.getType());
    sb.append(".");
    sb.append(lock.getName());
    return sb.toString();
  }

  /**
   * Returns names of taken locks
   * @param serializedFeatureParam feature parameter, that contains all locks
   * @return List of lock names, {@code result[0]} - contains readLocks,
   * {@code result[1]} contains writeLocks
   */
  @NotNull
  public static List<List<String>> getLockNames(String serializedFeatureParam) {
    final List<List<String>> result = new ArrayList<List<String>>();
    final List<String> readLockNames = new ArrayList<String>();
    final List<String> writeLockNames = new ArrayList<String>();
    final List<Lock> locks = getLocks(serializedFeatureParam);
    for (Lock lock: locks) {
      switch (lock.getType()) {
        case READ:
          readLockNames.add(lock.getName());
          break;
        case WRITE:
          writeLockNames.add(lock.getName());
          break;
      }
    }
    result.add(readLockNames);
    result.add(writeLockNames);
    return result;
  }


  public static List<Lock> getLocks(String serializedFeatureParam) {
    final List<Lock> result = new ArrayList<Lock>();
    if (serializedFeatureParam != null && !"".equals(serializedFeatureParam)) {
      final List<String> serializedLocks = StringUtil.split(serializedFeatureParam, true, '\n');
      for (String str: serializedLocks) {
        final Lock lock = getSingleLockFromString(str);
        if (lock != null) {
          result.add(lock);
        }
      }
    }
    return result;
  }


  private static Lock getSingleLockFromString(@NotNull String str) {
    int n = str.lastIndexOf(' ');
    final LockType type = LockType.byName(str.substring(n + 1));
    Lock result = null;
    if (type != null) {
      result =  new Lock(str.substring(0, n), type);
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
    final ParametersProvider pp = ((BuildPromotionEx)buildPromotionInfo).getParametersProvider();
    return extractLocksFromParams(pp.getAll());
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

  static Collection<Lock> extractLocksFromParams(@NotNull Map<String, String> params) {
    List<Lock> result = new ArrayList<Lock>();
    for (String str: params.keySet()) {
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(str);
      if (lock != null) {
        result.add(lock);
      }
    }
    return result;
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
