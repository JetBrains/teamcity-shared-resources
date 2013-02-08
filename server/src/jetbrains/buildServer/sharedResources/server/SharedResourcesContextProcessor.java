package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class SharedResourcesContextProcessor implements BuildStartContextProcessor {

  private final Object lock = new Object();

  private static final Logger log = Logger.getInstance(BuildStartContextProcessor.class.getName());

  // where is it called?
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
    final SBuildType myType = build.getBuildType();
    final String projectId = build.getProjectId();
    if (myType != null && projectId != null) {
      if (myFeatures.featuresPresent(myType)) {
        log.info("SRCP :>> features present");
        final Map<String, Lock> locks = myLocks.fromBuildParametersAsMap(
                ((BuildPromotionEx)build.getBuildPromotion()).getParametersProvider().getAll());

        if (!locks.isEmpty()) {
          log.info("SRCP :>> found some locks");

          // decide whether we need to resolve values
          final Map<String, Resource> myCustomResources = new HashMap<String, Resource>();
          final Map<String, Resource> resourceMap = myResources.asMap(projectId);
          final Map<Lock, String> myTakenValues = new HashMap<Lock, String>();
          for (Map.Entry<String, Lock> entry: locks.entrySet()) {
            myTakenValues.put(entry.getValue(), ""); // empty values for unresolved locks and locks without values
            final Resource r = resourceMap.get(entry.getKey());
            if (ResourceType.CUSTOM.equals(r.getType())) {
              log.info("SRCP :>> custom resource found! [" + r.getName() + "]");
              myCustomResources.put(r.getName(), r);
            }
          }

          synchronized (lock) {

            if (!myCustomResources.isEmpty()) { // need to resolve values
              log.info("SRCP :>> my build contains custom resources. must provide value for param");
              // get all builds from runtime.
              final List<SRunningBuild> runningBuilds = myRunningBuildsManager.getRunningBuilds();
              final Map<String, Set<String>> usedValues = new HashMap<String, Set<String>>();
              for (String name: locks.keySet()) {
                usedValues.put(name, new HashSet<String>());
              }
              // form Map<String, Set<String>>
              for (SRunningBuild runningBuild: runningBuilds) {
                Map<Lock, String> locksInRunningBuild = myLocksStorage.load(runningBuild);
                for (Lock l: locks.values()) {
                  String value = locksInRunningBuild.get(l);
                  if (value != null && !"".equals(value)) { // todo string comparison ?? length?
                    usedValues.get(l.getName()).add(value);
                  }
                }
              }
              // for each custom lock taken
              for (Map.Entry<String, Resource> entry: myCustomResources.entrySet()) {
                // get value space for current resources
                final Set<String> values = new HashSet<String>(((CustomResource) entry.getValue()).getValues());
                values.removeAll(usedValues.get(entry.getKey()));
                if (!values.isEmpty()) { // we have values that we can assign
                  final String paramName = "teamcity.locks.readLock." + entry.getKey(); // todo: lock as param name
                  String valueToTake = values.iterator().next();
                  context.addSharedParameter(paramName, valueToTake);
                  myTakenValues.put(locks.get(entry.getKey()), valueToTake);
                } else {
                  log.warn("Unable to assign value lo lock [" + entry.getKey() + "]");
                }
              }
            }
            myLocksStorage.store(build, myTakenValues);
          }
        } else {
          log.info("SRCP :>> no locks found");
        }
      } else {
        log.info("SRCP :>> no features present");
      }
    }
    // collect locks that are taken
    // determine value of lock to pass
  }
}
