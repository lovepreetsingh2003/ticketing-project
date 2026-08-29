package com.ai.ticketingApp.core.servlets;

import com.ai.ticketingApp.core.enums.TicketPriority;
import com.ai.ticketingApp.core.services.TicketService;
import com.ai.ticketingApp.core.services.TicketUserService;
import com.ai.ticketingApp.core.services.TicketValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(service = Servlet.class)
@SlingServletPaths("/bin/ticketing/ticket/update")
public class TicketUpdateServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private TicketService ticketService;

    @Reference
    private TicketValidationService validationService;

    @Reference
    private TicketUserService ticketUserService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String ticketPath  = request.getParameter("ticketPath");
        String title       = request.getParameter("title");
        String description = request.getParameter("description");
        String priority    = request.getParameter("priority");
        String assignee    = request.getParameter("assignee");

        if (StringUtils.isBlank(ticketPath)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap("ticketPath parameter is required")));
            return;
        }

        String validationError = validationService.validateUpdate(title, description, priority);
        if (validationError != null) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap(validationError)));
            return;
        }
        if (!ticketUserService.isAssignableUser(request.getResourceResolver(), assignee)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap("Assignee must be a QA or Developer user")));
            return;
        }

        try {
            TicketPriority ticketPriority = StringUtils.isNotBlank(priority)
                ? TicketPriority.fromString(priority) : null;
            ticketService.updateTicket(
                request.getResourceResolver(), ticketPath, title, description, ticketPriority, assignee);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            response.getWriter().write(MAPPER.writeValueAsString(result));
        } catch (IllegalArgumentException e) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap(e.getMessage())));
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap("Failed to update ticket: " + e.getMessage())));
        }
    }

    private Map<String, String> errorMap(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", message);
        return map;
    }
}
