package si.fri.rso.samples.recomender.services;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import si.fri.rso.samples.recomender.lib.Rating;

@ApplicationScoped
public class RatingBean {

    private Logger log = Logger.getLogger(RatingBean.class.getName());

    private List<Rating> ratings;

    @PostConstruct
    private void init() {
        ratings = new ArrayList<>();
        ratings.add(new Rating(1, 5, 1));
        ratings.add(new Rating(1, 1, 2));
        ratings.add(new Rating(1, 2, 5));
        ratings.add(new Rating(1, 3, 4));
        ratings.add(new Rating(1, 4, 4));
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public List<Rating> getRatingsForImage(Integer imageId) {
        return ratings.stream().filter(rating -> rating.getImageId().equals(imageId)).collect(Collectors.toList());
    }

}
