package com.drillster.api2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import com.drillster.api2.general.Error;
import com.drillster.api2.general.Response;
import com.drillster.api2.message.json.jackson.JacksonMarshaller;


/**
 *	Provides the base class for the Drillster API SDK.  See http://drill.st/api
 *	for more information about the API.  Go to http://www.drillster.com for
 *	more information about Drillster.
 *
 *	@author Tom van den Berge
 *	@author Thomas Goossens
 *	Provided by Drillster BV, dual licensed under MIT and GPL.
 */
public class Api {

	public enum HttpMethod {
		GET, POST, PUT, DELETE;
	}

	private static final Logger LOG = Logger.getLogger(Api.class);
	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

	private String oAuthToken;
	private String hostName = "www.drillster.com";
	private Integer port = 443;
	private String scheme = "https";
	private CloseableHttpClient httpClient;
	private HttpHost targetHost;
	private JacksonMarshaller jsonMarshaller;

	public String getHostName() {
		return this.hostName;
	}

	public Integer getPort() {
		return this.port;
	}

	public String getScheme() {
		return this.scheme;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setOAuthToken(String oAuthToken) {
		this.oAuthToken = oAuthToken;
	}

	/**
	 * Overrides the default http client that is used to send the requests to
	 * Drillster.
	 */
	public void setHttpClient(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	private synchronized void init() {
		if (this.targetHost != null) {
			return;
		}
		if (this.httpClient == null) {
			this.httpClient = HttpClients.createDefault();
		}
		this.targetHost = new HttpHost(this.hostName, this.port, this.scheme);
		this.jsonMarshaller = new JacksonMarshaller();
	}

	/**
	 * Sends a POST request with url-encoded parameters in the body. The JSON
	 * response is parsed into the specified response type, and returned.
	 */
	public <T extends com.drillster.api2.general.Response> T sendPostRequest(String url, List<NameValuePair> parameters,
			Class<T> responseType) throws ApiException {
		return sendPostRequest(url, parameters, responseType, this.oAuthToken);

	}
	
	/**
	 * Sends a POST request with url-encoded parameters in the body. The JSON
	 * response is parsed into the specified response type, and returned.
	 * 
	 * @param oAuthToken
	 *            the OAuth authentication token to include in the request, or
	 *            {@code null} to include no OAuth token.
	 */
	public <T extends com.drillster.api2.general.Response> T sendPostRequest(String url, List<NameValuePair> parameters,
			Class<T> responseType, String oAuthToken) throws ApiException {
		return sendPostRequest(url, parameters, responseType, Error.class, oAuthToken);
	}

	public <T extends com.drillster.api2.general.Response> T sendPostRequest(String url, List<NameValuePair> parameters,
			Class<T> responseType, Class<? extends Error> errorType) throws ApiException {
		return sendPostRequest(url, parameters, responseType, errorType, this.oAuthToken);
	}

	public <T extends com.drillster.api2.general.Response> T sendPostRequest(String url, List<NameValuePair> parameters,
			Class<T> responseType, Class<? extends Error> errorType, String oAuthToken) throws ApiException {
		init();
		return deserializeFromJson(sendRequestInternal(url, new UrlEncodedFormEntity(parameters, Consts.UTF_8), HttpMethod.POST, oAuthToken),
				responseType, errorType);
	}

	/**
	 * Sends a POST request with url-encoded parameters in the body. No response is expected.
	 */
	public void sendPostRequest(String url, List<NameValuePair> parameters) throws ApiException {
		sendPostRequest(url, parameters, this.oAuthToken);
	}

	/**
	 * Sends a POST request with url-encoded parameters in the body. No response
	 * is expected.
	 * 
	 * @param oAuthToken
	 *            the OAuth authentication token to include in the request, or
	 *            {@code null} to include no OAuth token.
	 */
	public void sendPostRequest(String url, List<NameValuePair> parameters, String oAuthToken) throws ApiException {
		init();
		sendRequestInternal(url, new UrlEncodedFormEntity(parameters, Consts.UTF_8), HttpMethod.POST, oAuthToken);
	}

	/**
	 * Sends a Get request to the specified url. The JSON response is parsed into the specified response type, and returned. 
	 */
	public <T extends com.drillster.api2.general.Response> T sendGetRequest(String url, Class<T> responseType) throws ApiException {
		return sendGetRequest(url, responseType, this.oAuthToken);
	}

	/**
	 * Sends a Get request to the specified url. The JSON response is parsed
	 * into the specified response type, and returned.
	 * 
	 * @param oAuthToken
	 *            the OAuth authentication token to include in the request, or
	 *            {@code null} to include no OAuth token.
	 */
	public <T extends com.drillster.api2.general.Response> T sendGetRequest(String url, Class<T> responseType, String oAuthToken) throws ApiException {
		init();
		return deserializeFromJson(sendRequestInternal(url, (String)null, HttpMethod.GET, oAuthToken), responseType);
	}



	// --------

	public <T> T deserializeFromJson(HttpResponse response, Class<T> responseType) throws ApiException {
		return deserializeFromJson(response, responseType, Error.class);
	}

	public <T> T deserializeFromJson(HttpResponse response, Class<T> responseType, Class<? extends Error> errorType) throws ApiException {
		try {
			String responseBody = EntityUtils.toString(response.getEntity());
			LOG.debug("Received:\n" + responseBody);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new UnsuccessfulRequestException(deserializeFromJson(responseBody, errorType));
			}
			return deserializeFromJson(responseBody, responseType);
		} catch (ParseException e) {
			throw new ApiException(e);
		} catch (IOException e) {
			throw new ApiException(e);
		}

	}

	@SuppressWarnings("unchecked")
	public <T> T deserializeFromJson(String json, Class<T> responseType) throws ApiException {
		init();
		try {
			if (responseType.equals(Response.class)) {
				return (T) this.jsonMarshaller.unmarshal(json);
			}
			return this.jsonMarshaller.unmarshal(json, responseType);
		} catch (JsonParseException jpx) {
			throw new ApiException(jpx);
		} catch (JsonMappingException jmx) {
			throw new ApiException(jmx);
		} catch (IOException iox) {
			throw new ApiException(iox);
		}
	}

	private HttpResponse sendRequestInternal(String url, String content, HttpMethod method, String oAuthToken) throws ApiException {
		LOG.debug("Sending request to " + url + ". Content:\n" + content);

		HttpRequest request;
		if (method == HttpMethod.GET) {
			request = new HttpGet(url);
		} else {
			try {
				if (method == HttpMethod.PUT) {
					HttpPut put = new HttpPut(url);
					put.setEntity(new StringEntity(content, FORM_CONTENT_TYPE, "UTF-8"));
					request = put;
				} else if (method == HttpMethod.POST) {
					HttpPost post = new HttpPost(url);
					if (content != null) {
						post.setEntity(new StringEntity(content, FORM_CONTENT_TYPE, "UTF-8"));
					}
					request = post;
				} else {
					throw new IllegalArgumentException();
				}
			} catch (UnsupportedEncodingException e) {
				throw new ApiException(e);
			}
		}
		return sendRequestInternal(request, oAuthToken);
	}

	private HttpResponse sendRequestInternal(String url, HttpEntity content, HttpMethod method, String oAuthToken) throws ApiException {
		LOG.debug("Sending request to " + url + ". Content:\n" + content);

		HttpRequest request;
		if (method == HttpMethod.GET) {
			request = new HttpGet(url);
		} else {
			if (method == HttpMethod.PUT) {
				HttpPut put = new HttpPut(url);
				put.setEntity(content);
				request = put;
			} else if (method == HttpMethod.POST) {
				HttpPost post = new HttpPost(url);
				post.setEntity(content);
				request = post;
			} else {
				throw new IllegalArgumentException();
			}
		}
		return sendRequestInternal(request, oAuthToken);
	}

	private HttpResponse sendRequestInternal(HttpRequest httpRequest, String oAuthToken) throws ApiException {
		if (oAuthToken != null) {
			httpRequest.addHeader("Authorization", "OAuth " + oAuthToken);
		}
		try {
			return this.httpClient.execute(this.targetHost, httpRequest);
		} catch (ClientProtocolException e) {
			throw new ApiException(e);
		} catch (IOException e) {
			throw new ApiException(e);
		}
	}
}
