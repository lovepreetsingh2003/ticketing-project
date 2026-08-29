package com.ai.ticketingApp.core.servlets;

import com.ai.ticketingApp.core.services.TicketUserService;
import com.ai.ticketingApp.core.testcontext.AppAemContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class TicketUsersServletTest {

    private final AemContext context = AppAemContext.newAemContext();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TicketUserService ticketUserService;
    private TicketUsersServlet servlet;

    @BeforeEach
    void setUp() {
        ticketUserService = Mockito.mock(TicketUserService.class);
        context.registerService(TicketUserService.class, ticketUserService);
        servlet = context.registerInjectActivateService(new TicketUsersServlet());
    }

    @Test
    void doGetReturnsAssignableUsers() throws Exception {
        Map<String, String> devUser = new HashMap<>();
        devUser.put("id", "dev-user");
        devUser.put("name", "Developer");
        Map<String, String> qaUser = new HashMap<>();
        qaUser.put("id", "qa-user");
        qaUser.put("name", "QA");
        List<Map<String, String>> users = Arrays.asList(devUser, qaUser);
        when(ticketUserService.getAssignableUsers(any())).thenReturn(users);

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        servlet.doGet(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        List<?> body = MAPPER.readValue(response.getOutputAsString(), List.class);
        assertEquals(2, body.size());
    }
}
