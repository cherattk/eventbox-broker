package dev.cherattk.eventbox.broker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.cloudevents.CloudEvent;
import io.cloudevents.http.vertx.VertxMessageFactory;
import io.cloudevents.rw.CloudEventRWException;
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

	private Map<String, JsonArray> eventBindingMap = new HashMap<String, JsonArray>();

	private WebClient webClient;

	private static final String CLOUDEVENT_CONTENT_TYPE_HEADER = "application/cloudevents+json";

	public EventManager(WebClient client) {
		this.webClient = client;
	}

	public void readPublishedEvent(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		VertxMessageFactory.createReader(request).onSuccess(messageReader -> {

			try {

				// Receive event
				CloudEvent ceEvent = messageReader.toEvent();

				// event validation
				String eventKey = getEventKey(ceEvent.getSource().toString(), ceEvent.getType());
				if (!eventBindingMap.containsKey(eventKey)) {
					JsonObject response = new JsonObject();
					response.put("message", "cloudevent with theses attributes does note exist : " + "source:"
							+ ceEvent.getSource() + "," + "type:" + ceEvent.getType());
					routingContext.response().setStatusCode(404).putHeader("Content-Type", "application/json")
							.end(response.encode());
					return;
				}

				JsonArray listenerArray = eventBindingMap.get(eventKey);
				if (listenerArray.isEmpty()) {
					JsonObject response = new JsonObject();
					response.put("message", "cloudevent with theses attribute has not listener" + " source :"
							+ ceEvent.getSource() + " , " + " type : " + ceEvent.getType());
					routingContext.response().setStatusCode(404).putHeader("Content-Type", "application/json")
							.end(response.encode());
					return;
				}

				// everything is OK
				notifyListener(ceEvent);
				routingContext.response().setStatusCode(200).end();

			} catch (Exception error) {
				System.out.println(error.getMessage());
				routingContext.response().setStatusCode(400).end();
			}

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
	public String getEventKey(String ceSource, String ceType) {
		return ceSource + "," + ceType;
	}

	public void pushHttpNotification(CloudEvent ceEvent, String absoluteListenerEndpoint) {

		VertxMessageFactory.createWriter(webClient.postAbs(absoluteListenerEndpoint))
				.writeStructured(ceEvent, CLOUDEVENT_CONTENT_TYPE_HEADER).onSuccess(response -> {
					// TODO : Log Success into database
					System.out.println("successfully sent event to listener endpoint : " + absoluteListenerEndpoint);
				}).onFailure(error -> {
					System.out.println(error.getMessage());
					// TODO : Log Failure into database
					System.out.println("Fail when sending event to listener endpoint : " + absoluteListenerEndpoint);
				});
	}

	public void notifyListener(CloudEvent ceEvent) {

		String eventKey = getEventKey(ceEvent.getSource().toString(), ceEvent.getType());
		JsonArray listenerArray = eventBindingMap.get(eventKey);
		String absoluteListenerUrl = "";
		for (Iterator<Object> iterator = listenerArray.iterator(); iterator.hasNext();) {
			JsonObject listener = (JsonObject) iterator.next();
			absoluteListenerUrl = listener.getString("url");
			pushHttpNotification(ceEvent, absoluteListenerUrl);
		}

	}

	/**
	 * Define a handler to parse the loaded {event,listener} binding map
	 */
	public JsonParser getEventBindingParser() {
		JsonParser eventBindingParser = JsonParser.newParser().arrayValueMode();
		eventBindingParser.handler(event -> {
			JsonArray object = event.arrayValue();
				if (object != null) {
					for (Object entry : object) {
						JsonObject jsonObj = (JsonObject) entry;
						JsonObject ceEvent = jsonObj.getJsonObject("event");
						String ceKey = getEventKey(ceEvent.getString("source"), ceEvent.getString("type"));
						eventBindingMap.put(ceKey, jsonObj.getJsonArray("listeners"));
					}
					System.out.print("INFO: ");
					System.out.println("Successfully loading Event/Listener Binding");
					if(eventBindingMap.size() == 0) {
						System.out.println("================= Warning =================");
						System.out.println("Event/Listener Map is empty");
						System.out.println("You need to "
								+ "Bind Listener to Event in "
								+ "EVENTBOX-ADMIN Board AND restart The BROKER");
						System.out.println("===========================================");
					}
				}
		});

		return eventBindingParser;
	}

}
