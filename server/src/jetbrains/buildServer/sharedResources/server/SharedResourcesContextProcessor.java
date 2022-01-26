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

package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import gnu.trove.TLongHashSet;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.report.BuildUsedResourcesReport;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.getReservedResourceAttributeKey;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesContextProcessor implements BuildStartContextProcessor {

  @NotNull
  private static final Logger LOG = Logger.getInstance(SharedResourcesContextProcessor.class.getName());

  @NotNull
  private final Object o = new Object();

  @NotNull
  private final SharedResourcesFeatures myFeatures;

  @NotNull
  private final Locks myLocks;

  @NotNull
  private final Resources myResources;

  @NotNull
  private final LocksStorage myLocksStorage;

  @NotNull
  private final BuildUsedResourcesReport myBuildUsedResourcesReport;

  public SharedResourcesContextProcessor(@NotNull final SharedResourcesFeatures features,
                                         @NotNull final Locks locks,
                                         @NotNull final Resources resources,
                                         @NotNull final LocksStorage locksStorage,
                                         @NotNull final BuildUsedResourcesReport buildUsedResourcesReport) {
    myFeatures = features;
    myLocks = locks;
    myResources = resources;
    myLocksStorage = locksStorage;
    myBuildUsedResourcesReport = buildUsedResourcesReport;
  }

  /**
   *  (C_r0) <-- ... <-- (C_rN) <-- (C_0) <-- ... <-- (C_N) <--- (CURRENT_BUILD)
   * C_r0, ..., C_rN part of the (composite) build chain that has started before this build
   * when, for example, some other subtree started
   * has locks fixed and stored in locksStorage
   * has values for locks defined
   * NB: builds can belong to other projects - need to resolve resources to get available values
   * NB: match custom lock values by RESOURCES (otherwise value space can be different)
   * custom lock values (if matched by resources) should define ANY-type locks in lower chain
   * traversal should be top-to-bottom
   *
   * C_0, ..., C_N part of the (composite) build chain that is starting with the current build
   *
   * @param context context of current starting build
   */
  @Override
  public void updateParameters(@NotNull final BuildStartContext context) {
    final SRunningBuild startingBuild = context.getBuild();
    final BuildPromotionEx startingBuildPromotion = (BuildPromotionEx)startingBuild.getBuildPromotion();
    final TLongHashSet compositeIds = new TLongHashSet();
    // projectID -> Map of custom resources
    final Map<String, Map<String, CustomResource>> projectTreeCustomResources = new HashMap<>();
    // projectId -> Map of all resources
    final Map<String, Map<String, Resource>> projectTreeResources = new HashMap<>();
    // several locks on same resource may be taken by the chain
    synchronized (o) {
      if (TeamCityProperties.getBooleanOrTrue(SharedResourcesPluginConstants.RESOURCES_IN_CHAINS_ENABLED) && startingBuildPromotion.isPartOfBuildChain()) {
        // get all dependent composite promotions
        final List<BuildPromotionEx> depPromos = startingBuildPromotion.getDependentCompositePromotions();
        // collect promotion ids of composite builds in chain.
        // we don't need to check the values against them
        depPromos.forEach(promo -> compositeIds.add(promo.getId()));
        // some build promotions in composite chain may not have the locks stored -> we need to process and store locks
        depPromos.stream()
                 .filter(promo -> !myLocksStorage.locksStored(promo))
                 .forEach(promo -> processBuild(context, promo, projectTreeResources, projectTreeCustomResources));
      }
      processBuild(context, startingBuild.getBuildPromotion(), projectTreeResources, projectTreeCustomResources);
    }
  }

  private void processBuild(@NotNull final BuildStartContext context,
                            @NotNull final BuildPromotion currentBuildPromotion,
                            @NotNull final Map<String, Map<String, Resource>> projectTreeResources,
                            @NotNull final Map<String, Map<String, CustomResource>> projectTreeCustomResources) {
    if (currentBuildPromotion.getBuildType() == null || currentBuildPromotion.getProjectId() == null) {
      return;
    }
    final Map<String, Lock> locks = extractLocks(currentBuildPromotion);
    final Map<Lock, String> myTakenValues = initTakenValues(locks.values());
    // get custom resources from our locks
    final Map<String, Resource> projectResources = getResources(currentBuildPromotion.getProjectId(), projectTreeResources);
    // FIXME: disable custom resources processing for composite builds until method of consistent delivery of values to the chain is implemented
    // from UI: user should not be able to add a lock on custom resource in composite build => initTakenValues will not contain any custom resources
    // locks on regular resources will be saved
    if (!currentBuildPromotion.isCompositeBuild()) {
      final Map<String, CustomResource> myCustomResources = matchCustomResources(getCustomResources(currentBuildPromotion.getProjectId(), projectResources, projectTreeCustomResources), locks);
      // decide whether we need to resolve values
      if (!myCustomResources.isEmpty()) {
        for (Map.Entry<String, CustomResource> entry : myCustomResources.entrySet()) {
          if (entry.getValue().isEnabled()) {
            // get value space for current resources
            final List<String> values = new ArrayList<>(entry.getValue().getValues());
            final String name = entry.getKey();
            final Lock currentLock = locks.get(name);
            final String paramName = myLocks.asBuildParameter(currentLock);
            String currentValue;
            if (LockType.READ.equals(currentLock.getType())) {
              if (currentLock.getValue().equals("")) { // ANY lock
                currentValue = (String)((BuildPromotionEx)currentBuildPromotion).getAttribute(getReservedResourceAttributeKey(entry.getValue().getId()));
                if (currentValue == null) {
                  final String message = "Expected reserved value for resource (" + entry.getValue().getId() + "|" + name
                                         + ") in build promotion " + currentBuildPromotion.getId() + ", got null";
                  LOG.error(message);
                  throw new RuntimeException(message);
                }
              } else { // SPECIFIC lock
                currentValue = currentLock.getValue();
              }
              myTakenValues.put(currentLock, currentValue);
            } else { // ALL lock
              currentValue = StringUtil.join(values, ";");
            }
            context.addSharedParameter(paramName, currentValue);
          }
        }
      }
    }
    myLocksStorage.store(currentBuildPromotion, myTakenValues);
    myBuildUsedResourcesReport.save((BuildPromotionEx)currentBuildPromotion, projectResources, myTakenValues);
  }

  private Map<String, Lock> extractLocks(@NotNull final BuildPromotion buildPromotion) {
    final Map<String, Lock> result = new HashMap<>();
    if (buildPromotion.getBuildType() != null) {
      result.putAll(myLocks.fromBuildFeaturesAsMap(myFeatures.searchForFeatures(buildPromotion.getBuildType())));
    }
    return result;
  }

  private Map<String, CustomResource> matchCustomResources(@NotNull final Map<String, CustomResource> resources,
                                                           @NotNull final Map<String, Lock> locks) {
    final Map<String, CustomResource> result = new HashMap<>();
    for (Map.Entry<String, Lock> entry: locks.entrySet()) {
      final CustomResource r = resources.get(entry.getKey());
      if (r != null) {
        result.put(r.getName(), r);
      }
    }
    return result;
  }

  private Map<String, Resource> getResources(@NotNull final String projectId,
                                             @NotNull final Map<String, Map<String, Resource>> projectTreeResources) {
    return projectTreeResources.computeIfAbsent(projectId, myResources::getResourcesMap);
  }

  private Map<String, CustomResource> getCustomResources(@NotNull final String projectId,
                                                         @NotNull final Map<String, Resource> projectResources,
                                                         @NotNull final Map<String, Map<String, CustomResource>> projectTreeCustomResources) {
    return projectTreeCustomResources.computeIfAbsent(projectId,
                                                      id -> projectResources.values().stream()
                                                                            .filter(resource -> ResourceType.CUSTOM.equals(resource.getType()))
                                                                            .map(resource -> (CustomResource)resource)
                                                                            .collect(Collectors.toMap(Resource::getName, Function.identity())));
  }

  @NotNull
  private Map<Lock, String> initTakenValues(@NotNull final Collection<Lock> myLocks) {
    return myLocks.stream()
                  .collect(Collectors.toMap(Function.identity(), val -> ""));
  }
}
