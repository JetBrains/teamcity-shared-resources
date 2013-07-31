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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class {@code LocksStorageImpl}
 *
 * Implements storage for taken locks during build execution
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class LocksStorageImpl implements LocksStorage {

  @NotNull
  static final String FILE_PARENT = ".teamcity/" + SharedResourcesPluginConstants.PLUGIN_NAME;

  @NotNull
  private static final String FILE_NAME = "taken_locks.txt";

  @NotNull
  static final String FILE_PATH = FILE_PARENT + "/" + FILE_NAME; // package visibility for tests

  @NotNull
  private static final Logger log = Logger.getInstance(LocksStorageImpl.class.getName());

  @NotNull
  private static final String MY_ENCODING = "UTF-8";

  /**
   * Map with separate guarding lock for each build
   */
  @NotNull
  private final ConcurrentMap<Long, ReentrantLock> myGuards = new ConcurrentHashMap<Long, ReentrantLock>();

  @Override
  public void store(@NotNull final SBuild build, @NotNull final Map<Lock, String> takenLocks) {
    if (!takenLocks.isEmpty()) {
      final Long buildId = build.getBuildId();
      final ReentrantLock l = new ReentrantLock(true);
      try {
        l.lock();
        myGuards.put(buildId, l);
        final Collection<String> serializedStrings = new ArrayList<String>();
        for (Map.Entry<Lock, String> entry: takenLocks.entrySet()) {
          serializedStrings.add(serializeTakenLock(entry.getKey(), entry.getValue()));
        }
        try {
          final File artifact = new File(build.getArtifactsDirectory(), FILE_PATH);
          if (FileUtil.createParentDirs(artifact)) {
            FileUtil.writeFile(artifact, StringUtil.join(serializedStrings, "\n"), MY_ENCODING);
          } else {
            log.warn("Failed to create parent dirs for file with taken locks for build {" + build + "}");
          }
        } catch (IOException e) {
          log.warn("Failed to store taken locks for build [" + build + "]; Message is: " + e.getMessage());
        }
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
      final Map<String, Lock> result = new HashMap<String, Lock>();
      final File artifact = new File(build.getArtifactsDirectory(), FILE_PATH);
      if (artifact.exists()) {
        try {
          final String content = FileUtil.readText(artifact, MY_ENCODING);
          final String[] lines = content.split("\\r?\\n");
          for (String line: lines) {
            final Lock lock = deserializeTakenLock(line);
            if (lock != null) {
              result.put(lock.getName(), lock);
            } else {
              if (log.isDebugEnabled()) {
                log.debug("Wrong locks storage format in file {" + artifact.getAbsolutePath() + "} line: {" + line + "}");
              }
            }
          }
        } catch(IOException e) {
          log.warn("Failed to load taken locks for build [" + build + "]; Message is: " + e.getMessage());
        }
      }
      return result;
    } finally {
      if (l != null) {
        l.unlock();
      }
    }
  }

  @Override
  public boolean locksStored(@NotNull final SBuild build) {
    final ReentrantLock l = myGuards.get(build.getBuildId());
    try {
      if (l != null) {
        l.lock();
      }
      final File artifact = new File(build.getArtifactsDirectory(), FILE_PATH);
      return artifact.exists();
    } finally {
      if (l != null) {
        l.unlock();
      }
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
      String value =  StringUtil.trim(strings.get(2));
      if (value == null) {
        value = "";
      }
      result = new Lock(strings.get(0), LockType.byName(strings.get(1)), value);
    }
    return result;
  }
}
