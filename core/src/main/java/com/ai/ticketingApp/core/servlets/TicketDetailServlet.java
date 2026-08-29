package com.ai.ticketingApp.core.servlets;

import com.ai.ticketingApp.core.services.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/ticketing/ticket/detail")
public class TicketDetailServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private TicketService ticketService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String ticketPath = request.getParameter("ticketPath");

        if (StringUtils.isBlank(ticketPath)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            Map<String, String> error = new HashMap<>();
            error.put("error", "ticketPath parameter is required");
            response.getWriter().write(MAPPER.writeValueAsString(error));
            return;
        }

        try {
            Map<String, Object> ticket = ticketService.getTicket(request.getResourceResolver(), ticketPath);
            if (ticket == null) {
                response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Ticket not found: " + ticketPath);
                response.getWriter().write(MAPPER.writeValueAsString(error));
                return;
            }
            response.getWriter().write(MAPPER.writeValueAsString(ticket));
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to load ticket: " + e.getMessage());
            response.getWriter().write(MAPPER.writeValueAsString(error));
        }
    }
}
