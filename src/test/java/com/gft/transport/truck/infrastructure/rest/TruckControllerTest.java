package com.gft.transport.truck.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.transport.truck.application.dto.CreateTruckRequest;
import com.gft.transport.truck.application.dto.TruckResponse;
import com.gft.transport.truck.application.usecase.GetTrucks;
import com.gft.transport.truck.application.usecase.RegisterTruck;
import com.gft.transport.truck.domain.TruckStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TruckController.class)
class TruckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterTruck registerTruck;

    @MockitoBean
    private GetTrucks getTrucks;

    @Test
    void postTrucks_returns201WithTruckResponse() throws Exception {
        TruckResponse response = new TruckResponse(UUID.randomUUID().toString(), "Truck 01", new TruckResponse.LocationDto(0, 0), TruckStatus.AVAILABLE);
        when(registerTruck.execute(any())).thenReturn(response);

        mockMvc.perform(post("/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTruckRequest("Truck 01", 0, 0, 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Truck 01"))
                .andExpect(jsonPath("$.location.x").value(0))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void postTrucks_returns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTruckRequest("", 0, 0, 10))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postTrucks_returns400WhenCapacityIsZero() throws Exception {
        mockMvc.perform(post("/trucks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTruckRequest("Truck 01", 0, 0, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTrucks_returns200WithTruckList() throws Exception {
        List<TruckResponse> trucks = List.of(
                new TruckResponse(UUID.randomUUID().toString(), "Truck 01", new TruckResponse.LocationDto(0, 0), TruckStatus.AVAILABLE),
                new TruckResponse(UUID.randomUUID().toString(), "Truck 02", new TruckResponse.LocationDto(3, 5), TruckStatus.IN_TRANSIT)
        );
        when(getTrucks.execute()).thenReturn(trucks);

        mockMvc.perform(get("/trucks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Truck 01"))
                .andExpect(jsonPath("$[1].location.x").value(3))
                .andExpect(jsonPath("$[1].name").value("Truck 02"));
    }

    @Test
    void getTrucks_returns200WithEmptyListWhenNoTrucks() throws Exception {
        when(getTrucks.execute()).thenReturn(List.of());

        mockMvc.perform(get("/trucks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
