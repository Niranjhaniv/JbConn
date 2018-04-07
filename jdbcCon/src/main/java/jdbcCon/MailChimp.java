package jdbcCon;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MailChimp {
	public static void main(String a[]) {
		try {
			// ----------------------------Fetch Auth Token ----------------------
			String authtoken = "5ef3984f3661c25cd08a4c9f7258c7b6";
			// https://zohocrmapi.wiki.zoho.com/using-authtoken.html
			String scope = "crmapi";
			String selectColumns = "Leads(Lead Owner,First Name,Last Name,Email,Company)";
			String newFormat = "1";
			String fromIndex = "1";
			String toIndex = "50";

			JSONObject respJson = new JSONObject();
			JSONArray respJsonOperations = new JSONArray();

			String targetURL = "https://crm.zoho.com/crm/private/json/Leads/getRecords";
			String paramname = "content";
			PostMethod post = new PostMethod(targetURL);
			PostMethod postMem = null;
			post.setParameter("authtoken", authtoken);
			post.setParameter("scope", scope);
			post.setParameter("newFormat", newFormat);
			post.setParameter("selectColumns", selectColumns);
			post.setParameter("fromIndex", fromIndex);
			post.setParameter("toIndex", toIndex);
			HttpClient httpclient = new HttpClient();
			PrintWriter myout = null;
			GetMethod get = new GetMethod("https://us18.api.mailchimp.com/3.0/lists");
			get.setRequestHeader("Authorization", "Basic a2V5OjYxMThmMzAzMTUzZWI1NTFlMmRjOGVkYTI0ZGY3NTY5LXVzMTg=");
			JSONParser parser = new JSONParser();

			// String getUrl = "https://us18.api.mailchimp.com/3.0/lists";

			// Execute http request
			try {
				long t1 = System.currentTimeMillis();
				int result = httpclient.executeMethod(post);
				System.out.println("HTTP Response status code: " + result);
				System.out.println(">> Time taken " + (System.currentTimeMillis() - t1));

				// writing the response to a file
				// myout = new PrintWriter(new File("response.xml"));
				// myout.print(post.getResponseBodyAsString());

				// -----------------------Get response as a string ----------------
				String postResp = post.getResponseBodyAsString();
				System.out.println("postResp=======>" + postResp);

				int result1 = httpclient.executeMethod(get);
				System.out.println("HTTP Response get status code: " + result1);
				System.out.println(">> Time taken " + (System.currentTimeMillis() - t1));
				String getRes = get.getResponseBodyAsString();
				System.out.println("getRes=======>" + getRes);

				// ------------------Getter json Parsing to get the list id --------------

				JSONObject getJsonObj = (JSONObject) parser.parse(getRes);

				JSONArray rowlist = (JSONArray) getJsonObj.get("lists");
				String listId = null;
				for (Object list : rowlist) {
					JSONObject listData = (JSONObject) list;
					listId = (String) listData.get("id");
					break;
				}

				System.out.println(listId);

				// String listId=getJsonObj.containsKey("id") ? getJsonObj.get("id"): null;

				HashMap<String, String> map;
				List<HashMap<String, String>> listMap = new ArrayList<HashMap<String, String>>();
				try {
					JSONObject jsonObj = (JSONObject) parser.parse(postResp);
					JSONObject resObj = (JSONObject) jsonObj.get("response");
					JSONObject resultObj = (JSONObject) resObj.get("result");
					JSONObject leadObj = (JSONObject) resultObj.get("Leads");
					JSONArray rows = (JSONArray) leadObj.get("row");
					for (Object o : rows) {
						JSONObject zohoCrmData = (JSONObject) o;
						map = new HashMap<String, String>();
						JSONArray rowDet = (JSONArray) zohoCrmData.get("FL");
						for (Object ro : rowDet) {

							JSONObject row = (JSONObject) ro;

							map.put((String) row.get("val"), (String) row.get("content"));

						}
						/*-------Sync Zoho to MailChimp Code------------*/

						JSONObject fullRespJson = new JSONObject();
						JSONObject userInfo = new JSONObject();
						JSONObject mergeInfo = new JSONObject();
						mergeInfo.put("FNAME", map.get("First Name"));
						mergeInfo.put("LNAME", map.get("Last Name"));
						userInfo.put("merge_fields", mergeInfo);
						userInfo.put("email_address", map.get("Email"));
						userInfo.put("status", "subscribed");

						String memberURL = "https://us18.api.mailchimp.com/3.0/lists/" + listId + "/members";
						postMem = new PostMethod(memberURL);
						postMem.setRequestHeader("Authorization",
								"Basic a2V5OjYxMThmMzAzMTUzZWI1NTFlMmRjOGVkYTI0ZGY3NTY5LXVzMTg=");
						postMem.setParameter("fromIndex", fromIndex);
						postMem.setRequestBody(userInfo.toString());
						int insertMem = httpclient.executeMethod(postMem);

						String postMemRes = postMem.getResponseBodyAsString();
						System.out.println("postMemRes=======>" + postMemRes);

						// listMap.add(map);
					}

					// ObjectMapper objectMapper = new ObjectMapper();
					// String json = objectMapper.writeValueAsString(listMap);

					// System.out.println(json);

				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// myout.close();
				post.releaseConnection();
				get.releaseConnection();
				postMem.releaseConnection();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}