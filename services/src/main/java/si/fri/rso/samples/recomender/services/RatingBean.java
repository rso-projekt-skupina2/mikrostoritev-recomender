package si.fri.rso.samples.recomender.services;

import static jdk.nashorn.internal.runtime.JSType.toDouble;
import static jdk.nashorn.internal.runtime.JSType.toInteger;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import si.fri.rso.samples.recomender.dto.Collaborative;
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

    public int addRating(Integer imageId, Integer userId, Integer rating) {

        try ( Session session = driver.session() ){
            StatementResult result = session.run("MATCH (m:Slike {name:'" + imageId + "'}) " +
                    "MATCH (u:idUporabnik{name:'" + userId + "'}) CREATE (u)-[r:RATED]->(m) SET r.rating=" + toDouble(rating) + " RETURN u");
            if (!result.hasNext()) return -1;
            return 1;
        } catch (Exception e) {
            log.warning("Povezava do Neo4j baze ni uspela!" + e);
            return -1;
        }
    }

    public int calculateSimilarities( Integer userId) {

        try ( Session session = driver.session() ){
            StatementResult result = session.run("MATCH (p1:idUporabnik {name: '"  + userId +"'})-[x:RATED]->(m:Slike)<-[y:RATED]-(p2:idUporabnik) \n" +
                    "WITH COUNT(m) AS stSlik, SUM(x.rating * y.rating) AS xyDotProduct, \n" +
                    "SQRT(REDUCE(xDot = 0.0, a IN COLLECT(x.rating) | xDot + a^2)) AS xLength, \n" +
                    "SQRT(REDUCE(yDot = 0.0, b IN COLLECT(y.rating) | yDot + b^2)) AS yLength, \n" +
                    "p1, p2 WHERE stSlik > 1 \n" +
                    "WITH p1, p2,(xyDotProduct / (xLength * yLength)) as podob Order by podob\n" +
                    "  MERGE (p1)-[s:SIMILARITY]-(p2) \n" +
                    "  SET s.similarity = podob \n" +
                    "  RETURN podob");
            if (!result.hasNext()) return -1;
            return 1;
        } catch (Exception e) {
            log.warning("Povezava do Neo4j baze ni uspela!" + e);
            return -1;
        }
    }

    public List<Collaborative> collaborative(Integer userId) {

        try ( Session session = driver.session() ){
            StatementResult result = session.run("MATCH (a:idUporabnik {name: '"  + userId +"'})-[s:SIMILARITY]->(b:idUporabnik)-[r:RATED]->(m:Slike) \n" +
                    "WHERE NOT((a)-[:RATED]->(m)) \n" +
                    "WITH b,r,m,a,SUM( s.similarity * r.rating) AS score \n" +
                    "ORDER BY score DESC LIMIT 8 \n" +
                    "RETURN distinct m.name,m.averageRating");
            List<Collaborative> sz = new ArrayList<>();
            while(result.hasNext()){
                Record record = result.next();
                Collaborative col = new Collaborative(toInteger(record.get("m.name").asString()), (record.get("m.averageRating")).asDouble());
                sz.add(col);
            }
            return sz;
        } catch (Exception e) {
            log.warning("Povezava do Neo4j baze ni uspela!" + e);
            return Collections.emptyList();
        }
    }

}
