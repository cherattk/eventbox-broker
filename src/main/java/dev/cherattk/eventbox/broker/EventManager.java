package dev.cherattk.eventbox.broker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.cloudevents.CloudEvent;
import io.cloudevents.http.vertx.VertxMessageFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class EventManager {

	private Map<String, JsonArray> eventBinding = new HashMap<String, JsonArray>();

	private WebClient webClient;

	public EventManager(WebClient client) {
		this.webClient = client;
	}

	public void readPublishedEvent(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		VertxMessageFactory.createReader(request).onSuccess(messageReader -> {
			CloudEvent event = messageReader.toEvent();
			System.out.println("received : " + event.toString());
			DeliverEvent(event);
			routingContext.response().setStatusCode(200).end();
		}).onFailure(error -> {
			System.out.println(error.getMessage());
			routingContext.response().setStatusCode(400).end();
		});
	}

	/**
	 * TODO : will change to accept only CloudEvent Object as parameter
	 * 
	 * @param ceSource
	 * @param ceType
	 * @return
	 */
	public String getCloudeventkey(String ceSource, String ceType) {
		return ceSource + "," + ceType;
	}

	public void DeliverEvent(CloudEvent ceEvent) {
		String eventKey = getCloudeventkey(ceEvent.getSource().toString(), ceEvent.getType());
		System.out.println("published event : " + eventKey);

		if (eventBinding.containsKey(eventKey)) {
			JsonArray listenerArray = eventBinding.get(eventKey);
			if (listenerArray != null) {
				String absoluteListenerUrl = "";
				for (Iterator<Object> iterator = listenerArray.iterator(); iterator.hasNext();) {
					JsonObject listener = (JsonObject) iterator.next();
					absoluteListenerUrl = listener.getString("url");
					System.out.println("listener url : " + absoluteListenerUrl);
//			System.out.println();
				}
			}
			else {
				System.out.println("=== event with key ["+ eventKey + "] has not listeners === ");
			}
		} else {
			System.out.println("=== event with key ["+ eventKey + "] does not exists === ");
		}

//		VertxMessageFactory.createWriter(webClient.postAbs(absoluteListenerUrl))
//				.writeStructured(event, "application/cloudevents+json").onSuccess(response -> {
//					// Logg Success event into database
//				}).onFailure(error -> {
//					System.out.println(error.getMessage());
//					// Logg Fail event into database
//				});
	}

	/**
	 * Define a handler to parse the loaded {event,listener} binding map
	 */
	public JsonParser getEventBindingParser() {
		JsonParser eventBindingParser = JsonParser.newParser().arrayValueMode();
		eventBindingParser.handler(event -> {
			JsonArray object = event.arrayValue();
			if (object != null) {
				System.out.println("======== start parsing json ========");
				// for (Entry<String, Object> entry : object) {
				for (Object entry : object) {
					JsonObject jsonObj = (JsonObject) entry;
					JsonObject ceEvent = jsonObj.getJsonObject("event");
					String ceKey = getCloudeventkey(ceEvent.getString("source"), ceEvent.getString("type"));
					eventBinding.put(ceKey, jsonObj.getJsonArray("listeners"));

//					System.out.println(jsonObj.getJsonObject("event").getString("id"));
//					System.out.println(jsonObj.getJsonArray("listeners"));
				}
				System.out.println("======== end parsing json ========");
			}
		});

		return eventBindingParser;
	}

}
