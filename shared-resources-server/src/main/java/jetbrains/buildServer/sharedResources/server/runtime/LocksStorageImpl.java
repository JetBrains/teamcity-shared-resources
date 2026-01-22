

package jetbrains.buildServer.sharedResources.server.runtime;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Striped;
import com.intellij.openapi.diagnostic.Logger;
import gnu.trove.TLongHashSet;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class {@code LocksStorageImpl}
 * <p>
 * Implements storage for taken locks during build execution
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class LocksStorageImpl implements LocksStorage {

  @NotNull
  private static final String FILE_NAME = "taken_locks.txt";

  @NotNull
  static final String FILE_PATH = SharedResourcesPluginConstants.BASE_ARTIFACT_PATH + "/" + FILE_NAME; // package visibility for tests

  @NotNull
  private static final Logger log = Logger.getInstance(LocksStorageImpl.class.getName());

  @NotNull
  private static final String MY_ENCODING = "UTF-8";

  /**
   * Contains the set of build ids, that contain taken locks that are stored
   * Added to avoid calling of {@code CacheLoader} for the items that were not stored
   */
  @NotNull
  private final TLongHashSet existsSet = new TLongHashSet();

  /**
   * Global lock for cache/exists set modification
   */
  @NotNull
  private final ReadWriteLock myGlobalLock = new ReentrantReadWriteLock(true);

  @SuppressWarnings("UnstableApiUsage")
  @NotNull
  private final LoadingCache<BuildPromotion, Map<String, Lock>> myLocksCache;

  @SuppressWarnings("UnstableApiUsage")
  @NotNull
  private final Striped<java.util.concurrent.locks.Lock> myGuards = Striped.lazyWeakLock(TeamCityProperties.getInteger("teamcity.sharedResources.locksStorage.stripedSize", 300));

  public LocksStorageImpl(@NotNull final EventDispatcher<BuildServerListener> dispatcher) {
    CacheLoader<BuildPromotion, Map<String, Lock>> loader = new CacheLoader<BuildPromotion, Map<String, Lock>>() {
      @Override
      public Map<String, Lock> load(@NotNull final BuildPromotion buildPromotion) {
        final Map<String, Lock> result;
        final File artifact = new File(buildPromotion.getArtifactsDirectory(), FILE_PATH);
        if (artifact.exists()) {
          result = new HashMap<>();
          try {
            final String content = FileUtil.readText(artifact, MY_ENCODING);
            final String[] lines = StringUtil.splitLines(content);
            for (String line : lines) {
              final Lock lock = deserializeTakenLock(line);
              if (lock != null) {
                result.put(lock.getName(), lock);
              } else {
                if (log.isDebugEnabled()) {
                  log.debug("Wrong locks storage format in file {" + artifact.getAbsolutePath() + "} line: {" + line + "}");
                }
              }
            }
          } catch (IOException e) {
            log.warn("Failed to load taken locks for build [" + buildPromotion + "]; Message is: " + e.getMessage());
          }
        } else {
          result = Collections.emptyMap();
        }
        return result;
      }
    };

    myLocksCache = CacheBuilder.newBuilder()
                               .maximumSize(TeamCityProperties.getInteger("teamcity.sharedResources.locksStorage.cacheSize", 300)) // each entry corresponds to a running build
                               .build(loader);

    dispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildFinished(@NotNull SRunningBuild build) {
        withLock(buildPromotionLock(build.getBuildPromotion()),
                 () -> {
                   withLock(myGlobalLock::writeLock,
                            () -> {
                              myLocksCache.invalidate(build.getBuildPromotion());
                              existsSet.remove(build.getBuildPromotion().getId());
                              return null;
                            });
                   return null;
                 });
      }
    });
  }

  @Override
  public void store(@NotNull final BuildPromotion buildPromotion,
                    @NotNull final Map<Lock, String> takenLocks) {
    if (!takenLocks.isEmpty()) {
      withLock(buildPromotionLock(buildPromotion), () -> {
        final Collection<String> serializedStrings = new ArrayList<>();
        final Map<String, Lock> locksToStore = new HashMap<>();
        takenLocks.forEach((lock, value) -> {
          serializedStrings.add(serializeTakenLock(lock, value));
          locksToStore.put(lock.getName(), Lock.createFrom(lock, value));
        });
        try {
          final File artifact = new File(buildPromotion.getArtifactsDirectory(), FILE_PATH);
          if (FileUtil.createParentDirs(artifact)) {
            FileUtil.writeFile(artifact, StringUtil.join(serializedStrings, "\n"), MY_ENCODING);
            withLock(myGlobalLock::writeLock, () -> {
              myLocksCache.put(buildPromotion, locksToStore);
              existsSet.add(buildPromotion.getId());
              return null;
            });
          } else {
            log.warn("Failed to create parent dirs for file with taken locks for build {" + buildPromotion + "}");
          }
        } catch (IOException e) {
          log.warn("Failed to store taken locks for build [" + buildPromotion + "]; Message is: " + e.getMessage());
        }
        return null;
      });
    }
  }

  @NotNull
  @Override
  public Map<String, Lock> load(@NotNull final BuildPromotion buildPromotion) {
    return withLock(buildPromotionLock(buildPromotion), () -> getFromCacheSafe(buildPromotion));
  }

  @Override
  public boolean locksStored(@NotNull final BuildPromotion buildPromotion) {
    return withLock(buildPromotionLock(buildPromotion), () -> withLock(myGlobalLock::readLock, () -> existsSet.contains(buildPromotion.getId())));
  }

  @NotNull
  private Map<String, Lock> getFromCacheSafe(@NotNull final BuildPromotion buildPromotion) {
    try {
      return myLocksCache.get(buildPromotion); // guava cache is thread safe
    } catch (Exception e) {
      log.warn(e);
      return Collections.emptyMap();
    }
  }

  @NotNull
  private String serializeTakenLock(@NotNull final Lock lock, @NotNull final String value) {
    return StringUtil.join("\t", lock.getName(), lock.getType(), value.equals("") ? " " : value);
  }

  @Nullable
  private Lock deserializeTakenLock(@NotNull final String line) {
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

  private Supplier<java.util.concurrent.locks.Lock> buildPromotionLock(@NotNull final BuildPromotion promotion) {
    return () -> myGuards.get(promotion);
  }

  private <T> T withLock(Supplier<java.util.concurrent.locks.Lock> s, Callable<T> block) {
    java.util.concurrent.locks.Lock lock = s.get();
    lock.lock();
    T result = null;
    try {
      result = block.call();
    } catch (Throwable t) {
      log.warn(t);
    } finally {
      lock.unlock();
    }
    return result;
  }
}