package dev.cherattk.eventbox.broker;

import io.vertx.core.parsetools.JsonParser;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class BrokerVerticle extends AbstractVerticle {
	
	private static final String EVENTBOX_BROKER_HOST = "127.0.0.1";
	
	private static final int EVENTBOX_BROKER_PORT = 80;
	
	private static final String API_MAP_ENDPOINT = "/api/eventbinding";

	private static final String EVENTBOX_ADMIN_HOST = System.getenv("EVENTBOX_ADMIN_HOST");

//	private static final String EVENTBOX_BROKER_AUTH_TOKEN = System.getenv("EVENTBOX_BROKER_AUTH_TOKEN");
	
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
		System.out.println("================= EventBox Broker =========");
		
		
		String eventbox_amdin_api_endpoint = EVENTBOX_ADMIN_HOST + API_MAP_ENDPOINT;
		System.out.println("Trying the reach EventBox/Admin at : " + eventbox_amdin_api_endpoint);
		
		webClient.requestAbs(HttpMethod.GET, eventbox_amdin_api_endpoint)
//				.authentication(new TokenCredentials(EVENTBOX_BROKER_AUTH_TOKEN))
				.as(bodyCodec)
				.send()
				.onSuccess(response -> {
					///////////////////////////////////////////////////
					// create and run the http server
					///////////////////////////////////////////////////
					HttpServer server = vertx.createHttpServer();
					server.requestHandler(router);
					server.listen(EVENTBOX_BROKER_PORT, EVENTBOX_BROKER_HOST , http -> {
						if (http.succeeded()) {
							startPromise.complete();
							System.out.println("EVENTBOX-BROKER started at " +  
							EVENTBOX_BROKER_HOST + ":" + EVENTBOX_BROKER_PORT);
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
