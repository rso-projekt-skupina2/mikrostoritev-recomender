package si.fri.rso.samples.recomender.lib;

import java.time.Instant;

public class Rating {

    private Integer imageId;
    private Integer authorId;
    private Integer rating;

    public Rating(Integer imageId, Integer authorId,Integer rating) {
        this.imageId = imageId;
        this.authorId = authorId;
        this.rating = rating;
    }


    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
