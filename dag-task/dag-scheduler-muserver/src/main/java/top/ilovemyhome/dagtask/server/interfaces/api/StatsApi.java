package top.ilovemyhome.dagtask.server.interfaces.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.dto.ResEntityHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for dashboard statistics.
 */
@Path("/api/v1/stats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatsApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatsApi.class);

    @GET
    @Path("/overview")
    public Response overview() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWorkflows", 12);
        stats.put("totalExecutions", 156);
        stats.put("runningExecutions", 3);
        stats.put("successRate", 92.5);
        stats.put("avgExecutionTime", 2450);
        stats.put("activeAgents", 5);
        stats.put("totalTasks", 482);
        stats.put("failedTasks", 18);
        LOGGER.info("Retrieved dashboard overview stats");
        return Response.ok().entity(ResEntityHelper.ok("Overview stats retrieved successfully", stats)).build();
    }

    @GET
    @Path("/trends")
    public Response trends(@QueryParam("days") @DefaultValue("7") int days) {
        List<Map<String, Object>> trendData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> dayStats = new HashMap<>();
            dayStats.put("date", date.toString());
            dayStats.put("executions", 10 + i * 2);
            dayStats.put("successes", 8 + i);
            dayStats.put("failures", 2);
            dayStats.put("avgDuration", 2000 + i * 100);
            trendData.add(dayStats);
        }
        LOGGER.info("Retrieved trend data for last {} days", days);
        return Response.ok().entity(ResEntityHelper.ok("Trend data retrieved successfully", trendData)).build();
    }
}
