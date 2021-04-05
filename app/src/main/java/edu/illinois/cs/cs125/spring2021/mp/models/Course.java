package edu.illinois.cs.cs125.spring2021.mp.models;

/**
 * Course is a class that gets description for the course.
 */
public class Course extends Summary {
  private String description;

  /**
   * gets the Description.
   * @return a string of the description.
   */
  public String getDescription() {
    return description;
  }
}
