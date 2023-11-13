package com.tseday.advert;

import com.facebook.ads.sdk.APIContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.StringReader;

@SpringBootApplication
public class AdvertApplication {

	@Value("${meta.accessToken}")
	String accessToken;

	@Value("${meta.appSecret}")
	String appSecret;

	@Value("${meta.appId}")
	String appId;

	public static void main(String[] args) {
		SpringApplication.run(AdvertApplication.class, args);
	}


	@Bean
	public APIContext metaApiContext() {
		return new APIContext(accessToken, appSecret).enableDebug(true);
	}

	@Bean
	public ObjectMapper customObjectMapper() {
		return JsonMapper.builder()
				.addModule(new JavaTimeModule())
				.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
				.build();
	}

	@Bean
	public StringTemplate.Processor<JsonObject, RuntimeException> jsonProcessor () {
		return StringTemplate.Processor.of(
				(StringTemplate stJSON) -> {
					try (JsonReader jsonReader = Json.createReader(new StringReader(
							stJSON.interpolate()))) {
						return jsonReader.readObject();
					}
				}
		);
	}
}
