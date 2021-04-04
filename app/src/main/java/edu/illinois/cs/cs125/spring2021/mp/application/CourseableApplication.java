package edu.illinois.cs.cs125.spring2021.mp.application;

import android.app.Application;
import android.os.Build;

import edu.illinois.cs.cs125.spring2021.mp.network.Client;
import edu.illinois.cs.cs125.spring2021.mp.network.Server;

/**
 * Application class for the Courseable app.
 *
 * <p>Starts the development server and creates the course API client.
 *
 * <p>You should not need to modify this file.
 */
public class CourseableApplication extends Application {
  /** Course API server port. You can change this if needed. */
  public static final int DEFAULT_SERVER_PORT = 8989;

  /** Course API server URL. */
  public static final String SERVER_URL = "http://localhost:" + DEFAULT_SERVER_PORT + "/";

  // Course API client created during application startup
  private Client client;

  @Override
  public final void onCreate() {
    super.onCreate();

    client = Client.start();
    if (Build.FINGERPRINT.equals("robolectric")) {
      Server.start();
    } else {
      new Thread(Server::start).start();
    }
  }

  /**
   * Retrieve the course API client instance for this app.
   *
   * @return the course API client instance.
   */
  public final Client getCourseClient() {
    return client;
  }
}
