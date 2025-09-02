package com.example.emr_server.service.urpl;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class UrplClient {

    private final RestTemplate restTemplate;

    public UrplClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UrplPageResponse fetchPage(int page, int size) {
        URI uri = UriComponentsBuilder
                .fromUriString("https://rejestry.ezdrowie.gov.pl/api/rpl/medicinal-products/search/public")
                .queryParam("subjectRolesIds", 1)
                .queryParam("isAdvancedSearch", false)
                .queryParam("size", size)
                .queryParam("page", page)
                .queryParam("sort", "name,ASC")
                .build(true)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        RequestEntity<Void> req = new RequestEntity<>(headers, HttpMethod.GET, uri);
        ResponseEntity<UrplPageResponse> resp = restTemplate.exchange(req, UrplPageResponse.class);
        return resp.getBody();
    }
}

