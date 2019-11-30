package si.fri.rso.samples.recomender.api.v1.resources;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;

import com.kumuluz.ee.logs.cdi.Log;

import si.fri.rso.samples.recomender.lib.Rating;
import si.fri.rso.samples.recomender.services.RatingBean;

@Log
@ApplicationScoped
@Path("/rating")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RatingResource {

    @Inject
    private RatingBean ratingsBean;

    @GET
    @Counted
    public Response getRatings(@QueryParam("imageId") Integer imageId) {
        List<Rating> ratings;
        if (imageId != null) {
            ratings = ratingsBean.getRatingsForImage(imageId);
        } else {
            ratings = ratingsBean.getRatings();
        }

        return Response.ok(ratings).build();
    }

    @GET
    @Counted
    @Path("count")
    public Response getRatingsCount(@QueryParam("imageId") Integer imageId) {
        List<Rating> ratings;
        if (imageId != null) {
            ratings = ratingsBean.getRatingsForImage(imageId);
        } else {
            ratings = ratingsBean.getRatings();
        }

        return Response.ok(ratings.size()).build();
    }

    @GET
    @Counted
    @Path("averge")
    public Response getAvergeRating(@QueryParam("imageId") Integer imageId) {
        List<Rating> ratings;
        if (imageId != null) {
            ratings = ratingsBean.getRatingsForImage(imageId);
        } else {
            ratings = ratingsBean.getRatings();
        }
        Double avgRating = ratings
                        .stream()
                        .collect(Collectors.averagingInt(Rating::getRating));
        return Response.ok(avgRating).build();
    }

}
