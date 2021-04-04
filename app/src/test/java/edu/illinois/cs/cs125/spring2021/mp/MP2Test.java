package edu.illinois.cs.cs125.spring2021.mp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.google.common.truth.Truth.assertThat;
import static edu.illinois.cs.cs125.spring2021.mp.RecyclerViewMatcher.withRecyclerView;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
//import edu.illinois.cs.cs125.spring2021.mp.activities.CourseActivity;
import edu.illinois.cs.cs125.spring2021.mp.activities.MainActivity;
import edu.illinois.cs.cs125.spring2021.mp.application.CourseableApplication;
import edu.illinois.cs.cs125.spring2021.mp.models.Course;
import edu.illinois.cs.cs125.spring2021.mp.models.Summary;
import edu.illinois.cs.cs125.spring2021.mp.network.Client;
import edu.illinois.cs.cs125.spring2021.mp.network.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.apache.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/*
 * This is the MP2 test suite.
 * These are the same tests that we will run on your code during official grading.
 *
 * You do not need to necessarily understand all of the code below.
 * However, we have tried to write these tests in a way similar to how we would for a real Android project.
 *
 * The MP2 unit tests test that your Course model and server route work properly.
 * The MP2 integration tests test your network `Client`, your `CourseActivity`, and confirm that the course detail
 * view launches properly from the MainActivity.
 *
 * As in MP1 we strongly suggest that you work through these test in order.
 * However, unlike MP1 you will need to create some stub methods initially before anything will work.
 * The MP2 writeup will walk you through this process.
 * Once your code is compiling, next:
 *
 * 1. Create your `Course.java` model and pass `testCourseClass`
 * 2. Modify `Server.java` to add the course detail route and pass `testServerCourseRoute`
 * 3. Improve `Client.java` so that it passes `testClientGetCourse`
 * 4. Complete `CourseActivity` so that you pass `testCourseView`
 * 5. Finally, return to `MainActivity` and add the onClick handler, although you may want to complete the last
 * two steps in the opposite order
 *
 * You may modify these tests if it helps you during your development.
 * For example, you may want to add test cases, or improve the error messages.
 * However, your changes will be discarded during official grading.
 * So please be careful, since this can be a point of confusion for students when your local grade does not match up
 * with your official grade.
 */
@RunWith(Enclosed.class)
public final class MP2Test {
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final List<String> summaries = new ArrayList<>();
  private static final List<String> courses = new ArrayList<>();

  @BeforeClass
  public static void setup() throws IOException {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Load courses JSON and create summaries
    String coursesJson =
        new Scanner(MP2Test.class.getResourceAsStream("/2021_spring.json"), "UTF-8")
            .useDelimiter("\\A")
            .next();
    JsonNode coursesNodes = mapper.readTree(coursesJson);
    for (Iterator<JsonNode> it = coursesNodes.elements(); it.hasNext(); ) {
      JsonNode course = it.next();
      courses.add(course.toPrettyString());

      ObjectMapper objectMapper = new ObjectMapper();
      ObjectNode summary = objectMapper.createObjectNode();
      summary.set("year", course.get("year"));
      summary.set("semester", course.get("semester"));
      summary.set("department", course.get("department"));
      summary.set("number", course.get("number"));
      summary.set("title", course.get("title"));
      summaries.add(summary.toPrettyString());
    }
  }

  public static class UnitTests {
    @BeforeClass
    public static void setup() throws IOException {
      MP2Test.setup();
    }

    /**
     * Retest summary filtering (Summary.filter).
     */
    @Test(timeout = 1000L)
    @Graded(points = 10)
    public void testSummaryFilter() {
      Summary BADM100 = new Summary("2020", "Fall", "BADM", "100", "Introduction to Badminton");
      Summary BADM200 = new Summary("2020", "Fall", "BADM", "200", "Intermediate Badminton");
      Summary CS125 = new Summary("2020", "Fall", "CS", "125", "Introduction to Computer Science");
      Summary CS125Spring =
          new Summary("2021", "Spring", "CS", "125", "Introduction to Computer Science");
      Summary CS225 = new Summary("2020", "Fall", "CS", "225", "Data Structures and Algorithms");
      Summary CS498VR = new Summary("2020", "Fall", "CS", "498", "Virtual Reality");
      Summary CS498CB = new Summary("2020", "Fall", "CS", "498", "Computational Badminton");

      assertThat(Summary.filter(Collections.emptyList(), "test")).hasSize(0);
      assertThat(Summary.filter(Collections.singletonList(BADM100), "intro")).hasSize(1);
      assertThat(Summary.filter(Collections.singletonList(BADM100), "intro")).contains(BADM100);
      assertThat(Summary.filter(Collections.singletonList(BADM100), "xyz")).hasSize(0);
      assertThat(Summary.filter(Arrays.asList(CS125, CS125Spring), "125")).hasSize(2);
      assertThat(Summary.filter(Arrays.asList(CS125, CS125Spring), "125")).contains(CS125);
      assertThat(Summary.filter(Arrays.asList(CS125, CS125Spring), "125")).contains(CS125Spring);
      assertThat(Summary.filter(Arrays.asList(CS125, CS125Spring), "Terrible")).hasSize(0);
      assertThat(Summary.filter(Arrays.asList(CS125, CS125Spring, CS225), "25")).hasSize(3);
      assertThat(
          Summary.filter(
              Arrays.asList(BADM100, CS125, CS125Spring, CS225, CS498VR, CS498CB), "Badminton"))
          .hasSize(2);
    }

    /**
     * Test the Course class.
     */
    @Test(timeout = 1000L)
    @Graded(points = 10)
    public void testCourseClass() throws JsonProcessingException {
      for (String courseString : courses) {
        Course course = mapper.readValue(courseString, Course.class);
        compareCourseToSerializedCourse(course, courseString);
      }
    }

    /**
     * Test the course server route.
     */
//    @Test(timeout = 10000L)
//    @Graded(points = 15)
//    public void testServerCourseRoute() throws IOException {
//      Server.start();
//      // Check the backend to make sure its responding to requests correctly
//      assertThat(Server.isRunning(false)).isTrue();
//
//      OkHttpClient client = new OkHttpClient();
//
//      for (String courseString : courses) {
//        ObjectNode node = (ObjectNode) mapper.readTree(courseString);
//        String url =
//            CourseableApplication.SERVER_URL
//                + "course/"
//                + node.get("year").asText()
//                + "/"
//                + node.get("semester").asText()
//                + "/"
//                + node.get("department").asText()
//                + "/"
//                + node.get("number").asText();
//        Request courseRequest = new Request.Builder().url(url).build();
//        Response courseResponse = client.newCall(courseRequest).execute();
//        assertThat(courseResponse.code()).isEqualTo(HttpStatus.SC_OK);
//        ResponseBody body = courseResponse.body();
//        assertThat(body).isNotNull();
//        Course course = mapper.readValue(body.string(), Course.class);
//        compareCourseToSerializedCourse(course, courseString);
//      }
//
//      // Test some bad requests
//      // Bad course
//      Request request =
//          new Request.Builder()
//              .url(CourseableApplication.SERVER_URL + "course/2020/spring/CS/188/")
//              .build();
//      Response response = client.newCall(request).execute();
//      assertThat(response.code()).isEqualTo(HttpStatus.SC_NOT_FOUND);
//
//      // Bad URL
//      request =
//          new Request.Builder()
//              .url(CourseableApplication.SERVER_URL + "courses/2021/spring/CS/125/")
//              .build();
//      response = client.newCall(request).execute();
//      assertThat(response.code()).isEqualTo(HttpStatus.SC_NOT_FOUND);
//    }
  }

//  @SuppressWarnings("SameParameterValue")
//  @RunWith(AndroidJUnit4.class)
//  @LooperMode(LooperMode.Mode.PAUSED)
//  public static class IntegrationTests {
//    @BeforeClass
//    public static void setup() throws IOException {
//      MP2Test.setup();
//    }
//
//    /**
//     * Test the client getCourse method
//     */
//    @Test(timeout = 20000L)
//    @Graded(points = 15)
//    public void testClientGetCourse()
//        throws JsonProcessingException, InterruptedException, ExecutionException {
//      Client client = Client.start();
//
//      for (String summaryString : summaries) {
//        Summary summary = mapper.readValue(summaryString, Summary.class);
//        CompletableFuture<Course> completableFuture = new CompletableFuture<>();
//        client.getCourse(
//            summary,
//            new Client.CourseClientCallbacks() {
//              @Override
//              public void courseResponse(Summary summary, Course course) {
//                completableFuture.complete(course);
//              }
//            });
//        Course course = completableFuture.get();
//        compareCourseToSerializedSummary(course, summaryString);
//      }
//    }
//
//    /**
//     * Test CourseActivity with intent.
//     */
//    @Test(timeout = 10000L)
//    @Graded(points = 20)
//    public void testCourseView() throws JsonProcessingException, InterruptedException {
//      for (int i = 0; i < 4; i++) {
//        String summaryString = summaries.get(i);
//        String courseString = courses.get(i);
//        Intent intent =
//            new Intent(ApplicationProvider.getApplicationContext(), CourseActivity.class);
//        intent.putExtra("COURSE", summaryString);
//        ActivityScenario<CourseActivity> courseScenario = ActivityScenario.launch(intent);
//        courseScenario.moveToState(Lifecycle.State.CREATED);
//        courseScenario.moveToState(Lifecycle.State.RESUMED);
//        ObjectNode course = (ObjectNode) mapper.readTree(courseString);
//        Thread.sleep(100);
//        onView(ViewMatchers.withText(course.get("description").asText()))
//            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
//      }
//    }
//
//    /**
//     * Test onClick CourseActivity launch from MainActivity
//     */
//    @Test(timeout = 10000L)
//    @Graded(points = 10)
//    public void testOnClickLaunch() {
//      // Launch the main activity and confirm correct transition to CourseActivity
//      ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
//      scenario.moveToState(Lifecycle.State.CREATED);
//      scenario.moveToState(Lifecycle.State.RESUMED);
//
//      scenario.onActivity(
//          activity -> {
//            // Sanity checks
//            onView(withId(R.id.recycler_view)).check(countRecyclerView(73));
//            onView(withRecyclerView(R.id.recycler_view).atPosition(0))
//                .check(matches(hasDescendant(withText("CS 101: Intro Computing: Engrg & Sci"))));
//            onView(withRecyclerView(R.id.recycler_view).atPosition(0)).perform(click());
//            Intent started = shadowOf(activity).getNextStartedActivity();
//            String courseExtra = started.getStringExtra("COURSE");
//            try {
//              ObjectNode node = (ObjectNode) mapper.readTree(courseExtra);
//              assertThat(node.get("year").asText()).isEqualTo("2021");
//              assertThat(node.get("semester").asText()).isEqualTo("spring");
//              assertThat(node.get("department").asText()).isEqualTo("CS");
//              assertThat(node.get("number").asText()).isEqualTo("101");
//            } catch (JsonProcessingException e) {
//              throw new IllegalStateException(e.getMessage());
//            }
//          });
//    }
//
//    // Helper functions for the test suite above.
//    private ViewAssertion countRecyclerView(int expected) {
//      return (v, noViewFoundException) -> {
//        if (noViewFoundException != null) {
//          throw noViewFoundException;
//        }
//        RecyclerView view = (RecyclerView) v;
//        RecyclerView.Adapter<?> adapter = view.getAdapter();
//        assert adapter != null;
//        assertThat(adapter.getItemCount()).isEqualTo(expected);
//      };
//    }
//  }

  private static void compareCourseToSerializedCourse(Course course, String serializedCourse)
      throws JsonProcessingException {
    compareCourseToSerializedSummary(course, serializedCourse);
    ObjectNode node = (ObjectNode) mapper.readTree(serializedCourse);
    assertThat(course.getDescription()).isEqualTo(node.get("description").asText());
  }

  private static void compareCourseToSerializedSummary(Course course, String serializedSummary)
      throws JsonProcessingException {
    ObjectNode node = (ObjectNode) mapper.readTree(serializedSummary);
    assertThat(course.getYear()).isEqualTo(node.get("year").asText());
    assertThat(course.getSemester()).isEqualTo(node.get("semester").asText());
    assertThat(course.getDepartment()).isEqualTo(node.get("department").asText());
    assertThat(course.getNumber()).isEqualTo(node.get("number").asText());
    assertThat(course.getTitle()).isEqualTo(node.get("title").asText());
  }
}
