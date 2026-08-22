package com.ai.ticketingApp.core.servlets;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.enums.TicketStatus;
import com.ai.ticketingApp.core.services.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/ticketing/tickets/search")
public class TicketSearchServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private TicketService ticketService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String keyword     = request.getParameter("keyword");
        String statusStr   = request.getParameter("status");
        String priorityStr = request.getParameter("priority");

        TicketStatus   statusFilter   = TicketStatus.fromString(statusStr);
        TicketPriority priorityFilter = TicketPriority.fromString(priorityStr);

        try {
            List<Map<String, Object>> tickets = ticketService.searchTickets(
                request.getResourceResolver(), keyword, statusFilter, priorityFilter);
            Map<String, Object> result = new HashMap<>();
            result.put("tickets", tickets);
            result.put("total", tickets.size());
            response.getWriter().write(MAPPER.writeValueAsString(result));
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Search failed: " + e.getMessage());
            response.getWriter().write(MAPPER.writeValueAsString(error));
        }
    }
}
