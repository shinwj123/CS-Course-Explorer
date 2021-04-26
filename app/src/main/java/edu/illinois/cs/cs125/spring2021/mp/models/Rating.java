package edu.illinois.cs.cs125.spring2021.mp.models;

public class Rating {
  public static final double NOT_RATED = -1.0;
  private String id;
  private double rating;

  public Rating() {}

  public Rating(String setId, double setRating) {
    id = setId;
    rating = setRating;
  }

  public String getId() {
    return null;
  }

  public double getRating() {
    return NOT_RATED;
  }
}
