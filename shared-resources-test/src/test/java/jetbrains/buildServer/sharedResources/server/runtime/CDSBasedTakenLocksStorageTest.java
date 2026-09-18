

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SQueuedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.TestFor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;

@TestFor (testForClass = {LocksStorage.class, CDSBasedTakenLocksStorage.class})
public class CDSBasedTakenLocksStorageTest extends BaseServerTestCase {
  private LocksStorage myLocksStorage;

  private EventDispatcher<BuildServerListener> myDispatcher;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myLocksStorage = new CDSBasedTakenLocksStorage(myFixture.getProjectManager(),
                                                   myFixture.getBuildPromotionManager(),
                                                   myFixture.getBuildsManager(),
                                                   myFixture.getServerResponsibility(),
                                                   myFixture.getEventDispatcher());
  }

  @Test
  public void deserialize_no_values() {
    Map<String, Lock> deserialized = CDSBasedTakenLocksStorage.deserializeTakenLocks("lock1\treadLock\t \nlock2\twriteLock\t \nlock3\treadLock\t \nlock4\twriteLock\t \n");
    then(deserialized).hasSize(4);
    then(deserialized.get("lock1").getType()).isEqualTo(LockType.READ);
    then(deserialized.get("lock1").getValue()).isEmpty();
    then(deserialized.get("lock3").getType()).isEqualTo(LockType.READ);
    then(deserialized.get("lock3").getValue()).isEmpty();
    then(deserialized.get("lock2").getType()).isEqualTo(LockType.WRITE);
    then(deserialized.get("lock2").getValue()).isEmpty();
    then(deserialized.get("lock4").getType()).isEqualTo(LockType.WRITE);
    then(deserialized.get("lock4").getValue()).isEmpty();
  }

  @Test
  public void deserialize_with_values() {
    Map<String, Lock> deserialized = CDSBasedTakenLocksStorage.deserializeTakenLocks("lock1\treadLock\tMy Value 1\nlock2\twriteLock\tMy Value 2\n");
    then(deserialized).hasSize(2);
    then(deserialized.get("lock1").getType()).isEqualTo(LockType.READ);
    then(deserialized.get("lock1").getValue()).isEqualTo("My Value 1");
    then(deserialized.get("lock2").getType()).isEqualTo(LockType.WRITE);
    then(deserialized.get("lock2").getValue()).isEqualTo("My Value 2");
  }

  @Test
  public void deserialize_mixed() {
    Map<String, Lock> deserialized = CDSBasedTakenLocksStorage.deserializeTakenLocks("lock1\treadLock\tMy Value 1\nlock2\twriteLock\tMy Value 2\nlock3\twriteLock\t ");
    then(deserialized).hasSize(3);
    then(deserialized.get("lock1").getType()).isEqualTo(LockType.READ);
    then(deserialized.get("lock1").getValue()).isEqualTo("My Value 1");
    then(deserialized.get("lock2").getType()).isEqualTo(LockType.WRITE);
    then(deserialized.get("lock2").getValue()).isEqualTo("My Value 2");
    then(deserialized.get("lock3").getType()).isEqualTo(LockType.WRITE);
    then(deserialized.get("lock3").getValue()).isEmpty();
  }

  @Test
  public void deserialize_incorrect_values() {
    Map<String, Lock> deserialized = CDSBasedTakenLocksStorage.deserializeTakenLocks("lock1\treadLock\t \nHELLO!\n");
    then(deserialized).hasSize(1);
    then(deserialized.get("lock1").getType()).isEqualTo(LockType.READ);
  }

  @Test
  public void empty_locks_map() {
    String serialized = CDSBasedTakenLocksStorage.serializeTakenLocks(Collections.emptyMap());
    Map<String, Lock> deserialized = CDSBasedTakenLocksStorage.deserializeTakenLocks(serialized);
    then(deserialized).isEmpty();
  }

  @Test
  public void testStore_NoValues() {
    SRunningBuild build = myFixture.startBuild();

    final Map<Lock, String> takenLocks = new HashMap<>();
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);

    takenLocks.put(lock1, "");
    takenLocks.put(lock2, "");

    myLocksStorage.store(build.getBuildPromotion(), takenLocks);

    final Map<String, Lock> result = myLocksStorage.load(build.getBuildPromotion());
    then(result).isNotNull();
    then(result).hasSize(2);
    then(result.values()).contains(lock1, lock2);
    then(result.get(lock1.getName()).getValue()).isEmpty();
    then(result.get(lock2.getName()).getValue()).isEmpty();
  }

  @Test
  public void testStore_Values() {
    SRunningBuild build = myFixture.startBuild();

    final Map<Lock, String> takenLocks = new HashMap<>();
    final String value1 = "_value_1_";
    final String value2 = "_value_2_";
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);
    takenLocks.put(lock1, value1);
    takenLocks.put(lock2, value2);
    // values are in cache. No file access needed

    myLocksStorage.store(build.getBuildPromotion(), takenLocks);
    final Map<String, Lock> result = myLocksStorage.load(build.getBuildPromotion());
    then(result).isNotNull();
    then(result).hasSize(2);
    then(result.get(lock1.getName()).getValue()).isEqualTo(value1);
    then(result.get(lock2.getName()).getValue()).isEqualTo(value2);
  }

  @Test
  public void testStore_Mixed() {
    SRunningBuild build = myFixture.startBuild();

    final Map<Lock, String> takenLocks = new HashMap<>();
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);
    final Lock lock11 = new Lock("lock11", LockType.READ);

    final String value = "_MY_VALUE_";
    takenLocks.put(lock1, "");
    takenLocks.put(lock11, value);
    takenLocks.put(lock2, "");
    myLocksStorage.store(build.getBuildPromotion(), takenLocks);
    final Map<String, Lock> result = myLocksStorage.load(build.getBuildPromotion());
    then(result).isNotNull().hasSize(3);
    then(result.get(lock1.getName()).getValue()).isEmpty();
    then(result.get(lock2.getName()).getValue()).isEmpty();
    then(result.get(lock11.getName()).getValue()).isEqualTo(value);
  }

  @Test
  public void testGetAllTakenLocks_RunningBuild() {
    SRunningBuild build = myFixture.startBuild();
    final Lock lock = new Lock("lock1", LockType.READ);
    myLocksStorage.store(build.getBuildPromotion(), Collections.singletonMap(lock, "_value_1_"));

    final Map<BuildPromotion, Map<String, Lock>> allTakenLocks = myLocksStorage.getAllTakenLocks();
    then(allTakenLocks).containsOnlyKeys(build.getBuildPromotion());
    then(allTakenLocks.get(build.getBuildPromotion())).containsOnlyKeys(lock.getName());
    then(allTakenLocks.get(build.getBuildPromotion()).get(lock.getName()).getValue()).isEqualTo("_value_1_");
  }

  @Test
  public void testGetAllTakenLocks_BuildNotStartedYet() {
    final SQueuedBuild queuedBuild = myBuildType.addToQueue("");
    then(queuedBuild).isNotNull();
    final BuildPromotion promotion = queuedBuild.getBuildPromotion();
    final Lock lock = new Lock("lock1", LockType.READ);
    myLocksStorage.store(promotion, Collections.singletonMap(lock, ""));

    then(myLocksStorage.getAllTakenLocks()).containsOnlyKeys(promotion);
    then(myLocksStorage.locksStored(promotion)).isTrue();
  }

  @Test
  public void testGetAllTakenLocks_BuildRemovedFromQueue() {
    final SQueuedBuild queuedBuild = myBuildType.addToQueue("");
    then(queuedBuild).isNotNull();
    final BuildPromotion promotion = queuedBuild.getBuildPromotion();
    myLocksStorage.store(promotion, Collections.singletonMap(new Lock("lock1", LockType.READ), ""));

    myFixture.getBuildQueue().removeItems(Collections.singleton(queuedBuild.getItemId()), null, null);

    then(myLocksStorage.getAllTakenLocks()).isEmpty();
    then(myLocksStorage.locksStored(promotion)).isFalse();
  }

  @Test
  @TestFor(issues = "TW-44474")
  public void testBuildFinished_CacheCleaned() {
    SRunningBuild build = myFixture.startBuild();
    then(myLocksStorage.locksStored(build.getBuildPromotion())).isFalse();

    final Map<Lock, String> takenLocks = new HashMap<>();
    takenLocks.put(new Lock("lock1", LockType.READ), "");
    myLocksStorage.store(build.getBuildPromotion(), takenLocks);
    then(myLocksStorage.locksStored(build.getBuildPromotion())).isTrue();

    finishBuild();

    then(myLocksStorage.locksStored(build.getBuildPromotion())).isFalse();
  }
}