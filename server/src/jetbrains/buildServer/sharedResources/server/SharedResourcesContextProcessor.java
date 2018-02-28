package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import java.util.*;
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


  @Override
  public void updateParameters(@NotNull final BuildStartContext context) {
    final SRunningBuild build = context.getBuild();
    // build can be part of build chain that contains shared resources.
    // walk the chain graph.
    // (R) <-- (R) <-- ... <-- (Q) <-- (Q)
    // any upper subpath can be running (when there are running builds in other subtrees
    // Move from top
    // for each running build in build chain get taken locks
    // for each queued build resolve locks and store
    // for current (current build id NOT composite -> assign locks as usual)
    //  corner cases:
    //  write locks in upper composite builds do not prevent taking read lock in sub builds

    final BuildPromotionEx promo = (BuildPromotionEx)build.getBuildPromotion();
      // several locks on same resource may be taken by the chain
    if (promo.isPartOfBuildChain()) {
      final List<BuildPromotionEx> depPromos = promo.getDependentCompositePromotions();
      for (BuildPromotionEx p: depPromos) {
        SBuild b = p.getAssociatedBuild();
        if (b instanceof RunningBuildEx) {
          // When context processor is called, current build as well as all composite builds are already running
          // Only thing that differentiates composite builds that were looked through and not is the result of locksStored method
          LOG.debug("Composite build " + build.getBuildId() + " is running");
          // build is running, locks are taken. print for now
          if (myLocksStorage.locksStored(b)) {
            // if this build contains any locks -> load
            myLocksStorage.load(b).forEach((k, v) -> LOG.debug("" + b.getBuildId() + ": " + k + "=" + v));
          } else {
            // store locks
            if (b.getBuildType() != null) {
              final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(b.getBuildType());
              final Map<String, Lock> locks = new HashMap<>();
              // get locks required for composite build
              for (SharedResourcesFeature f : features) {
                locks.putAll(f.getLockedResources());
              }
              // store locks with no values for now
              final Map<Lock, String> myTakenValues = initTakenValues(locks.values());
              myLocksStorage.store(b, myTakenValues);
              // save locks as acquired by the chain.
            }
          }
        }
      }
    }

    findAndProcessLocks(context, build); // <-- here we should add restrictions

  }

  private void findAndProcessLocks(final @NotNull BuildStartContext context, final SRunningBuild build) {
    if (build.getBuildType() != null) {
      final Collection<SharedResourcesFeature> features = myFeatures.searchForFeatures(build.getBuildType());
      if (!features.isEmpty()) {
        final Map<String, Lock> locks = new HashMap<>();
        for (SharedResourcesFeature f : features) {
          locks.putAll(f.getLockedResources());
        }
        if (!locks.isEmpty()) {
          processCustomLocks(context, build, locks);
        }
      }
    }
  }

  private void processCustomLocks(@NotNull final BuildStartContext context,
                                  @NotNull final SRunningBuild build,
                                  @NotNull final Map<String, Lock> locks) {
    if (build.getProjectId() != null) {
      processCustomLocks(context, build, build.getProjectId(), locks);
    }
  }

  private void processCustomLocks(@NotNull final BuildStartContext context,
                                  @NotNull final SRunningBuild build,
                                  @NotNull final String projectId,
                                  @NotNull final Map<String, Lock> locks) {
    final Map<Lock, String> myTakenValues = initTakenValues(locks.values());
    // get custom resources from our locks
    final Map<String, CustomResource> myCustomResources = getCustomResources(projectId, locks);
    synchronized (o) {
      // decide whether we need to resolve values
      if (!myCustomResources.isEmpty()) {
        final Map<String, List<String>> usedValues = collectTakenValuesFromRuntime(locks);
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
  private Map<String, List<String>> collectTakenValuesFromRuntime(@NotNull final Map<String, Lock> locks) {
    final List<SRunningBuild> runningBuilds = myRunningBuildsManager.getRunningBuilds();
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
    final Map<Lock, String> result = new HashMap<>();
    for (Lock lock: myLocks) {
      result.put(lock, "");
    }
    return result;
  }
}
