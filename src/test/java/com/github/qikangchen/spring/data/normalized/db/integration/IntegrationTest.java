package com.github.qikangchen.spring.data.normalized.db.integration;

import com.github.qikangchen.spring.data.normalized.db.annotation.DataMysqlTest;
import com.github.qikangchen.spring.data.normalized.db.data.Incident;
import com.github.qikangchen.spring.data.normalized.db.data.MatchedItem;
import com.github.qikangchen.spring.data.normalized.db.data.Request;
import com.github.qikangchen.spring.data.normalized.db.data.RequestLocalInfo;
import com.github.qikangchen.spring.data.normalized.db.database.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@DataMysqlTest
public class IntegrationTest {

    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private RequestLocalInfoRepository requestLocalInfoRepository;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private MatchedItemRepository matchedItemRepository;

    private MyRepo myRepo;

    @BeforeEach
    void setUp(){
        myRepo = new MyRepo(requestRepository, requestLocalInfoRepository);
    }

    @Test
    void test() throws JsonProcessingException {

        // On startup: Add ctiy information
        RequestLocalInfo requestLocalInfoBerlin = new RequestLocalInfo();
        requestLocalInfoBerlin.setCityName("Berlin");
        requestLocalInfoBerlin.setCentreLongitude(111);
        requestLocalInfoBerlin.setCentreLatitude(200);
        requestLocalInfoBerlin.setSearchRadiusInKm(22);

        RequestLocalInfo requestLocalInfoMunich = new RequestLocalInfo();
        requestLocalInfoMunich.setCityName("Muenchen");
        requestLocalInfoMunich.setCentreLongitude(222);
        requestLocalInfoMunich.setCentreLatitude(333);
        requestLocalInfoMunich.setSearchRadiusInKm(33);

        myRepo.insertLocalInfo(requestLocalInfoBerlin);
        myRepo.insertLocalInfo(requestLocalInfoMunich);


        // Insert request
        RequestLocalInfo requestLocalInfoBerlinFromDb = myRepo.getRequestLocalInfoFromCityName("Berlin");

        Incident incidentHere = DummyData.getIncidentDummy();
        incidentHere.setProvider(Incident.Provider.HERE);

        Incident incidentTomTom = DummyData.getIncidentDummy();
        incidentTomTom.setProvider(Incident.Provider.TOMTOM);

        MatchedItem matchedItem = new MatchedItem();
        matchedItem.setHereIncident(incidentHere);
        matchedItem.setTomtomIncident(incidentTomTom);
        matchedItem.setConfidenceLevel(4);

        Request request = new Request();
        request.addIncident(incidentHere);
        request.addIncident(incidentTomTom);
        request.addMatchedItem(matchedItem);
        request.setRequestLocalInfo(requestLocalInfoBerlinFromDb);
        request.setRequestTimeStamp(2000);
        myRepo.insertRequest(request);


        // Get request
        Request requestFromDb = myRepo.getRequest("Berlin", 2000);
        assertThat(requestFromDb, equalTo(request));

        assertThat(requestLocalInfoRepository.count(), equalTo(2L));
        assertThat(requestRepository.count(), equalTo(1L));
        assertThat(incidentRepository.count(), equalTo(2L));
        assertThat(matchedItemRepository.count(), equalTo(1L));


        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestFromDb.getRequestLocalInfo());
        System.out.println(getPrettyJson(json));
    }

    public static String getPrettyJson(String rawJson){
        try {
            JSONObject jsonObject = new JSONObject(rawJson);
            return jsonObject.toString(3);
        } catch (JSONException e) {
            throw new IllegalStateException("Can't prettify this json: " + rawJson);
        }
    }

}
