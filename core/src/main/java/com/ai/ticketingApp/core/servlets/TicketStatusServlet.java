package com.ai.ticketingApp.core.servlets;

import com.ai.ticketingApp.core.enums.TicketStatus;
import com.ai.ticketingApp.core.services.TicketService;
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
@SlingServletPaths("/bin/ticketing/ticket/status")
public class TicketStatusServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private TicketService ticketService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String ticketPath   = request.getParameter("ticketPath");
        String newStatusStr = request.getParameter("status");

        if (StringUtils.isBlank(ticketPath)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap("ticketPath parameter is required")));
            return;
        }
        if (StringUtils.isBlank(newStatusStr)) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap("status parameter is required")));
            return;
        }

        TicketStatus newStatus = TicketStatus.fromString(newStatusStr);
        if (newStatus == null) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap("Invalid status value: " + newStatusStr)));
            return;
        }

        try {
            ticketService.changeStatus(request.getResourceResolver(), ticketPath, newStatus);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("status", newStatus.name());
            response.getWriter().write(MAPPER.writeValueAsString(result));
        } catch (IllegalStateException e) {
            response.setStatus(409);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap(e.getMessage())));
        } catch (IllegalArgumentException e) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap(e.getMessage())));
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(MAPPER.writeValueAsString(errorMap("Failed to change status: " + e.getMessage())));
        }
    }

    private Map<String, String> errorMap(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", message);
        return map;
    }
}
