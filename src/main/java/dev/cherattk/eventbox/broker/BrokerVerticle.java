package dev.cherattk.eventbox.broker;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.vertx.core.parsetools.JsonParser;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.http.vertx.VertxMessageFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class BrokerVerticle extends AbstractVerticle {
	
	private static final String EVENTBOX_ADMIN_BINDINGMAP_ENDPOINT = System.getenv("EVENTBOX_ADMIN_BINDINGMAP_ENDPOINT");

	private static final String EVENTBOX_BROKER_PORT = System.getenv("EVENTBOX_BROKER_PORT");

	private static final String EVENTBOX_BROKER_HOST = System.getenv("EVENTBOX_BROKER_HOST");

	private static final String EVENTBOX_BROKER_AUTH_TOKEN = System.getenv("EVENTBOX_BROKER_AUTH_TOKEN");
	
	public void stopApplication() {
		vertx.close();
		System.exit(0);
	}
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {
        
		WebClient webClient = WebClient.create(vertx);
		Router router = Router.router(vertx);
		
		EventManager eventManager = new EventManager(webClient);
		
		// define route
		router.post("/cloudevent").handler(routingContext -> {
			try {
				eventManager.readPublishedEvent(routingContext);				
			} catch (Throwable e) {
				routingContext.response().setStatusCode(400).end();
			}
		});

		JsonParser parser = eventManager.getEventBindingParser();
		parser.exceptionHandler(throwable -> {
			System.err.println("Fail Parsing Event/Listener Binding Map, Caused By: " 
								+ throwable.getMessage());
			stopApplication();
		});
		
		BodyCodec<Void> bodyCodec = BodyCodec.jsonStream(parser);
		
		System.out.println();
		System.out.println("======================== EVENTBOX BROKER ========================");
		
		webClient.requestAbs(HttpMethod.GET, EVENTBOX_ADMIN_BINDINGMAP_ENDPOINT)
				.authentication(new TokenCredentials(EVENTBOX_BROKER_AUTH_TOKEN))
				.as(bodyCodec)
				.send()
				.onSuccess(response -> {
					///////////////////////////////////////////////////
					// create and run the http server
					///////////////////////////////////////////////////
					int brokerPort = Integer.parseInt(EVENTBOX_BROKER_PORT);
					String brokerHost = EVENTBOX_BROKER_HOST;
					HttpServer server = vertx.createHttpServer();
					server.requestHandler(router);
					server.listen(brokerPort, brokerHost , http -> {
						if (http.succeeded()) {
							startPromise.complete();
							System.out.println("EVENTBOX-BROKER started at " +  brokerHost + ":" + brokerPort);
							System.out.println();
						} else {
							System.err.println();
							startPromise.fail("Error launching http server, Caused By :" 
												+ http.cause().getMessage());
							stopApplication();
						}
					});
				}).onFailure(err -> {
					System.err.println();
					startPromise.fail("Unable to connect to EVENTBOX-ADMIN Server - "
										+ "Check that EVENTBOX-ADMIN Server is running "
										+ "before starting the Borker, Caused By : " 
										+ err.getMessage());
					stopApplication();
				});

	}
}
