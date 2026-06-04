package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.controller.PiController;
import com.example.demo.mqtt.MqttPublisher;
import com.example.demo.service.PiStore;
import com.example.demo.service.UserStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HelloControllerTest {

    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        PiController controller = new PiController(
                Mockito.mock(PiStore.class),
                Mockito.mock(UserStore.class),
                Mockito.mock(MqttPublisher.class),
                new ObjectMapper());
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void registerWithMissingBodyReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/pi/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
