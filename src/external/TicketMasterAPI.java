// sent HTTP request to TicketMaster with query and customerKey to get the response
// the search method will return the events JSONArray which is in the map of response's _embedded key's value 
package external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item.ItemBuilder;
import entity.Item;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json"; // HTTP request 
	private static final String DEFAULT_KEYWORD = "event"; // set if the keyword input is null this will replace the keyword
	private static final String API_KEY = "edRzUUS2AaMl71IrQcu2SonCfFY2306A"; // customerKey of TicketMaster
	
	public List<Item> search(double lat, double lon, String keyword) { 
        if (keyword == null) { // check if keyword is legal
        	keyword = DEFAULT_KEYWORD;
        }
        try {
			keyword = URLEncoder.encode(keyword,"UTF-8"); // encode the keyword to avoid the no unicode words such as space
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        String query = String.format("apikey=%s&latlong=%s,%s&keyword=%s&radius",API_KEY , lat , lon , keyword , 50); // create the HTTP request's query
        // add a size parameter will can change the events that search each time
        String url = URL + "?" + query; // create the full HTTP request with query
        try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();// create the connection object
			connection.setRequestMethod("GET");// set the HTTP request method , since is set so no body
			
			int responseCode = connection.getResponseCode();// return the status of response
			System.out.println("Sending request to url:" + url); 
			System.out.println("Response code" + responseCode);
			
			if(responseCode != 200) { // for ticketMasterAPI , just check if this is 200 is fine but for other need to check other status
				return new ArrayList<>();
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())); // get a stream then use Reader to read and storage in buffer with string
			// create the buffer and is assigned with the response from TicketMaster
			//connection.getInputStream() get content from the ticketMaster servlet 
			String line;
			StringBuilder response = new StringBuilder();
			while((line = reader.readLine()) != null) {	
				response.append(line); // BufferedReader are line by line so put all line in one String
			}
			reader.close();
			JSONObject obj = new JSONObject(response.toString()); // this will change the string to Hashmap structure
			if(!obj.isNull("_embedded")) {
				JSONObject embedded = obj.getJSONObject("_embedded");
				return getItemList(embedded.getJSONArray("events"));
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
		return new ArrayList<>();
	}
	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray events) throws JSONException {
		// change all the event in JSONArray to a item object which get all the needed INFO from raw JSON , and storage at a ArrayList;
		List<Item> itemList = new ArrayList<>();
		Set<String> set = new HashSet<>();
		for(int i = 0 ; i < events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			ItemBuilder builder = new ItemBuilder();
			if(!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			if (!set.add(event.getString("name"))) { // deduplicate the same name event 
				continue;
			}
			if(!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			if(!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			if(!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			if(!event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			builder.setAddress(getAddress(event));
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			Item item= builder.build();
			itemList.add(item);
		}
		return itemList;
	}
	/**
	 * Helper methods
	 */
	// method of to get INFO we need from the raw JSON ; return by String type
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			if(!embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				for(int i = 0 ; i < venues.length() ; i ++) {
					JSONObject venue = venues.getJSONObject(i);
					StringBuilder addressBuilder = new StringBuilder();
					if(!venue.isNull("address")) {
						JSONObject address = venue.getJSONObject("address");
						if(!address.isNull("line1")) {
							addressBuilder.append(address.getString("line1"));
						}
						if(!address.isNull("line2")) {
							addressBuilder.append(',');
							addressBuilder.append(address.getString("line1"));
						}
						if(!address.isNull("line3")) {
							addressBuilder.append(',');
							addressBuilder.append(address.getString("line1"));
						}
					}
					if(!venue.isNull("city")) {
						JSONObject city = venue.getJSONObject("city");
						if(!city.isNull("name")) {
							addressBuilder.append(',');
							addressBuilder.append(city.getString("name"));
						}
					}
					String addressStr = addressBuilder.toString();
					if(!addressStr.equals("")) {
						return addressStr;
					}
				}
			}
		}
		return "";
	}
	
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("images")) {
			JSONArray array = event.getJSONArray("images");
			for (int i = 0; i < array.length(); ++i) {
				JSONObject image = array.getJSONObject(i);
				if (!image.isNull("url")) {
					return image.getString("url");
				}
			}
		}
		return "";
	}
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (!event.isNull("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			for (int i = 0; i < classifications.length(); ++i) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (!segment.isNull("name")) {
						categories.add(segment.getString("name"));
					}
				}
			}
		}
		return categories;
	}


	// test
	private void queryAPI(double lat, double lon) {
		List<Item> events = search(lat, lon, null);

		for (Item event : events) {
			System.out.println(event.toJSONObject());
		}
	}

	public static void main (String arg[]) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(37.38, -122.08);
	}
}

