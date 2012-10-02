package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.serverSide.buildDistribution.RunningBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.LOCK_PREFIX;

/**
 *
 * @author Oleg Rybak
 */
final class WaitPreconditionUtils {

  private static final int PREFIX_OFFSET = LOCK_PREFIX.length();

  private static final Logger LOG = Logger.getInstance(WaitPreconditionUtils.class.getName());

  static Lock getLockFromBuildParam(String paramName) {
    Lock result = null;
    if (paramName.startsWith(LOCK_PREFIX)) {
      String lockString = paramName.substring(PREFIX_OFFSET);
      LockType lockType = null;
      for (LockType type: LockType.values()) {
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
          LOG.warn("Error parsing lock name of '" + paramName + "'. Supported format is 'system.locks.[read|write]Lock.<lock name>'");
          return null;
        }
        result = new Lock(lockName, lockType);
      } catch (IndexOutOfBoundsException e) {
        LOG.warn("Error parsing lock name of '" + paramName + "'. Supported format is 'system.locks.[read|write]Lock.<lock name>'");
        return null;
      }
    }
    return result;
  }

  @NotNull
  static Collection<Lock> extractLocksFromPromotion(BuildPromotionInfo buildPromotionInfo) {
    Collection<Lock> result = new HashSet<Lock>();
    Map<String, String> buildParameters = buildPromotionInfo.getBuildParameters();
    for (Map.Entry<String, String> param: buildParameters.entrySet()) {
      Lock lock = getLockFromBuildParam(param.getKey());
      if (lock != null) {
        result.add(lock);
      }
    }
    return result;
  }

  @NotNull
  static Collection<BuildPromotionInfo> getBuildPromotions(@NotNull Collection<RunningBuildInfo> runningBuilds, @NotNull Collection<QueuedBuildInfo> queuedBuilds) {
    ArrayList<BuildPromotionInfo> result = new ArrayList<BuildPromotionInfo>(runningBuilds.size() + queuedBuilds.size());
    for (RunningBuildInfo runningBuildInfo: runningBuilds){
      result.add(runningBuildInfo.getBuildPromotionInfo());
    }
    for (QueuedBuildInfo queuedBuildInfo: queuedBuilds){
      result.add(queuedBuildInfo.getBuildPromotionInfo());
    }
    return result;
  }

  static boolean locksAvailable(Collection<Lock> locksToTake, Collection<BuildPromotionInfo> buildPromotions) {
    Map<String, TakenLockInfo> takenLocks = new HashMap<String, TakenLockInfo>();
    for (BuildPromotionInfo info: buildPromotions) {
      Collection<Lock> locks = extractLocksFromPromotion(info);
      for (Lock lock: locks) {
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

    boolean result = true;
    for (Lock lock: locksToTake) {
      TakenLockInfo takenLock = takenLocks.get(lock.getName());
      switch (lock.getType()) {
        case READ:
          if (takenLock != null && !takenLock.writeLocks.isEmpty()) {
            return false;
          }
          break;
        case WRITE:
          if (takenLock != null && (!takenLock.readLocks.isEmpty() || !takenLock.writeLocks.isEmpty())) {
            return false;
          }
          break;
      }
    }
    return result;
  }

  static final class TakenLockInfo {
    final Set<BuildPromotionInfo> readLocks = new HashSet<BuildPromotionInfo>();
    final Set<BuildPromotionInfo> writeLocks = new HashSet<BuildPromotionInfo>();
  }
}
