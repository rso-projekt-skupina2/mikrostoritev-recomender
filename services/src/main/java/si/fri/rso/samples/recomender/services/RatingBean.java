package si.fri.rso.samples.recomender.services;

import static jdk.nashorn.internal.runtime.JSType.toInteger;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

import com.kumuluz.ee.configuration.cdi.ConfigBundle;
import com.kumuluz.ee.configuration.cdi.ConfigValue;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import si.fri.rso.samples.recomender.lib.Rating;

@ApplicationScoped
public class RatingBean implements AutoCloseable {
    private Optional<String> url;
    private Optional<String> username;
    private Optional<String> password;
    private Driver driver;

    private Logger log = Logger.getLogger(RatingBean.class.getName());

    private List<Rating> ratings;

    @Override
    public void close() throws Exception
    {
        driver.close();
    }


    @PostConstruct
    private void init() {
        url = ConfigurationUtil.getInstance() .get("configurations.neo4j.url");
        username = ConfigurationUtil.getInstance() .get("configurations.neo4j.username");
        password = ConfigurationUtil.getInstance() .get("configurations.neo4j.password");
        driver = GraphDatabase.driver( url.get(), AuthTokens.basic( username.get(), password.get() ) );
        ratings = new ArrayList<>();
        try ( Session session = driver.session() ){
            ratings = session.writeTransaction(tx -> {
                StatementResult result = tx.run( "MATCH (i:idUporabnik)-[r:RATED]->(s:Slike) " +
                        "RETURN i.name AS idUp,r.rating AS rating,s.name as idSli");
                List<Rating> tempRatings = new ArrayList<>();
                while ( result.hasNext() ) {
                    Record record = result.next();
                    Integer idSli = toInteger(record.get("idSli").asString());
                    Integer idUp = toInteger(record.get("idUp").asString());
                    Integer rating = toInteger( record.get("rating").asFloat());
                    tempRatings.add(new Rating(idSli, idUp,rating));
                }
                return tempRatings;
            });
        } catch (Exception e) {
            log.warning("Povezava do Neo4j baze ni uspela!" + e);
        }
    }

    public List<Rating> getRatings() {
        return ratings;
    }

    public List<Rating> getRatingsForImage(Integer imageId) {
        if(ratings.isEmpty()){
            try ( Session session = driver.session() ){
                return session.writeTransaction(tx -> {
                    StatementResult result = tx.run( "MATCH (i:idUporabnik)-[r:RATED]->(s:Slike) " +
                                    "WHERE s.name =$imageId " +
                                    "RETURN i.name AS idUp,r.rating AS rating,s.name as idSli",
                            parameters( "imageId", imageId ) );
                    List<Rating> tempRatings = new ArrayList<>();
                    while ( result.hasNext() ) {
                        Record record = result.next();
                        Integer idSli = toInteger(record.get("idSli").asString());
                        Integer idUp = toInteger(record.get("idUp").asString());
                        Integer rating = toInteger( record.get("rating").asFloat());
                        tempRatings.add(new Rating(idSli, idUp,rating));
                    }
                    return tempRatings;
                });
            } catch (Exception e) {
                log.warning("Povezava do Neo4j baze ni uspela!" + e);
                return null;
            }
        } else {
            return ratings.stream().filter(rating -> rating.getImageId().equals(imageId)).collect(Collectors.toList());
        }
    }

}
