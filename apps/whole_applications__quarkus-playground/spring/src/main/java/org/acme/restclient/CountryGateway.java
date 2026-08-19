package org.acme.restclient;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Client for the public country API https://restcountries.eu (replaces the MP REST client). */
@Component
public class CountryGateway {

  private final RestClient restClient;

  public CountryGateway(RestClient.Builder builder) {
    this.restClient = builder.baseUrl("https://restcountries.eu/rest").build();
  }

  /**
   * Fetches countries by name.
   *
   * @param name the country name
   * @return matching countries
   */
  public List<Country> getByName(String name) {
    return restClient
        .get()
        .uri("/v2/name/{name}", name)
        .retrieve()
        .body(new ParameterizedTypeReference<List<Country>>() {});
  }
}
