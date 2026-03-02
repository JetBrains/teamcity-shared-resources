package jetbrains.buildServer.sharedResources.server.runtime;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
  @NotNull
  private static final String FILE_NAME = "taken_locks.txt";
  @NotNull
  static final String FILE_PATH = SharedResourcesPluginConstants.BASE_ARTIFACT_PATH + "/" + FILE_NAME; // package visibility for tests
  private final static Logger LOG = Logger.getInstance(CDSBasedTakenLocksStorage.class);
  private static final String SHARED_RESOURCES_TAKEN_LOCKS_CDS_ID = "SharedResourcesTakenLocks";
  public static final String BUILD_ID_PREFIX = "buildId:";

  private final CustomDataStorage myTakenLocksStorage;
  private final BuildPromotionManager myBuildPromotionManager;

  public CDSBasedTakenLocksStorage(@NotNull ProjectManager projectManager,
                                   @NotNull BuildPromotionManager buildPromotionManager,
                                   @NotNull final EventDispatcher<BuildServerListener> dispatcher) {
    myBuildPromotionManager = buildPromotionManager;
    myTakenLocksStorage = projectManager.getRootProject().getCustomDataStorage(SHARED_RESOURCES_TAKEN_LOCKS_CDS_ID);
    dispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildFinished(@NotNull SRunningBuild build) {
        myTakenLocksStorage.putValue(buildLocksKey(build.getBuildPromotion()), null);
      }

      @Override
      public void buildInterrupted(@NotNull SRunningBuild build) {
        myTakenLocksStorage.putValue(buildLocksKey(build.getBuildPromotion()), null);
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
    return readFromStorage(Collections.singleton(buildPromotion)).getOrDefault(buildPromotion, Collections.emptyMap());
  }

  @NotNull
  @Override
  public Map<BuildPromotion, Map<String, Lock>> loadMultiple(@NotNull Collection<BuildPromotion> buildPromotions) {
    return readFromStorage(buildPromotions);
  }

  @NotNull
  @Override
  public Map<BuildPromotion, Map<String, Lock>> getAllTakenLocks() {
    Map<String, String> vals = myTakenLocksStorage.getValues();
    if (vals == null) return Collections.emptyMap();

    Map<BuildPromotion, Map<String, Lock>> res = new HashMap<>();
    for (Map.Entry<String, String> lockEntry: vals.entrySet()) {
      if (!lockEntry.getKey().startsWith(BUILD_ID_PREFIX)) {
        LOG.warn("Incorrect lock entry prefix " + lockEntry.getKey() + ", the entry will be removed");
        myTakenLocksStorage.putValue(lockEntry.getKey(), null);
        continue;
      }

      try {
        Long buildId = Long.parseLong(lockEntry.getKey().substring(BUILD_ID_PREFIX.length()));
        BuildPromotion bp = myBuildPromotionManager.findPromotionById(buildId);
        if (bp == null) {
          myTakenLocksStorage.putValue(lockEntry.getKey(), null);
        }
        res.put(bp, deserializeTakenLocks(lockEntry.getValue()));
      } catch (NumberFormatException e) {
        // broken entry
        LOG.warnAndDebugDetails("Could not parse build id from " + lockEntry.getKey() + ", the entry will be removed", e);
        myTakenLocksStorage.putValue(lockEntry.getKey(), null);
      }
    }

    return res;
  }

  @Override
  public boolean locksStored(@NotNull BuildPromotion buildPromotion) {
    return myTakenLocksStorage.getValue(buildLocksKey(buildPromotion)) != null;
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
      myTakenLocksStorage.putValue(buildLocksKey(buildPromotion), null);
    } else {
      myTakenLocksStorage.putValue(buildLocksKey(buildPromotion), serializeTakenLocks(takenLocks));
    }
  }

  @NotNull
  private Map<BuildPromotion, Map<String, Lock>> readFromStorage(@NotNull Collection<BuildPromotion> buildPromotions) {
    Map<BuildPromotion, Map<String, Lock>> res = new HashMap<>();
    for (BuildPromotion bp: buildPromotions) {
      String value = myTakenLocksStorage.getValue(buildLocksKey(bp));
      if (value == null) continue;
      res.put(bp, deserializeTakenLocks(value));
    }

    return res;
  }

  @NotNull
  private static String buildLocksKey(@NotNull BuildPromotion promotion) {
    return BUILD_ID_PREFIX + promotion.getId();
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
