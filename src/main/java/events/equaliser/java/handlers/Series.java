package events.equaliser.java.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import events.equaliser.java.model.event.BareSeries;
import events.equaliser.java.util.Json;
import events.equaliser.java.util.Request;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public class Series {

    public static void getId(RoutingContext context,
                             SQLConnection connection,
                             Handler<AsyncResult<JsonNode>> handler) {
        try {
            String rawId = context.request().getParam("id");
            if (rawId == null) {
                handler.handle(Future.failedFuture("'id' param missing"));
                return;
            }
            int id = Integer.parseInt(rawId);
            events.equaliser.java.model.event.Series.retrieveFromId(id, connection, data -> {
                if (data.succeeded()) {
                    events.equaliser.java.model.event.Series series = (events.equaliser.java.model.event.Series) data.result();  // TODO fix cast - caused by generics erasure issue
                    JsonNode node = Json.MAPPER.convertValue(series, JsonNode.class);
                    handler.handle(Future.succeededFuture(node));
                }
                else {
                    handler.handle(Future.failedFuture(data.cause()));
                }
            });
        }
        catch (NumberFormatException e) {
            handler.handle(Future.failedFuture("Invalid series id"));
        }
    }

    public static void getByTag(RoutingContext context,
                                SQLConnection connection,
                                Handler<AsyncResult<JsonNode>> handler) {
        HttpServerRequest request = context.request();
        try {
            String tag = Request.validateField("tag", request.getParam("tag"));
            BareSeries.retrieveFromTag(tag, connection, res -> {
                if (res.failed()) {
                    handler.handle(Future.failedFuture(res.cause()));
                    return;
                }

                List<BareSeries> series = res.result();
                JsonNode node = Json.MAPPER.convertValue(series, JsonNode.class);
                handler.handle(Future.succeededFuture(node));
            });
        } catch (IllegalArgumentException e) {
            handler.handle(Future.failedFuture(e.getMessage()));
        }
    }

    public static void getShowcase(RoutingContext context,
                                   SQLConnection connection,
                                   Handler<AsyncResult<JsonNode>> handler) {
        events.equaliser.java.model.event.Series.retrieveShowcase(connection, seriesRes -> {
            if (seriesRes.failed()) {
                handler.handle(Future.failedFuture(seriesRes.cause()));
                return;
            }

            List<BareSeries> series = seriesRes.result();
            JsonNode node = Json.MAPPER.convertValue(series, JsonNode.class);
            handler.handle(Future.succeededFuture(node));
        });
    }
}
