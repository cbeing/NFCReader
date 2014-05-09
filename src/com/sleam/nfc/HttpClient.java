package com.sleam.nfc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class HttpClient {

private static String URL = "http://192.168.0.100/update_presence.php";

public String postJsonData(String data) {
	try {
		StringBuffer buffer = new StringBuffer();
		// Apache HTTP Reqeust
		System.out.println("Sending data..");
		System.out.println("Data: [" + data + "]");
		org.apache.http.client.HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(URL);
		List<NameValuePair> nvList = new ArrayList<NameValuePair>();
		BasicNameValuePair bnvp = new BasicNameValuePair("json", data);
		// We can add more
		nvList.add(bnvp);
		post.setEntity(new UrlEncodedFormEntity(nvList));

		HttpResponse resp = client.execute(post);

		// We read the response
		InputStream is = resp.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder str = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			str.append(line + "\n");
		}

		is.close();
		buffer.append(str.toString());

		// Done!

		return buffer.toString();
	} catch (Throwable t) {
		t.printStackTrace();
	}

	return null;
}
}

