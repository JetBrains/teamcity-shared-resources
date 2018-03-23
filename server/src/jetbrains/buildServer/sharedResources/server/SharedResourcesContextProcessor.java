package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import gnu.trove.TLongHashSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

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
  private final RunningBuildsManager myRunningBuildsManager;

  public SharedResourcesContextProcessor(@NotNull final SharedResourcesFeatures features,
                                         @NotNull final Locks locks,
                                         @NotNull final Resources resources,
                                         @NotNull final LocksStorage locksStorage,
                                         @NotNull final RunningBuildsManager runningBuildsManager) {
    myFeatures = features;
    myLocks = locks;
    myResources = resources;
    myLocksStorage = locksStorage;
    myRunningBuildsManager = runningBuildsManager;
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
    final Map<String, Map<String, CustomResource>> projectTreeCustomResources = new HashMap<>();
    final AtomicReference<List<SRunningBuild>> runningBuilds = new AtomicReference<>();
    // several locks on same resource may be taken by the chain
    synchronized (o) {
      if (TeamCityProperties.getBoolean(SharedResourcesPluginConstants.RESOURCES_IN_CHAINS_ENABLED) && startingBuildPromotion.isPartOfBuildChain()) {
        // get all dependent composite promotions
        final List<BuildPromotionEx> depPromos = startingBuildPromotion.getDependentCompositePromotions();
        // collect promotion ids of composite builds in chain.
        // we don't need to check the values against them
        depPromos.forEach(promo -> compositeIds.add(promo.getId()));
        // some build promotions in composite chain may not have the locks stored -> we need to process and store locks
        depPromos.stream()
                 .filter(promo -> !myLocksStorage.locksStored(promo))
                 .forEach(promo -> processBuild(context, promo, compositeIds, projectTreeCustomResources, runningBuilds));
      }
      processBuild(context, startingBuild.getBuildPromotion(), compositeIds, projectTreeCustomResources, runningBuilds);
    }
  }

  private void processBuild(@NotNull final BuildStartContext context,
                            @NotNull final BuildPromotion currentBuildPromotion,
                            @NotNull final TLongHashSet compositeRunningBuildIds,
                            @NotNull final Map<String, Map<String, CustomResource>> projectTreeCustomResources,
                            @NotNull final AtomicReference<List<SRunningBuild>> runningBuilds) {
    if (currentBuildPromotion.getBuildType() == null || currentBuildPromotion.getProjectId() == null) {
      return;
    }
    final Map<String, Lock> locks = extractLocks(currentBuildPromotion);
    final Map<Lock, String> myTakenValues = initTakenValues(locks.values());
    // get custom resources from our locks

    // FIXME: disable custom resources processing for composite builds until method of consistent delivery of values to the chain is implemented
    // from UI: user should not be able to add a lock on custom resource in composite build => initTakenValues will not contain any custom resources
    // locks on regular resources will be saved
    if (!currentBuildPromotion.isCompositeBuild()) {
      final Map<String, CustomResource> myCustomResources = matchCustomResources(getCustomResources(currentBuildPromotion.getProjectId(), projectTreeCustomResources), locks);
      // decide whether we need to resolve values
      if (!myCustomResources.isEmpty()) {
        // used values should not include the values from composite chain
        final Map<String, List<String>> usedValues = collectTakenValuesFromRuntime(locks, compositeRunningBuildIds, runningBuilds);
        for (Map.Entry<String, CustomResource> entry : myCustomResources.entrySet()) {
          if (entry.getValue().isEnabled()) {
            // get value space for current resources
            final List<String> values = new ArrayList<>(entry.getValue().getValues());
            final String key = entry.getKey();
            // remove used values
            usedValues.get(key).forEach(values::remove);
            if (!values.isEmpty()) {
              final Lock currentLock = locks.get(key);
              final String paramName = myLocks.asBuildParameter(currentLock);
              String currentValue;
              if (LockType.READ.equals(currentLock.getType())) {
                currentValue = currentLock.getValue().equals("") ? values.iterator().next() : currentLock.getValue();
                // todo: add support of multiple values per lock in custom storage
                myTakenValues.put(currentLock, currentValue);
              } else {
                currentValue = StringUtil.join(values, ";");
              }
              if (!currentBuildPromotion.isCompositeBuild()) {
                context.addSharedParameter(paramName, currentValue);
              }
            } else {
              // throw exception?
              LOG.warn("Unable to assign value to lock [" + key + "] for build promotion with id [" + currentBuildPromotion.getId() + "]");
            }
          }
        }
      }
    }
    myLocksStorage.store(currentBuildPromotion, myTakenValues);
  }

  private Map<String, Lock> extractLocks(@NotNull final BuildPromotion buildPromotion) {
    final Map<String, Lock> result = new HashMap<>();
    if (buildPromotion.getBuildType() != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(buildPromotion.getBuildType());
      features.stream()
              .map(SharedResourcesFeature::getLockedResources)
              .forEach(result::putAll);
    }
    return result;
  }

  /**
   * Collects acquired values for all locks from runtime
   *
   * @param locks locks required by current build
   * @param runningBuilds reference to running builds collection
   * @return map of locks and taken values
   */
  @NotNull
  private Map<String, List<String>> collectTakenValuesFromRuntime(@NotNull final Map<String, Lock> locks,
                                                                  @NotNull final TLongHashSet compositePromotionIds,
                                                                  @NotNull final AtomicReference<List<SRunningBuild>> runningBuilds) {
    if (runningBuilds.get() == null) {
      runningBuilds.set(myRunningBuildsManager.getRunningBuilds()
                                              .stream()
                                              .filter(b -> !compositePromotionIds.contains(b.getBuildPromotion().getId()))
                                              .collect(Collectors.toList()));
    }

    final Map<String, List<String>> usedValues = new HashMap<>();
    locks.forEach((k, v) -> usedValues.put(k, new ArrayList<>()));
    // collect taken values from runtime
    for (SRunningBuild runningBuild: runningBuilds.get()) {
      Map<String, Lock> locksInRunningBuild = myLocksStorage.load(runningBuild.getBuildPromotion());
      for (Lock l: locks.values()) {
        Lock runningLock = locksInRunningBuild.get(l.getName());
        if (runningLock != null) {
          String value = runningLock.getValue();
          if (!"".equals(value)) {
            usedValues.get(l.getName()).add(value);
          }
        }
      }
    }
    return usedValues;
  }

  private Map<String, CustomResource> matchCustomResources(@NotNull final Map<String, CustomResource> resources,
                                                           @NotNull final Map<String, Lock> locks) {
    final Map<String, CustomResource> result = new HashMap<>();
    for (Map.Entry<String, Lock> entry: locks.entrySet()) {
      final Resource r = resources.get(entry.getKey());
      if (r != null) {
        result.put(r.getName(), (CustomResource)r);
      }
    }
    return result;
  }

  private Map<String, CustomResource> getCustomResources(@NotNull final String projectId,
                                                         @NotNull final Map<String, Map<String, CustomResource>> projectTreeCustomResources) {
    return projectTreeCustomResources.computeIfAbsent(projectId, this::doGetCustomResources);
  }

  private Map<String, CustomResource> doGetCustomResources(@NotNull final String projectId) {
    return myResources.getResources(projectId)
                      .stream()
                      .filter(resource -> ResourceType.CUSTOM.equals(resource.getType()))
                      .map(resource -> (CustomResource)resource)
                      .collect(Collectors.toMap(Resource::getName, Function.identity()));
  }

  @NotNull
  private Map<Lock, String> initTakenValues(@NotNull final Collection<Lock> myLocks) {
    return myLocks.stream()
                  .collect(Collectors.toMap(Function.identity(), val -> ""));
  }
}
