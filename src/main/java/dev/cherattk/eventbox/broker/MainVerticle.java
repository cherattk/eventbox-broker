package dev.cherattk.eventbox.broker;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.http.vertx.VertxMessageFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class MainVerticle extends AbstractVerticle {

//	public Future<HttpResponse<Void>> loadEventMap(String AbsoluteEventBindingURL) {
//		return webClient.requestAbs(HttpMethod.GET, AbsoluteEventBindingURL)
//						.authentication(new TokenCredentials("myBearerToken"))
//						// .as(BodyCodec.json(EventBinding.class))
//						.as(BodyCodec.jsonStream(eventManager.getEventBindingParser()))
//						.send();
//	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		WebClient webClient = WebClient.create(vertx);
		Router router = Router.router(vertx);
		EventManager eventManager = new EventManager(webClient);

		// define route
		router.post("/cloudevent").handler(routingContext -> {
			eventManager.readPublishedEvent(routingContext);
		});

		ConfigRetriever retriever = ConfigRetriever.create(vertx);
		retriever.getConfig().onComplete(ar -> {
			if (ar.failed()) {
				// Failed to retrieve the configuration
			} else {
				JsonObject config = ar.result();
				String eventBindingURL = config.getString("EVENTBOX_EVENTMAP_URL");
				Integer brokerPort = config.getInteger("EVENTBOX_BROKER_PORT");
				System.out.println(eventBindingURL);
//		    for (Iterator<Entry<String, Object>> iterator = config.iterator(); iterator.hasNext();) {
//				Entry<String, Object> data = (Entry<String, Object>) iterator.next();
//				System.out.println(data.getKey() + " -->  " + data.getValue());
//			}

				// TODO URI.create(eventMapURL)
				webClient.requestAbs(HttpMethod.GET, eventBindingURL)
						.authentication(new TokenCredentials("myBearerToken"))
						// .as(BodyCodec.json(EventBinding.class))
						.as(BodyCodec.jsonStream(eventManager.getEventBindingParser()))
						.send()
						.onSuccess(response -> {
							// JsonObject body = response.body();
							System.out.println("Received eventbinding with status code " + response.statusCode());

							///////////////////////////////////////////////////
							// create and run the http server
							///////////////////////////////////////////////////
							vertx.createHttpServer().requestHandler(router).listen(brokerPort, http -> {
								if (http.succeeded()) {
									startPromise.complete();
									System.out.println("EVENTBOX Broker started on port " + brokerPort);
								} else {
									startPromise.fail(http.cause());
								}
							});

						}).onFailure(err -> {
							System.out.println("Something went wrong " + err.getMessage());
							vertx.close();
							System.exit(0);
						});

			}
//			System.exit(0);
		});

	}
}
