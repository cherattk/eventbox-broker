package dev.cherattk.eventbox.broker;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.http.vertx.VertxMessageFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class MainVerticle extends AbstractVerticle {

	Map<String, JsonObject> eventMap = new HashMap<String, JsonObject>();

	WebClient client;
	Router router;

	public void ReadEvent(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		VertxMessageFactory.createReader(request)
		.onSuccess(messageReader -> {
			CloudEvent event = messageReader.toEvent();
			// System.out.println("received : " + event.toString());
			DeliverEvent(event);
		}).onFailure(error  -> {
			System.out.println(error.getMessage());
			routingContext.response().setStatusCode(400).end();
		});
	}

	public void DeliverEvent(CloudEvent event) {
		VertxMessageFactory.createWriter(client.postAbs("http://localhost:8080"))
		.writeStructured(event , "application/cloudevents+json")
		.onSuccess(response -> {
			// Logg Success event into database
		}).onFailure(error -> {
			System.out.println(error.getMessage());
			// Logg Fail event into database
		});
	}
	

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		client = WebClient.create(vertx);
		router = Router.router(vertx);

		router.post("/cloudevent").handler(routingContext -> {
			ReadEvent(routingContext);
		});

		//////////////////////////////////////////////////
		// Parse the loaded event/listener binding map
		///////////////////////////////////////////////////
		JsonParser parser = JsonParser.newParser().objectValueMode();
		parser.handler(event -> {
			JsonObject object = event.objectValue();
			if (object != null) {
				System.out.println("======== start ========");
				for (Entry<String, Object> entry : object) {
					eventMap.put(entry.getKey(), (JsonObject) entry.getValue());
				}
				System.out.println("======== end ========");
			}
		});

		//////////////////////////////////////////////////
		// Load EventBinding Map before running a server
		///////////////////////////////////////////////////
		// TODO URI.create("http://localhost:8080/eventbinding")
		final String AbsoluteEventBindingURL = "http://localhost:8080/eventbinding";
		client.requestAbs(HttpMethod.GET, AbsoluteEventBindingURL)
				.authentication(new TokenCredentials("myBearerToken"))
				// .as(BodyCodec.json(EventBinding.class))
				.as(BodyCodec.jsonStream(parser)).send().onSuccess(response -> {
					// JsonObject body = response.body();
					System.out.println("Received response with status code " + response.statusCode() 
					+ " with body " + response.body());

					if(response.body() != null) {
						///////////////////////////////////////////////////
						// create and run the http server
						///////////////////////////////////////////////////
						vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
							if (http.succeeded()) {
								startPromise.complete();
								System.out.println("HTTP server started on port 8888");
							} else {
								startPromise.fail(http.cause());
							}
						});
					}
					else {
						System.out.println("Something went wrong configuration from from EventBox-Admin is empty");
						vertx.close();
						System.exit(0);
					}

					
					
				}).onFailure(err -> {
					System.out.println("Something went wrong " + err.getMessage());
					vertx.close();
					System.exit(0);
				});
	}
}
