package si.fri.rso.samples.recomender.dto;

public class Collaborative {
    private int imageId;
    private double averageRating;

    public Collaborative(int imageId, double averageRating) {
        this.imageId = imageId;
        this.averageRating = averageRating;
    }

    public int getImageId() {
        return imageId;
    }

    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }


}
