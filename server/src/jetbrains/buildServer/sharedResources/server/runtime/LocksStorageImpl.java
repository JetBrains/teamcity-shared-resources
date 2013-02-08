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

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Class {@code LocksStorageImpl}
 *
 * Implements storage for taken locks during build execution
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class LocksStorageImpl implements LocksStorage {

  static final String FILE_PARENT = ".teamcity/" + SharedResourcesPluginConstants.PLUGIN_NAME;

  private static final String FILE_NAME = "taken_locks.txt";

  static final String FILE_PATH = FILE_PARENT + "/" + FILE_NAME; // package visibility for tests

  private static final Logger log = Logger.getInstance(LocksStorageImpl.class.getName());

  @Override
  public void store(@NotNull final SBuild build, @NotNull final Map<Lock, String> takenLocks) {
    if (!takenLocks.isEmpty()) {
      final Collection<String> serializedStrings = new ArrayList<String>();
      for (Map.Entry<Lock, String> entry: takenLocks.entrySet()) {
        serializedStrings.add(serializeTakenLock(entry.getKey(), entry.getValue()));
      }
      try {
        final File artifact = new File(build.getArtifactsDirectory(), FILE_PATH);
        if (FileUtil.createParentDirs(artifact)) {
          FileUtil.writeFile(artifact, StringUtil.join(serializedStrings, "\n"), "UTF-8");
        } else {
          log.warn("Failed to create parent dirs for file with taken locks");
        }
      } catch (IOException e) {
        log.warn("Failed to store taken locks for build [" + build + "]; Message is: " + e.getMessage());
      }
    }
  }

  @NotNull
  @Override
  public Map<Lock, String> load(@NotNull final SBuild build) {
    final Map<Lock, String> result = new HashMap<Lock, String>();
    final File artifact = new File(build.getArtifactsDirectory(), FILE_PATH);
    if (artifact.exists()) {
      try {
        log.info("Got artifact with locks for build [" + build +"]");
        final List<String> lines = FileUtil.readFile(artifact);
        for (String line: lines) {
          List<String> strings = StringUtil.split(line, true, '\t'); // we need empty values for locks without values
          if (strings.size() == 3) {
            Lock lock = new Lock(strings.get(0), LockType.byName(strings.get(1)));
            result.put(lock, StringUtil.trim(strings.get(2)));
          } else {
            log.warn("Wrong locks storage format"); // todo: line? file?
          }
        }
      } catch(IOException e) {
        log.warn("Failed to load taken locks for build [" + build + "]; Message is: " + e.getMessage());
      }
    } else {
      log.info("Skipping artifact for build [" + build +"]. No locks are taken");
    }
    return result;
  }


  @Override
  public boolean locksStored(@NotNull final SBuild build) {
    final File artifact = new File(build.getArtifactsDirectory(), FILE_PATH);
    return artifact.exists();
  }

  private static String serializeTakenLock(@NotNull final Lock lock, @NotNull final String value) {
    return StringUtil.join("\t", lock.getName(), lock.getType(), value.equals("") ? " " : value);
  }
}
