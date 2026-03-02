

package jetbrains.buildServer.sharedResources.server.runtime;

import com.google.common.cache.Cache;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
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
    myLocksStorage = new CDSBasedTakenLocksStorage(myFixture.getProjectManager(), myFixture.getBuildPromotionManager(), myFixture.getEventDispatcher());
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