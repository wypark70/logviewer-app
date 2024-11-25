package org.atsoft.logviewerapp.controller;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogViewerControllerTest {

    @Test
    void hello() throws UnirestException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.get("http://localhost:8080/api/hello?action=hello3").asString();
        System.out.println( response.getCode());
        System.out.println(response.getBody());
   }
}