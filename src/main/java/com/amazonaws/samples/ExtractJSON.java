package com.amazonaws.samples;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.ExecutionContext;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ExtractJSON {

	/**
	 * Extracts the JSON objects from a file given as an argument.
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Please provide a filename to parse.");
			return;
		}

		try (FileReader input = new FileReader(args[0])){

			JsonArray topElement = new JsonParser().parse(input).getAsJsonArray();

			for (int i = 0; i < topElement.size(); i++) {
				// Send each sub-element to ElasticSearch
				System.out.println(topElement.get(i).toString());
				putDocument("link", i, topElement.get(i).toString());
			}
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file: " + args[0]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	// Code sample from https://github.com/aws/aws-sdk-java/issues/861
	
	private static final String REGION = "us-east-1"; // region that your ES cluster is deployed to eg "us-west-2";
	private static final String ES_ENDPOINT = "https://search-comp327nnd-ujpccvnyq4wv5lwk3hpdzmrmii.us-east-1.es.amazonaws.com/"; // endpoint of your ES cluster eg "http://blah-asdflasjfd.us-west-2.es.amazon.com"
	private static final AWSCredentials CREDENTIALS = new DefaultAWSCredentialsProviderChain().getCredentials();

	public static void putDocument(String index, int id, String jsonPayload) {
		Request<?> request = new DefaultRequest<Void>("es");
		request.setContent(new ByteArrayInputStream(jsonPayload.getBytes()));
		request.setEndpoint(URI.create(ES_ENDPOINT + "/" + index + "/" + id));
		request.setHttpMethod(HttpMethodName.POST);

		AWS4Signer signer = new AWS4Signer();
		signer.setRegionName("us-east-1");
		signer.setServiceName("es");
		signer.sign(request, CREDENTIALS);

		AmazonHttpClient client = new AmazonHttpClient(new ClientConfiguration());

		client.execute(request, new DummyHandler<>(new AmazonWebServiceResponse<Void>()), new DummyHandler<>(new AmazonServiceException("oops")), new ExecutionContext(true));
	}

	public static class DummyHandler<T> implements HttpResponseHandler<T> {
		private final T preCannedResponse;
		public DummyHandler(T preCannedResponse) { this.preCannedResponse = preCannedResponse; }

		@Override
		public T handle(HttpResponse response) throws Exception {
			System.out.println(IOUtils.toString(response.getContent()));
			return preCannedResponse;
		}

		@Override
		public boolean needsConnectionLeftOpen() { return false; }
	}
}
