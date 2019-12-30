package si.fri.rso.samples.recomender.api.v1.resources;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.annotation.Counted;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.cors.annotations.CrossOrigin;
import com.kumuluz.ee.logs.cdi.Log;

import si.fri.rso.samples.recomender.api.v1.dtos.ApiError;
import si.fri.rso.samples.recomender.api.v1.dtos.ApiResponse;
import si.fri.rso.samples.recomender.dto.Collaborative;
import si.fri.rso.samples.recomender.lib.Rating;
import si.fri.rso.samples.recomender.services.RatingBean;

@Log
@ApplicationScoped
@Path("/rating")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RatingResource {
    private Logger log = Logger.getLogger(RatingResource.class.getName());

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

    @GET
    @Counted
    @Path("similarities")
    public Response calculateSimilarities() {
        int countSuccessful = 0;
        for(int i = 1; i<=100; i++){
            if(ratingsBean.calculateSimilarities(i) == 1) countSuccessful++;
        }
        log.info("Za " + countSuccessful + " uporabnikov so bile izracunane podobnosti.");
        return Response.ok().build();
    }

    @GET
    @Counted
    @Path("collaborative/{userId}")
    public Response collaborative(@PathParam("userId") Integer userId) throws JsonProcessingException {
        if (userId == null) {
            ApiError apiError = new ApiError();
            return Response.ok(apiError).build();
        } else {
            List<Collaborative> collaboratives = ratingsBean.collaborative(userId);
            if( !collaboratives.isEmpty()) {
                ApiResponse apiResponse = new ApiResponse();
                ObjectMapper mapper = new ObjectMapper();
                String simpleJSON = mapper.writeValueAsString(collaboratives);
                apiResponse.setMessage(simpleJSON);
                return Response.ok(collaboratives).build();
            } else {
                ApiError apiError = new ApiError();
                return Response.ok(apiError).build();
            }
        }
    }

    @POST
    @Counted
    @Path("rate")
    @CrossOrigin(allowOrigin = "*", supportedHeaders="*")
    @Produces(MediaType.APPLICATION_JSON)
    public Response rate (@QueryParam("imageId") Integer imageId, @QueryParam("userId") Integer userId, @QueryParam("rating") Integer rating,  @QueryParam("callback") String callback) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setStatus(404);
        apiResponse.setCode("resource.not.found");

        if (imageId == null) {
            apiResponse.setMessage("imageId ne sme biti prazen!");
            return Response.status(400).build();
        } else if (userId == null){
            apiResponse.setMessage("userId ne sme biti prazen!");
            return Response.status(400).build();
        } else if (rating == null){
            apiResponse.setMessage("rating ne sme biti prazen!");
            return Response.status(400).build();
        } else if (!(rating >= 1 && rating <= 5)){
            apiResponse.setMessage("rating mora biti na intervalu od 1 do 5!");
            return Response.status(400).build();
        } else {
            int addRating = ratingsBean.addRating(imageId,userId,rating);
            if(addRating == -1) {
                apiResponse.setMessage("Rating ni bil dodan! Uporabnik ali slika s tem ID ne obstaja.");
                return Response.status(400).build();
            } else {
                ApiResponse successful = new ApiResponse();
                successful.setMessage("Rating uspesno dodan.");
                return Response.ok(successful).build();
            }
        }
    }
}
