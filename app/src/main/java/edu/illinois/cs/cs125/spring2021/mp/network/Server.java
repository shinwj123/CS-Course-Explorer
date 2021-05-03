package edu.illinois.cs.cs125.spring2021.mp.network;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.illinois.cs.cs125.spring2021.mp.application.CourseableApplication;
import edu.illinois.cs.cs125.spring2021.mp.models.Rating;
import edu.illinois.cs.cs125.spring2021.mp.models.Summary;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Development course API server.
 *
 * <p>Normally you would run this server on another machine, which the client would connect to over
 * the internet. For the sake of development, we're running the server right alongside the app on
 * the same device. However, all communication between the course API client and course API server
 * is still done using the HTTP protocol. Meaning that eventually it would be straightforward to
 * move this server to another machine where it could provide data for all course API clients.
 *
 * <p>You will need to add functionality to the server for MP1 and MP2.
 */
public final class Server extends Dispatcher {
  @SuppressWarnings({"unused", "RedundantSuppression"})
  private static final String TAG = Server.class.getSimpleName();

  private final Map<String, String> summaries = new HashMap<>();

  private boolean isJson(final String string) {
    try {
      ObjectMapper test = new ObjectMapper();
      test.readTree(string);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private MockResponse getSummary(@NonNull final String path) {
    String[] parts = path.split("/");
    if (parts.length != 2) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    String summary = summaries.get(parts[0] + "_" + parts[1]);
    if (summary == null) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(summary);
  }

  private MockResponse getString(@NonNull final RecordedRequest request) {
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("foo");

  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<Summary, String> courses = new HashMap<>();

  private MockResponse getCourse(@NonNull final String path) {
    String[] parts = path.split("/");
    final int pathSize = 4;
    if (parts.length != pathSize) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    Summary summary = new Summary(parts[0], parts[1], parts[2], parts[3], "");
    String course = courses.get(summary);
    if (course == null) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    }
    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(course);
  }

  private final Map<Summary, Map<String, Rating>> ratings = new HashMap<>();

  private MockResponse getRating(@NonNull final String value,
                                 @NonNull final RecordedRequest request) throws JsonProcessingException {
    ObjectMapper map = new ObjectMapper();
    if (!value.contains("?")) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    int uuid = value.indexOf("=");

    final int uuidLength = 36;
    String clientID = value.substring(uuid + 1);

    int sum = value.indexOf("?");
    String sumOfString = value.substring(0, sum);

    int count = 0;
    Summary summary = new Summary();
    for (Summary sums : courses.keySet()) {
      if (sumOfString.equals(sums.getYear() + "/" + sums.getSemester() + "/" + sums.getDepartment()
              + "/" + sums.getNumber())) {
        count++;
        summary = sums;
      }
    }

    if (request.getMethod().equals("GET")) {
      Map<String, Rating> secondLevel = ratings.getOrDefault(summary, new HashMap<>());
      if (secondLevel.get(clientID) == null) {
        secondLevel.put(clientID, new Rating(clientID, Rating.NOT_RATED));
      }
      ratings.put(summary, secondLevel);
      if (clientID.length() != uuidLength) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      } else if (count < 1) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
      }
      Rating rate = ratings.get(summary).get(clientID);
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(map.writeValueAsString(rate));
    }

    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK);
  }

  private MockResponse postRating(@NonNull final String value,
                                  @NonNull final RecordedRequest request) throws JsonProcessingException {
    String string = request.getBody().readUtf8();
    if (!(isJson(string)) || (value.startsWith("/rating/")) || !(value.contains("?client"))) {
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
    } else {
      Rating rating = mapper.readValue(string, Rating.class);
      int input = value.indexOf("?");
      String urlPath = value.substring(0, input);
      Summary summary = new Summary();
      int count = 0;
      for (Summary sums : courses.keySet()) {
        if (urlPath.equals(sums.getYear() + "/" + sums.getSemester() + "/" + sums.getDepartment()
                + "/" + sums.getNumber())) {
          summary = sums;
          count++;
        }
      }
      if (count < 1) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);
      }
      Map<String, Rating> secondLevel = new HashMap<>();
      if (ratings.get(summary) == null) {
        secondLevel.put(rating.getId(), rating);
        ratings.put(summary, secondLevel);
      } else {
        secondLevel = ratings.get(summary);
        secondLevel.put(rating.getId(), rating);
        ratings.put(summary, secondLevel);
      }
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
              .setHeader("Location", "");
    }
  }


  @NonNull
  @Override
  public MockResponse dispatch(@NonNull final RecordedRequest request) {
    try {
      String path = request.getPath();
      if (path == null || request.getMethod() == null) {
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
      } else if (path.equals("/") && request.getMethod().equalsIgnoreCase("GET")) {
        return new MockResponse().setBody("CS125").setResponseCode(HttpURLConnection.HTTP_OK);
      } else if (path.startsWith("/summary/")) {
        return getSummary(path.replaceFirst("/summary/", ""));
      } else if (path.startsWith("/string")) {
        return getString(request);
      } else if (path.startsWith("/course/")) {
        return getCourse(path.replaceFirst("/course/", ""));
      } else if (path.startsWith("/rating/") && request.getMethod().equalsIgnoreCase("GET")) {
        System.out.println("found the rating");
        return getRating(path.replaceFirst("/rating/", ""), request);
      } else if (path.startsWith("/rating/") && request.getMethod().equalsIgnoreCase("POST")) {
        System.out.println("found the rating");
        return postRating(path.replaceFirst("/rating/", ""), request);
      }
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
    } catch (Exception e) {
      e.printStackTrace();
      return new MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
    }
  }

  /**
   * Start the server if has not already been started.
   *
   * <p>We start the server in a new thread so that it operates separately from and does not
   * interfere with the rest of the app.
   */
  public static void start() {
    if (!isRunning(false)) {
      new Thread(Server::new).start();
    }
    if (!isRunning(true)) {
      throw new IllegalStateException("Server should be running");
    }
  }

  /**
   * Number of times to check the server before failing.
   */
  private static final int RETRY_COUNT = 8;

  /**
   * Delay between retries.
   */
  private static final int RETRY_DELAY = 512;

  /**
   * Determine if the server is currently running.
   *
   * @param wait whether to wait or not
   * @return whether the server is running or not
   * @throws IllegalStateException if something else is running on our port
   */
  public static boolean isRunning(final boolean wait) {
    for (int i = 0; i < RETRY_COUNT; i++) {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().url(CourseableApplication.SERVER_URL).get().build();
      try {
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
          if (Objects.requireNonNull(response.body()).string().equals("CS125")) {
            return true;
          } else {
            throw new IllegalStateException(
                    "Another server is running on port " + CourseableApplication.DEFAULT_SERVER_PORT);
          }
        }
      } catch (IOException ignored) {
        if (!wait) {
          break;
        }
        try {
          Thread.sleep(RETRY_DELAY);
        } catch (InterruptedException ignored1) {
        }
      }
    }
    return false;
  }

  private final ObjectMapper mapper = new ObjectMapper();

  private Server() {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    loadSummary("2021", "spring");
    loadCourses("2021", "spring");

    try {
      MockWebServer server = new MockWebServer();
      server.setDispatcher(this);
      server.start(CourseableApplication.DEFAULT_SERVER_PORT);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException(e.getMessage());
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void loadSummary(@NonNull final String year, @NonNull final String semester) {
    String filename = "/" + year + "_" + semester + "_summary.json";
    String json =
            new Scanner(Server.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A").next();
    summaries.put(year + "_" + semester, json);
  }

  @SuppressWarnings("SameParameterValue")
  private void loadCourses(@NonNull final String year, @NonNull final String semester) {
    String filename = "/" + year + "_" + semester + ".json";
    String json =
            new Scanner(Server.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A").next();
    try {
      JsonNode nodes = mapper.readTree(json);
      for (Iterator<JsonNode> it = nodes.elements(); it.hasNext(); ) {
        JsonNode node = it.next();
        Summary course = mapper.readValue(node.toString(), Summary.class);
        courses.put(course, node.toPrettyString());
      }
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
