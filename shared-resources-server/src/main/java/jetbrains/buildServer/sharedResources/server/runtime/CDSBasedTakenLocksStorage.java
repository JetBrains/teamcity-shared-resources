package jetbrains.buildServer.sharedResources.server.runtime;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CDSBasedTakenLocksStorage implements LocksStorage {
  private static final String FILE_NAME = "taken_locks.txt";
  static final String FILE_PATH = SharedResourcesPluginConstants.BASE_ARTIFACT_PATH + "/" + FILE_NAME; // package visibility for tests
  private final static Logger LOG = Logger.getInstance(CDSBasedTakenLocksStorage.class);
  private static final String SHARED_RESOURCES_TAKEN_LOCKS_CDS_ID = "SharedResourcesTakenLocks";
  private static final String BUILD_ID_PREFIX = "buildId:";

  private final BuildPromotionManager myBuildPromotionManager;
  private final ServerResponsibility myServerResponsibility;
  private final ProjectManager myProjectManager;
  private final BuildsManager myBuildsManager;

  public CDSBasedTakenLocksStorage(@NotNull ProjectManager projectManager,
                                   @NotNull BuildPromotionManager buildPromotionManager,
                                   @NotNull BuildsManager buildsManager,
                                   @NotNull ServerResponsibility serverResponsibility,
                                   @NotNull EventDispatcher<BuildServerListener> dispatcher) {
    myBuildPromotionManager = buildPromotionManager;
    myBuildsManager = buildsManager;
    myServerResponsibility = serverResponsibility;
    myProjectManager = projectManager;
    dispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildFinished(@NotNull SRunningBuild build) {
        removeTakenLocks(build.getBuildPromotion());
      }

      @Override
      public void buildInterrupted(@NotNull SRunningBuild build) {
        removeTakenLocks(build.getBuildPromotion());
      }

      private void removeTakenLocks(@NotNull BuildPromotion buildPromotion) {
        removeTakenLocksForEntry(buildLocksKey(buildPromotion));
      }
    });
  }

  @Override
  public void store(@NotNull BuildPromotion buildPromotion, @NotNull Map<Lock, String> takenLocks) {
    saveToStorage(buildPromotion, takenLocks);
    // save taken locks artifact for diagnostics purposes
    saveTakenLocksArtifact(buildPromotion, takenLocks);
  }

  @NotNull
  @Override
  public Map<String, Lock> load(@NotNull BuildPromotion buildPromotion) {
    String value = getTakenLocksStorage().getValue(buildLocksKey(buildPromotion));
    if (value == null) {
      return Collections.emptyMap();
    }

    return deserializeTakenLocks(value);
  }

  @NotNull
  private CustomDataStorage getTakenLocksStorage() {
    return myProjectManager.getRootProject().getCustomDataStorage(SHARED_RESOURCES_TAKEN_LOCKS_CDS_ID);
  }

  @NotNull
  @Override
  public Map<BuildPromotion, Map<String, Lock>> getAllTakenLocks() {
    Map<String, String> vals = getTakenLocksStorage().getValues();
    if (vals == null) return Collections.emptyMap();

    Map<Long, Map<String, Lock>> unfilteredLocks = new HashMap<>();
    for (Map.Entry<String, String> lockEntry: vals.entrySet()) {
      if (!lockEntry.getKey().startsWith(BUILD_ID_PREFIX)) {
        LOG.warn("Incorrect lock entry prefix " + lockEntry.getKey() + ", the entry will be removed");
        removeTakenLocksForEntry(lockEntry.getKey());
        continue;
      }

      try {
        Long buildId = Long.parseLong(lockEntry.getKey().substring(BUILD_ID_PREFIX.length()));
        unfilteredLocks.put(buildId, deserializeTakenLocks(lockEntry.getValue()));
      } catch (NumberFormatException e) {
        // broken entry
        LOG.warnAndDebugDetails("Could not parse build id from " + lockEntry.getKey() + ", the entry will be removed", e);
        removeTakenLocksForEntry(lockEntry.getKey());
      }
    }

    Map<BuildPromotion, Map<String, Lock>> result = new HashMap<>();
    // we need to go through the locks to ensure that they are taken by the existing builds
    for (SBuild build: myBuildsManager.findBuildInstances(unfilteredLocks.keySet())) {
      unfilteredLocks.remove(build.getBuildId());
      if (build instanceof SRunningBuild) {
        result.put(build.getBuildPromotion(), unfilteredLocks.get(build.getBuildId()));
        continue;
      }

      if (myServerResponsibility.canManageBuilds()) {
        // we're using debug logging because we have a buildFinished event handler which
        // also removes the locks upon finishing of a build, and there is a chance it did not remove the locks yet,
        // so the fact that we found a finished build here is not necessarily a problem
        LOG.debug("Removing the stale locks belonging to an already finished build: " + LogUtil.describe(build));
        removeTakenLocksForEntry(buildLocksKey(build.getBuildId()));
      }
    }

    if (myServerResponsibility.canManageBuilds()) {
      for (long buildId: unfilteredLocks.keySet()) {
        LOG.warn("Removing the stale locks belonging to no longer existing build: " + buildId);
        removeTakenLocksForEntry(buildLocksKey(buildId));
      }
    }

    return result;
  }

  private void removeTakenLocksForEntry(@NotNull String key) {
    if (!myServerResponsibility.canManageBuilds()) return;
    getTakenLocksStorage().putValue(key, null);
  }

  @Override
  public boolean locksStored(@NotNull BuildPromotion buildPromotion) {
    return getTakenLocksStorage().getValue(buildLocksKey(buildPromotion)) != null;
  }

  @VisibleForTesting
  @NotNull
  static String serializeTakenLock(@NotNull final Lock lock, @NotNull final String value) {
    return StringUtil.join("\t", lock.getName(), lock.getType(), value.isEmpty() ? " " : value);
  }

  @VisibleForTesting
  @Nullable
  static Lock deserializeTakenLock(@NotNull final String line) {
    final List<String> strings = StringUtil.split(line, true, '\t'); // we need empty values for locks without values
    Lock result = null;
    if (strings.size() == 3) {
      String value = StringUtil.trim(strings.get(2));
      if (value == null) {
        value = "";
      }
      final LockType type = LockType.byName(strings.get(1));
      result = type == null ? null : new Lock(strings.get(0), type, value);
    }
    return result;
  }

  @VisibleForTesting
  @NotNull
  static Map<String, Lock> deserializeTakenLocks(@NotNull final String serializedValue) {
    Map<String, Lock> res = new HashMap<>();
    for (String line: StringUtil.split(serializedValue, true, '\n')) {
      Lock lock = deserializeTakenLock(line);
      if (lock != null) {
        res.put(lock.getName(), lock);
      }
    }

    return res;
  }

  @NotNull
  static String serializeTakenLocks(@NotNull Map<Lock, String> takenLocks) {
    StringBuilder serializedLocks = new StringBuilder();
    takenLocks.forEach((lock, value) -> {
      serializedLocks.append(serializeTakenLock(lock, value)).append('\n');
    });
    return serializedLocks.toString();
  }

  private void saveToStorage(@NotNull BuildPromotion buildPromotion, @NotNull Map<Lock, String> takenLocks) {
    if (takenLocks.isEmpty()) {
      removeTakenLocksForEntry(buildLocksKey(buildPromotion));
    } else {
      getTakenLocksStorage().putValue(buildLocksKey(buildPromotion), serializeTakenLocks(takenLocks));
    }
  }

  @NotNull
  private static String buildLocksKey(@NotNull BuildPromotion promotion) {
    return buildLocksKey(promotion.getId());
  }

  @NotNull
  private static String buildLocksKey(long buildId) {
    return BUILD_ID_PREFIX + buildId;
  }

  private void saveTakenLocksArtifact(@NotNull final BuildPromotion buildPromotion, @NotNull final Map<Lock, String> takenLocks) {
    if (!takenLocks.isEmpty()) {
      try {
        final File artifact = new File(buildPromotion.getArtifactsDirectory(), FILE_PATH);
        if (FileUtil.createParentDirs(artifact)) {
          FileUtil.writeFile(artifact, serializeTakenLocks(takenLocks), StandardCharsets.UTF_8);
        } else {
          LOG.warn("Failed to create parent dirs for file with taken locks for build " + LogUtil.describe(buildPromotion));
        }
      } catch (IOException e) {
        LOG.warnAndDebugDetails("Failed to store taken locks for build " + LogUtil.describe(buildPromotion), e);
      }
    }
  }
}
