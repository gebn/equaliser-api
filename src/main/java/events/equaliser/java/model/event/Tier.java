package events.equaliser.java.model.event;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Tier {

    private final int id;
    private final String name;
    private final BigDecimal price;
    private final int availability;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getAvailability() {
        return availability;
    }

    private Tier(int id, String name, BigDecimal price, int availability) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.availability = availability;
    }

    /**
     * Turn a JSON object into a tier.
     *
     * @param json The JSON object with correct keys.
     * @return The Tier representation of the object.
     */
    private static Tier fromJsonObject(JsonObject json) {
        return new Tier(json.getInteger("TierID"),
                json.getString("TierName"),
                new BigDecimal(json.getString("TierPrice")),
                json.getInteger("TierAvailability"));
    }

    static void retrieveByFixture(int fixtureId,
                                  SQLConnection connection,
                                  Handler<AsyncResult<List<Tier>>> result) {
        JsonArray params = new JsonArray().add(fixtureId);
        connection.queryWithParams(
                "SELECT " +
                    "TierID, " +
                    "Name AS TierName, " +
                    "Price AS TierPrice, " +
                    "Availability AS TierAvailability, " +
                    "ReturnsPolicy AS TierReturnsPolicy " +
                "FROM Tiers " +
                "WHERE FixtureID = ?;",
                params, tiersRes -> {
                    if (tiersRes.succeeded()) {
                        ResultSet resultSet = tiersRes.result();
                        List<Tier> tiers = resultSet.getRows()
                                .stream()
                                .map(Tier::fromJsonObject)
                                .collect(Collectors.toList());
                        result.handle(Future.succeededFuture(tiers));
                    }
                    else {
                        result.handle(Future.failedFuture(tiersRes.cause()));
                    }
                });
    }

    static void retrieveBySeries(int seriesId,
                                 SQLConnection connection,
                                 Handler<AsyncResult<Map<Integer,List<Tier>>>> result) {
        JsonArray params = new JsonArray().add(seriesId);
        connection.queryWithParams(
                "SELECT " +
                    "Fixtures.FixtureID, " +
                    "Tiers.TierID, " +
                    "Tiers.Name AS TierName, " +
                    "Tiers.Price AS TierPrice, " +
                    "Tiers.Availability AS TierAvailability, " +
                    "Tiers.ReturnsPolicy AS TierReturnsPolicy " +
                "FROM Fixtures " +
                    "INNER JOIN Tiers " +
                        "ON Tiers.FixtureID = Fixtures.FixtureID " +
                "WHERE Fixtures.SeriesID = ?;",
                params, tiers -> {
                    if (tiers.succeeded()) {
                        ResultSet resultSet = tiers.result();
                        if (resultSet.getNumRows() == 0) {
                            result.handle(Future.failedFuture("No series found with id " + seriesId));
                        }
                        else {
                            Map<Integer, List<Tier>> fixtureTiers = new HashMap<>();
                            for (JsonObject row : resultSet.getRows()) {
                                int fixtureId = row.getInteger("FixtureID");
                                if (!fixtureTiers.containsKey(fixtureId)) {
                                    fixtureTiers.put(fixtureId, new ArrayList<>());
                                }
                                fixtureTiers.get(fixtureId).add(Tier.fromJsonObject(row));
                            }
                            result.handle(Future.succeededFuture(fixtureTiers));
                        }
                    }
                    else {
                        result.handle(Future.failedFuture(tiers.cause()));
                    }
                });
    }
}
