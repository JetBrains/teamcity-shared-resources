/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server.report;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildUsedResourcesReport {

  @NotNull
  private static final Logger LOG = Logger.getInstance(BuildUsedResourcesReport.class.getName());

  private static final String ARTIFACT_PATH = SharedResourcesPluginConstants.BASE_ARTIFACT_PATH + "/used_resources.json";

  @NotNull
  private final UsedResourcesSerializer mySerializer;

  public BuildUsedResourcesReport(@NotNull final UsedResourcesSerializer serializer) {
    mySerializer = serializer;
  }

  public void save(@NotNull final BuildPromotionEx promo,
                   @NotNull final Map<String, Resource> resources,
                   @NotNull final Map<Lock, String> takenLocks) {
    if (takenLocks.isEmpty()) return;
    final List<UsedResource> usedResources = new ArrayList<>();
    takenLocks.forEach((lock, value) -> {
      final Resource resource = resources.get(lock.getName());
      if (resource != null) {
        usedResources.add(new UsedResource(resource, Collections.singleton(Lock.createFrom(lock, value))));
      } else {
        LOG.warn("Resource with name " + lock.getName() + " was not found for used resources report for build promotion with id " + promo.getId());
      }
    });
    final File artifact = new File(promo.getArtifactsDirectory(), ARTIFACT_PATH);
    try {
      if (FileUtil.createParentDirs(artifact)) {
        try (FileWriter w = new FileWriter(artifact)) {
          mySerializer.write(usedResources, w);
        }
      }
    } catch (IOException | JsonIOException e) {
      LOG.warnAndDebugDetails("Failed to store resources and locks to " + artifact.getPath() + " for build promotion with id " + promo.getId(), e);
    }
  }

  public List<UsedResource> load(@NotNull final SBuild build) {
    final File artifact = new File(build.getArtifactsDirectory(), ARTIFACT_PATH);
    if (artifact.isFile()) {
      try (FileReader reader = new FileReader(artifact)) {
        return mySerializer.read(reader);
      } catch(IOException | JsonParseException e) {
        LOG.warnAndDebugDetails("Failed to load stored resources and locks from " + artifact.getPath() + " for build with id " + build.getBuildId(), e);
      }
    }
    return Collections.emptyList();
  }

  public boolean exists(@NotNull final SBuild build) {
    return new File(build.getArtifactsDirectory(), ARTIFACT_PATH).isFile();
  }
}
