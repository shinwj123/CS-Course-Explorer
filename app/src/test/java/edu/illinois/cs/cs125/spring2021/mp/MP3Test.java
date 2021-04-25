package edu.illinois.cs.cs125.spring2021.mp;

import static android.os.Looper.getMainLooper;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.view.View;
import android.widget.RatingBar;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
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
import edu.illinois.cs.cs125.spring2021.mp.activities.CourseActivity;
import edu.illinois.cs.cs125.spring2021.mp.application.CourseableApplication;
import edu.illinois.cs.cs125.spring2021.mp.models.Rating;
import edu.illinois.cs.cs125.spring2021.mp.models.Summary;
import edu.illinois.cs.cs125.spring2021.mp.network.Client;
import edu.illinois.cs.cs125.spring2021.mp.network.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/*
 * This is the MP3 test suite.
 * These are the same tests that we will run on your code during official grading.
 *
 * You do not need to necessarily understand all of the code below.
 * However, we have tried to write these tests in a way similar to how we would for a real Android project.
 *
 * The MP3 unit tests test that your Course model and server route work properly.
 * The MP3 integration tests test your network `Client`, your `CourseActivity`, and confirm that the course detail
 * view launches properly from the MainActivity.
 *
 * As in previous checkpoints we strongly suggest that you work through these test in order.
 * Like MP2 you will need to create some stub methods initially before anything will work.
 * The MP3 writeup will walk you through this process.
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
public final class MP3Test {
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final List<String> summaries = new ArrayList<>();
  private static final List<String> courses = new ArrayList<>();

  private static final Random random = new Random();

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
      MP3Test.setup();
    }

    /**
     * Test the GET rating server route.
     */
    @Test(timeout = 10000L)
    @Graded(points = 15)
    public void testServerGetRating() throws IOException {
      Server.start();
      // Check the backend to make sure its responding to requests correctly
      assertThat(Server.isRunning(false)).isTrue();

      OkHttpClient client = new OkHttpClient();

      String[] randomIDs = {
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString()
      };
      for (String courseString : courses.subList(0, 8)) {
        for (String clientID : randomIDs) {
          ObjectNode node = (ObjectNode) mapper.readTree(courseString);
          String url =
              CourseableApplication.SERVER_URL
                  + "rating/"
                  + node.get("year").asText()
                  + "/"
                  + node.get("semester").asText()
                  + "/"
                  + node.get("department").asText()
                  + "/"
                  + node.get("number").asText()
                  + "?client="
                  + clientID;
          Request ratingRequest = new Request.Builder().url(url).build();
          Response ratingResponse = client.newCall(ratingRequest).execute();
          assertThat(ratingResponse.code()).isEqualTo(HttpStatus.SC_OK);
          ResponseBody body = ratingResponse.body();
          assertThat(body).isNotNull();
          ObjectNode rating = (ObjectNode) mapper.readTree(body.string());
          assertThat(rating.get("rating").asDouble()).isEqualTo(Rating.NOT_RATED);
        }
      }

      // Test some bad requests

      // Bad course
      Request request =
          new Request.Builder()
              .url(
                  CourseableApplication.SERVER_URL
                      + "rating/2021/spring/CS/188?client=79137a60-19a5-405b-8a6e-65f48c0b5400")
              .build();
      Response response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_NOT_FOUND);

      // Bad URL
      request =
          new Request.Builder()
              .url(
                  CourseableApplication.SERVER_URL
                      + "ratings/2021/spring/CS/125/?client=79137a60-19a5-405b-8a6e-65f48c0b5400")
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_NOT_FOUND);

      // No client
      request =
          new Request.Builder()
              .url(CourseableApplication.SERVER_URL + "rating/2021/spring/CS/125/")
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_BAD_REQUEST);

      // Bogus client
      request =
          new Request.Builder()
              .url(CourseableApplication.SERVER_URL + "rating/2021/spring/CS/125/?client=bogus")
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test the POST rating server route.
     */
    @Test(timeout = 20000L)
    @Graded(points = 15)
    public void testServerPostRating() throws IOException {
      Server.start();
      // Check the backend to make sure its responding to requests correctly
      assertThat(Server.isRunning(false)).isTrue();

      OkHttpClient client = new OkHttpClient();

      List<String> randomIDs =
          Arrays.asList(
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString());
      List<String> testedCourses = courses.subList(0, 8);
      Map<String, Double> testRatings = new HashMap<>();

      for (int i = 0; i < 128; i++) {
        String courseString = testedCourses.get(random.nextInt(testedCourses.size()));
        String clientID = randomIDs.get(random.nextInt(randomIDs.size()));

        // Construct URL
        ObjectNode node = (ObjectNode) mapper.readTree(courseString);
        String url =
            CourseableApplication.SERVER_URL
                + "rating/"
                + node.get("year").asText()
                + "/"
                + node.get("semester").asText()
                + "/"
                + node.get("department").asText()
                + "/"
                + node.get("number").asText()
                + "?client="
                + clientID;

        // Initial GET
        Request firstGetRequest = new Request.Builder().url(url).build();
        Response firstGetResponse = client.newCall(firstGetRequest).execute();
        assertThat(firstGetResponse.code()).isEqualTo(HttpStatus.SC_OK);
        assertThat(firstGetResponse.body()).isNotNull();
        ObjectNode firstRating =
            (ObjectNode) mapper.readTree(Objects.requireNonNull(firstGetResponse.body()).string());
        assertThat(firstRating.get("id").asText()).isEqualTo(clientID);
        assertThat(firstRating.get("rating").asDouble())
            .isEqualTo(testRatings.getOrDefault(url + "_" + clientID, Rating.NOT_RATED));

        // POST to change rating
        double testRating = random.nextInt(51) / 10.0;
        testRatings.put(url + "_" + clientID, testRating);
        ObjectNode newRating = mapper.createObjectNode();
        newRating.set("id", mapper.convertValue(clientID, JsonNode.class));
        newRating.set("rating", mapper.convertValue(testRating, JsonNode.class));

        Request postRatingRequest =
            new Request.Builder()
                .url(url)
                .post(RequestBody.create(newRating.toString(), MediaType.parse("application/json")))
                .build();
        Response postRatingResponse = client.newCall(postRatingRequest).execute();
        assertThat(postRatingResponse.code()).isEqualTo(HttpStatus.SC_OK);
        ResponseBody body = postRatingResponse.body();
        assertThat(body).isNotNull();
        ObjectNode rating = (ObjectNode) mapper.readTree(body.string());
        assertThat(rating.get("rating").asDouble())
            .isEqualTo(testRatings.get(url + "_" + clientID));

        // Final GET
        Request secondGetRequest = new Request.Builder().url(url).build();
        Response secondGetResponse = client.newCall(secondGetRequest).execute();
        assertThat(secondGetResponse.code()).isEqualTo(HttpStatus.SC_OK);
        ObjectNode secondRating =
            (ObjectNode) mapper.readTree(Objects.requireNonNull(secondGetResponse.body()).string());
        assertThat(secondRating.get("id").asText()).isEqualTo(clientID);
        assertThat(secondRating.get("rating").asDouble())
            .isEqualTo(testRatings.getOrDefault(url + "_" + clientID, Rating.NOT_RATED));
      }

      // Bad requests

      String clientID = "79137a60-19a5-405b-8a6e-65f48c0b5400";
      ObjectNode newRating = mapper.createObjectNode();
      newRating.set("id", mapper.convertValue(clientID, JsonNode.class));
      newRating.set("rating", mapper.convertValue(3.0, JsonNode.class));

      // Bad URL
      Request request =
          new Request.Builder()
              .url(
                  CourseableApplication.SERVER_URL
                      + "ratings/2021/spring/CS/125/?client="
                      + clientID)
              .post(RequestBody.create(newRating.toString(), MediaType.parse("application/json")))
              .build();
      Response response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_NOT_FOUND);

      // No client
      request =
          new Request.Builder()
              .url(CourseableApplication.SERVER_URL + "rating/2021/spring/CS/125/")
              .post(RequestBody.create(newRating.toString(), MediaType.parse("application/json")))
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_BAD_REQUEST);

      // Bogus client in rating
      newRating.set("id", mapper.convertValue("bogus", JsonNode.class));
      request =
          new Request.Builder()
              .url(
                  CourseableApplication.SERVER_URL
                      + "rating/2021/spring/CS/125/?client="
                      + clientID)
              .post(RequestBody.create(newRating.toString(), MediaType.parse("application/json")))
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_BAD_REQUEST);

      // Bogus client in URL
      newRating.set("id", mapper.convertValue(clientID, JsonNode.class));
      request =
          new Request.Builder()
              .url(CourseableApplication.SERVER_URL + "rating/2021/spring/CS/125/?client=bogus")
              .post(RequestBody.create(newRating.toString(), MediaType.parse("application/json")))
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_BAD_REQUEST);

      // Rating id v. URL id mismatch
      request =
          new Request.Builder()
              .url(
                  CourseableApplication.SERVER_URL
                      + "rating/2021/spring/CS/125/?client=78ee69ee-65e9-472b-8bca-4e3c9c085661")
              .post(RequestBody.create(newRating.toString(), MediaType.parse("application/json")))
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_BAD_REQUEST);

      // Bogus body
      request =
          new Request.Builder()
              .url(
                  CourseableApplication.SERVER_URL
                      + "rating/2021/spring/CS/125/?client=78ee69ee-65e9-472b-8bca-4e3c9c085661")
              .post(RequestBody.create("you are not alone", MediaType.parse("application/json")))
              .build();
      response = client.newCall(request).execute();
      assertThat(response.code()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }
  }

  @SuppressWarnings({"SameParameterValue", "MismatchedQueryAndUpdateOfCollection"})
  @RunWith(AndroidJUnit4.class)
  @LooperMode(LooperMode.Mode.PAUSED)
  public static class IntegrationTests {
    @BeforeClass
    public static void setup() throws IOException {
      MP3Test.setup();
    }

    /**
     * Test the client getRating method
     */
    @Test(timeout = 20000L)
    @Graded(points = 15)
    public void testClientGetRating()
        throws JsonProcessingException, InterruptedException, ExecutionException {
      Client client = Client.startTesting();

      List<String> randomIDs =
          Arrays.asList(
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString());

      for (String summaryString : summaries) {
        Summary summary = mapper.readValue(summaryString, Summary.class);
        String clientID = randomIDs.get(random.nextInt(randomIDs.size()));

        CompletableFuture<Rating> completableFuture = new CompletableFuture<>();
        client.getRating(
            summary,
            clientID,
            new Client.CourseClientCallbacks() {
              @Override
              public void yourRating(Summary summary, Rating rating) {
                completableFuture.complete(rating);
              }
            });
        Rating rating = completableFuture.get();
        assertThat(rating.getId()).isEqualTo(clientID);
        assertThat(rating.getRating()).isEqualTo(Rating.NOT_RATED);
      }
    }

    /**
     * Test the client getRating method
     */
    @Test(timeout = 10000L)
    @Graded(points = 15)
    public void testClientPostRating()
        throws JsonProcessingException, InterruptedException, ExecutionException {
      Client client = Client.startTesting();

      List<String> randomIDs =
          Arrays.asList(
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString(),
              UUID.randomUUID().toString());

      Map<String, Map<Summary, Double>> testRatings = new HashMap<>();

      for (String summaryString : summaries.subList(0, 8)) {
        Summary summary = mapper.readValue(summaryString, Summary.class);
        String clientID = randomIDs.get(0); // randomIDs.get(random.nextInt(randomIDs.size()));

        CompletableFuture<Rating> completableFuture = new CompletableFuture<>();
        if (random.nextBoolean()) {
          client.getRating(
              summary,
              clientID,
              new Client.CourseClientCallbacks() {
                @Override
                public void yourRating(Summary summary, Rating rating) {
                  completableFuture.complete(rating);
                }
              });
        } else {
          double testRating = random.nextInt(51) / 10.0;
          Map<Summary, Double> innerMap = testRatings.getOrDefault(clientID, new HashMap<>());
          innerMap.put(summary, testRating);
          testRatings.put(clientID, innerMap);
          client.postRating(
              summary,
              new Rating(clientID, testRating),
              new Client.CourseClientCallbacks() {
                @Override
                public void yourRating(Summary summary, Rating rating) {
                  completableFuture.complete(rating);
                }
              });
        }
        double expectedRating = Rating.NOT_RATED;
        try {
          expectedRating = Objects.requireNonNull(testRatings.get(clientID)).get(summary);
        } catch (NullPointerException ignored) {
        }
        Rating rating = completableFuture.get();
        assertThat(rating.getId()).isEqualTo(clientID);
        assertThat(rating.getRating()).isEqualTo(expectedRating);
      }
    }

    /**
     * Test rating view.
     */
    @Test(timeout = 30000L)
    @Graded(points = 20)
    public void testRatingView() throws JsonProcessingException, InterruptedException {

      int i = 1;
      for (int j = 0; j < 4; j++) {
        String summaryString = summaries.get(i);
        String courseString = courses.get(i);
        Intent intent =
            new Intent(ApplicationProvider.getApplicationContext(), CourseActivity.class);
        ObjectNode summaryForIntent = (ObjectNode) mapper.readTree(summaryString);
        summaryForIntent.remove("description");
        intent.putExtra("COURSE", summaryForIntent.toString());
        ActivityScenario<CourseActivity> courseScenario = ActivityScenario.launch(intent);
        courseScenario.moveToState(Lifecycle.State.CREATED);
        courseScenario.moveToState(Lifecycle.State.RESUMED);
        shadowOf(getMainLooper()).runToEndOfTasks();
        Thread.sleep(100);
        ObjectNode course = (ObjectNode) mapper.readTree(courseString);
        onView(ViewMatchers.withText(course.get("description").asText()))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        onView(withId(R.id.rating)).check(hasRating(0)).perform(setRating(i)).check(hasRating(i));
        i++;
      }

      i = 1;
      for (int j = 0; j < 4; j++) {
        String summaryString = summaries.get(i);
        Intent intent =
            new Intent(ApplicationProvider.getApplicationContext(), CourseActivity.class);
        ObjectNode summaryForIntent = (ObjectNode) mapper.readTree(summaryString);
        summaryForIntent.remove("description");
        intent.putExtra("COURSE", summaryForIntent.toString());
        ActivityScenario<CourseActivity> courseScenario = ActivityScenario.launch(intent);
        courseScenario.moveToState(Lifecycle.State.CREATED);
        courseScenario.moveToState(Lifecycle.State.RESUMED);
        shadowOf(getMainLooper()).runToEndOfTasks();
        Thread.sleep(100);
        onView(withId(R.id.rating))
            .check(hasRating(i))
            .perform(setRating(5 - i))
            .check(hasRating(5 - i));
        i++;
      }

      i = 1;
      for (int j = 0; j < 4; j++) {
        String summaryString = summaries.get(i);
        Intent intent =
            new Intent(ApplicationProvider.getApplicationContext(), CourseActivity.class);
        ObjectNode summaryForIntent = (ObjectNode) mapper.readTree(summaryString);
        summaryForIntent.remove("description");
        intent.putExtra("COURSE", summaryForIntent.toString());
        ActivityScenario<CourseActivity> courseScenario = ActivityScenario.launch(intent);
        courseScenario.moveToState(Lifecycle.State.CREATED);
        courseScenario.moveToState(Lifecycle.State.RESUMED);
        shadowOf(getMainLooper()).runToEndOfTasks();
        Thread.sleep(100);
        onView(withId(R.id.rating))
            .check(hasRating(5 - i))
            .perform(setRating(i))
            .check(hasRating(i));
        i++;
      }
    }

    /**
     * ViewAssertion for RatingBar rating examination.
     *
     * @param rating the rating the RatingBar should have
     * @return a ViewAssertion that checks that the view is a RatingBar with the specified rating
     */
    public static ViewAssertion hasRating(int rating) {
      return (view, noViewFoundException) -> {
        RatingBar ratingBar = (RatingBar) view;
        assertThat(ratingBar.getRating()).isEqualTo(rating);
      };
    }

    /**
     * ViewAction for RatingBar rating modification.
     *
     * @param rating the rating to set
     * @return a ViewAction that sets the rating for the RatingBar to the specified rating
     */
    public static ViewAction setRating(int rating) {
      return actionWithAssertions(
          new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
              return ViewMatchers.isAssignableFrom(RatingBar.class);
            }

            @Override
            public String getDescription() {
              return "Custom view action to set rating.";
            }

            @Override
            public void perform(UiController uiController, View view) {
              RatingBar ratingBar = (RatingBar) view;
              ratingBar.setRating(rating);
            }
          });
    }
  }
}
