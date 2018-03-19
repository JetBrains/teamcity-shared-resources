/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.server.runtime;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.intellij.openapi.diagnostic.Logger;
import gnu.trove.TLongHashSet;
import gnu.trove.impl.sync.TSynchronizedLongObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.server.storage.BuildArtifactsAccessor;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

/**
 * Class {@code LocksStorageImpl}
 *
 * Implements storage for taken locks during build execution
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class LocksStorageImpl implements LocksStorage {

  @NotNull
  private static final Logger log = Logger.getInstance(LocksStorageImpl.class.getName());

  /**
   * Contains the set of build ids, that contain taken locks that are stored
   * Added to avoid calling of {@code CacheLoader} for the items that were not stored
   */
  @NotNull
  private final TLongHashSet existsSet = new TLongHashSet();

  @NotNull
  private final BuildArtifactsAccessor myAccessor;

  /**
   * Stores last N entries of taken locks
   * It is highly unlikely, that we will have more than 300 running builds at the same time
   * Ff we do, data for at least 300 of them will be accessed without accessing artifacts storage
   */
  @NotNull
  private LoadingCache<SBuild, Map<String, Lock>> myLocksCache;

  /**
   * Map with separate guarding lock for each build
   */
  @NotNull
  private final TLongObjectMap<ReentrantLock> myGuards = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());


  public LocksStorageImpl(@NotNull final EventDispatcher<BuildServerListener> dispatcher,
                          @NotNull final BuildArtifactsAccessor accessor) {
    myAccessor = accessor;

    final CacheLoader<SBuild, Map<String, Lock>> loader = new CacheLoader<SBuild, Map<String, Lock>>() {
      @Override
      public Map<String, Lock> load(@NotNull final SBuild build) {
        return myAccessor.load(build);
      }
    };

    myLocksCache = CacheBuilder.<SBuild, Map<String, Lock>>newBuilder()
            .maximumSize(300) // each entry corresponds to a running build
            .build(loader);

    dispatcher.addListener(new BuildServerAdapter() {

      /**
       * Evicts stored items from cache, as the build is finished and locks are no longer needed
       */
      @Override
      public void buildFinished(@NotNull SRunningBuild build) {
        final ReentrantLock l = myGuards.get(build.getBuildId());
        try {
          if (l != null) {
            l.lock();
          }
          myLocksCache.invalidate(build);
          existsSet.remove(build.getBuildId());
        } finally {
          if (l != null) {
            l.unlock();
          }
        }
      }
    });
  }

  @Override
  public void store(@NotNull final SBuild build, @NotNull final Map<Lock, String> takenLocks) {
    if (!takenLocks.isEmpty()) {
      final Long buildId = build.getBuildId();
      final ReentrantLock l = new ReentrantLock(true);
      try {
        l.lock();
        myGuards.put(buildId, l);
        final Map<String, Lock> locksToStore = new HashMap<>();
        takenLocks.forEach((lock, value) -> locksToStore.put(lock.getName(), Lock.createFrom(lock, value)));
        try {
          myAccessor.store(build, takenLocks);
        } catch (IOException e) {
          log.warn("Failed to store taken locks for build [" + build + "]; Message is: " + e.getMessage());
        }
        myLocksCache.put(build, locksToStore);
        existsSet.add(buildId);
      } finally {
        l.unlock();
        myGuards.remove(buildId);
      }
    }
  }

  @NotNull
  @Override
  public Map<String, Lock> load(@NotNull final SBuild build) {
    final ReentrantLock l = myGuards.get(build.getBuildId());
    try {
      if (l != null) {
        l.lock();
      }
      try {
        return myLocksCache.get(build);
      } catch (Exception e) {
        log.warn(e);
        return Collections.emptyMap();
      }
    } finally {
      if (l != null) {
        l.unlock();
      }
    }
  }

  @Override
  public boolean locksStored(@NotNull final SBuild build) {
    final long id = build.getBuildId();
    final ReentrantLock l = myGuards.get(id);
    try {
      if (l != null) {
        l.lock();
      }
      return existsSet.contains(id);
    } finally {
      if (l != null) {
        l.unlock();
      }
    }
  }
}
