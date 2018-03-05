package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.*;
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
    final Set<Long> runningCompositeIds = new HashSet<>();
    // several locks on same resource may be taken by the chain
    if (startingBuildPromotion.isPartOfBuildChain()) {
      // get all dependent composite promotions
      final List<BuildPromotionEx> depPromos = startingBuildPromotion.getDependentCompositePromotions();
      // At this moment ALL composite builds are already in RunningBuildsManager
      depPromos.stream()
               .filter(p -> p.getAssociatedBuild() instanceof SRunningBuild)
               .map(BuildPromotionEx::getAssociatedBuildId)
               .filter(Objects::nonNull)
               .forEach(runningCompositeIds::add);
      // When context processor is called, current build as well as all composite builds are already running
      // Only thing that differentiates composite builds that were looked through and not is the result of locksStored method
      depPromos.stream()
               .map(BuildPromotionEx::getAssociatedBuild)
               .filter(build -> build instanceof RunningBuildEx)
               .filter(build -> !myLocksStorage.locksStored(build))
               .forEach(build -> processCompositeBuild(build, runningCompositeIds));
    }
    //todo: SPECIFIC (ex. A) -> ANY -> ANY -> ANY ===> A -> A -> A -> A
    findAndProcessLocks(context, startingBuild, runningCompositeIds); // <-- here we should add restrictions
  }
  private void processCompositeBuild(@NotNull final SBuild build,
                                     @NotNull final Set<Long> compositeRunningBuildIds) {
    // load locks
    // load resources
    if (build.getBuildType() != null && build.getProjectId() != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(build.getBuildType());
      if (!features.isEmpty()) {
        final Map<String, Lock> locks = new HashMap<>();
        for (SharedResourcesFeature f : features) {
          locks.putAll(f.getLockedResources());
        }
        if (!locks.isEmpty()) {
          final Map<Lock, String> myTakenValues = initTakenValues(locks.values());
          // get custom resources from our locks
          final Map<String, CustomResource> myCustomResources = getCustomResources(build.getProjectId(), locks);
          synchronized (o) {
            // decide whether we need to resolve values
            if (!myCustomResources.isEmpty()) {
              // used values should not include the values from composite chain
              final Map<String, List<String>> usedValues = collectTakenValuesFromRuntime(locks, compositeRunningBuildIds);
              for (Map.Entry<String, CustomResource> entry: myCustomResources.entrySet()) {
                if (entry.getValue().isEnabled()) {
                  // get value space for current resources
                  final List<String> values = new ArrayList<>(entry.getValue().getValues());
                  final String key = entry.getKey();
                  // remove used values
                  usedValues.get(key).forEach(values::remove);
                  if (!values.isEmpty()) {
                    final Lock currentLock = locks.get(key);
                    String currentValue;
                    if (LockType.READ.equals(currentLock.getType())) {
                      currentValue = currentLock.getValue().equals("") ? values.iterator().next() : currentLock.getValue();
                      // todo: add support of multiple values per lock in custom storage
                      myTakenValues.put(currentLock, currentValue);
                    }
                  } else {
                    // throw exception?
                    LOG.warn("Unable to assign value to lock [" + key + "] for build with id [" + build.getBuildId() + "]");
                  }
                }
              }
            }
            myLocksStorage.store(build, myTakenValues);
          }
        }
      }
    }
  }

  private void findAndProcessLocks(@NotNull final BuildStartContext context,
                                   @NotNull final SRunningBuild build,
                                   @NotNull final Set<Long> compositeRunningBuildIds) {
    if (build.getBuildType() != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(build.getBuildType());
      if (!features.isEmpty()) {
        final Map<String, Lock> locks = new HashMap<>();
        for (SharedResourcesFeature f : features) {
          locks.putAll(f.getLockedResources());
        }
        if (!locks.isEmpty()) {
          processCustomLocks(context, build, locks, compositeRunningBuildIds);
        }
      }
    }
  }

  private void processCustomLocks(@NotNull final BuildStartContext context,
                                  @NotNull final SRunningBuild build,
                                  @NotNull final Map<String, Lock> locks,
                                  @NotNull final Set<Long> compositeRunningBuildIds) {
    if (build.getProjectId() != null) {
      processCustomLocks(context, build, build.getProjectId(), locks, compositeRunningBuildIds);
    }
  }

  private void processCustomLocks(@NotNull final BuildStartContext context,
                                  @NotNull final SRunningBuild build,
                                  @NotNull final String projectId,
                                  @NotNull final Map<String, Lock> locks,
                                  @NotNull final Set<Long> compositeRunningBuildIds) {
    final Map<Lock, String> myTakenValues = initTakenValues(locks.values());
    // get custom resources from our locks
    final Map<String, CustomResource> myCustomResources = getCustomResources(projectId, locks);
    synchronized (o) {
      // decide whether we need to resolve values
      if (!myCustomResources.isEmpty()) {
        // used values should not include the values from composite chain
        final Map<String, List<String>> usedValues = collectTakenValuesFromRuntime(locks, compositeRunningBuildIds);
        for (Map.Entry<String, CustomResource> entry: myCustomResources.entrySet()) {
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
              context.addSharedParameter(paramName, currentValue);
            } else {
              // throw exception?
              LOG.warn("Unable to assign value to lock [" + key + "] for build with id [" + build.getBuildId() + "]");
            }
          }
        }
      }
      myLocksStorage.store(build, myTakenValues);
    }
  }

  /**
   * Collects acquired values for all locks from runtime
   *
   * @param locks locks required by current build
   * @return map of locks and taken values
   */
  @NotNull
  private Map<String, List<String>> collectTakenValuesFromRuntime(@NotNull final Map<String, Lock> locks, Set<Long> runningCompositeBuilds) {
    // ignore locks taken in chain
    final List<SRunningBuild> runningBuilds = myRunningBuildsManager.getRunningBuilds()
                                                                    .stream()
                                                                    .filter(b -> !runningCompositeBuilds.contains(b.getBuildId()))
                                                                    .collect(Collectors.toList());
    final Map<String, List<String>> usedValues = new HashMap<>();
    for (String name: locks.keySet()) {
      usedValues.put(name, new ArrayList<>());
    }
    // collect taken values from runtime
    for (SRunningBuild runningBuild: runningBuilds) {
      Map<String, Lock> locksInRunningBuild = myLocksStorage.load(runningBuild);
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

  /**
   * Determines, what locks are acquired on custom resources
   *
   * @param projectId id of current project
   * @param locks locks required
   * @return collection of resources among required locks that are custom
   */
  @NotNull
  private Map<String, CustomResource> getCustomResources(@NotNull final String projectId,
                                                         @NotNull final Map<String, Lock> locks) {
    final Map<String, CustomResource> myCustomResources = new HashMap<>();
    final Map<String, Resource> resourceMap = myResources.getResourcesMap(projectId);
    for (Map.Entry<String, Lock> entry: locks.entrySet()) {
      final Resource r = resourceMap.get(entry.getKey());
      if (ResourceType.CUSTOM.equals(r.getType())) {
        myCustomResources.put(r.getName(), (CustomResource)r);
      }
    }
    return myCustomResources;
  }

  @NotNull
  private Map<Lock, String> initTakenValues(@NotNull final Collection<Lock> myLocks) {
    return myLocks.stream()
                  .collect(Collectors.toMap(Function.identity(), val -> ""));
  }
}
