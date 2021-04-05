package edu.illinois.cs.cs125.spring2021.mp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import edu.illinois.cs.cs125.spring2021.mp.R;
import edu.illinois.cs.cs125.spring2021.mp.application.CourseableApplication;
import edu.illinois.cs.cs125.spring2021.mp.databinding.ActivityCourseBinding;

import edu.illinois.cs.cs125.spring2021.mp.models.Course;
import edu.illinois.cs.cs125.spring2021.mp.models.Summary;
import edu.illinois.cs.cs125.spring2021.mp.network.Client;

/**
 * CourseActivity is the screen for app, displays the course.
 */
public class CourseActivity extends AppCompatActivity implements Client.CourseClientCallbacks {
  @SuppressWarnings({"unused", "RedundantSuppression"})
  private static final String TAG = CourseActivity.class.getSimpleName();

  // Binding to the layout in activity_main.xml
  private ActivityCourseBinding binding;

  /**
   * Oncreate is called when you go into the screen.
   * @param savedInstanceState it remembers the previous screen you were on.
   */
  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i(TAG, "CourseActivity Launched");

    // Bind to the layout in activity_main.xml
    binding = DataBindingUtil.setContentView(this, R.layout.activity_course);
    binding.title.setText("Hello, Chuchu!");

    Intent intent = getIntent();
    String course = intent.getStringExtra("COURSE");
    Log.i(TAG, course);

    //TODO: Use getCourse to retrieve information about the course passed in the intent\
    CourseableApplication application = (CourseableApplication) getApplication();
    //application.getCourseClient().getCourse(course, this);
  }

  @Override
  public void courseResponse(final Summary summary, final Course course) {

  }
}
