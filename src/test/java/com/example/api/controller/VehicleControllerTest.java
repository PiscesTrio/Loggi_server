package com.example.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.api.handler.GlobalExceptionHandler;
import com.example.api.handler.GlobalResponseHandler;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.enums.VehicleType;
import com.example.api.security.SecurityConfiguration;
import com.example.api.service.VehicleService;
import com.example.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Creating a vehicle, which nothing tested until {@code type} stopped being a String.
 *
 * <p>The field carried {@code @NotBlank}. Blankness is a property of a CharSequence, so once the
 * field became a {@link VehicleType} the constraint had no validator — and Hibernate Validator does
 * not ignore a constraint it cannot apply, it throws {@code UnexpectedTypeException} the first time
 * a request is validated. Every create would have answered 500. Nothing failed to compile, no unit
 * test posted a vehicle, and the only visible symptom was a {@code minLength: 1} on an enum in the
 * generated OpenAPI document.
 *
 * <p>Hence a test that posts one. The first case is red against {@code @NotBlank} and green against
 * {@code @NotNull}; the second pins that the constraint still does its job.
 */
@WebMvcTest(VehicleController.class)
@Import({SecurityConfiguration.class, GlobalResponseHandler.class, GlobalExceptionHandler.class})
class VehicleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtTokenUtil jwtTokenUtil;
    @MockitoBean VehicleService vehicleService;

    @Test
    @WithMockUser
    @DisplayName("A vehicle with a valid type is created")
    void createsVehicle() throws Exception {
        Vehicle saved = new Vehicle();
        saved.setId("v1");
        saved.setNumber("品川800へ12-34");
        saved.setType(VehicleType.LIGHT_TRUCK);
        when(vehicleService.save(any())).thenReturn(saved);

        mockMvc.perform(
                        post("/api/vehicle")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"number\":\"品川800へ12-34\",\"type\":\"LIGHT_TRUCK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("LIGHT_TRUCK"));
    }

    @Test
    @WithMockUser
    @DisplayName("A vehicle with no type is rejected as a bad request, not a server error")
    void rejectsMissingType() throws Exception {
        mockMvc.perform(
                        post("/api/vehicle")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"number\":\"品川800へ12-34\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("A type outside the closed set is rejected, rather than stored")
    void rejectsUnknownType() throws Exception {
        // What the enum buys. The column used to take any string the client sent, so a typo
        // stored happily and every later comparison against it took the wrong branch.
        mockMvc.perform(
                        post("/api/vehicle")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"number\":\"品川800へ12-34\",\"type\":\"货车\"}"))
                .andExpect(status().isBadRequest());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
        return org.springframework.security.test.web.servlet.request
                .SecurityMockMvcRequestPostProcessors.csrf();
    }
}
