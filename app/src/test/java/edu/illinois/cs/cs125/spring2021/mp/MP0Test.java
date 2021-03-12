package edu.illinois.cs.cs125.spring2021.mp;

import static com.google.common.truth.Truth.assertThat;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import edu.illinois.cs.cs125.spring2021.mp.activities.MainActivity;
import edu.illinois.cs.cs125.spring2021.mp.network.Server;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/*
 * This is the MP0 test suite.
 * These are the same tests that we will run on your code during official grading.
 *
 * You do not need to necessarily understand all of the code below.
 * However, we have tried to write these tests in a way similar to how we would for a real Android project.
 *
 * This test suite is very simple, since it is only designed to determine whether you are set up properly
 * and ready for future checkpoints. However, we may have introduced a few checkstyle problems for you to fix.
 */

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
public final class MP0Test {

  /** Test that setup is complete. */
  @Test(timeout = 10000L)
  @Graded(points = 90)
  public void testActivityTitle() throws InterruptedException {
    // Create a new activity scenario and start a MainActivity
    ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
    scenario.moveToState(Lifecycle.State.CREATED);
    scenario.moveToState(Lifecycle.State.RESUMED);

    // Check the backend to make sure its responding to requests correctly
    assertThat(Server.isRunning(true)).isTrue();

    scenario.onActivity(
        activity -> {
          // Check activity title after startup
          assertThat(activity.getTitle()).isEqualTo("Search Courses");
        });
  }
}
