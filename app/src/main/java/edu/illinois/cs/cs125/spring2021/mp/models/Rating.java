package edu.illinois.cs.cs125.spring2021.mp.models;

/**
 * allows to give thr Rating feature.
 */
public class Rating {
  /**
   * final double Not_Rated.
   */
  public static final double NOT_RATED = -1.0;
  private String id;
  private double rating;

  /**
   * Empty constructor.
   */
  public Rating() {}

  /**
   * constructer for setID and setRating.
   * @param setId
   * @param setRating
   */
  public Rating(final String setId, final double setRating) {
    id = setId;
    rating = setRating;
  }

  /**
   * gets the ID.
   * @return ID.
   */
  public String getId() {
    return null;
  }

  /**
   * gets the Rating.
   * @return NOT_RATED.
   */
  public double getRating() {
    return NOT_RATED;
  }
}
