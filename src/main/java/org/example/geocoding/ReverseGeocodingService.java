package org.example.geocoding;

import jakarta.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


@Path("/reverse")
public class ReverseGeocodingService {
    private static final String BASE_URL = "https://nominatim.md7.info/reverse";
    private static final Logger LOGGER = Logger.getLogger(ReverseGeocodingService.class.getName());

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response reverseGeocode(@QueryParam("lat") double lat, @QueryParam("lon") double lon) {
        if (lat == 0 || lon == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing lat or lon parameters").build();
        }

        Connection connection = null;
        try {
            // Load H2 driver
            Class.forName("org.h2.Driver");

            // Establish connection
            connection = DriverManager.getConnection("jdbc:h2:~/test;AUTO_SERVER=TRUE", "sa", "");

            // Disable auto-commit
            connection.setAutoCommit(false);

            // Log request
            String insertRequest = "INSERT INTO request_log (lat, lon) VALUES (?, ?)";
            PreparedStatement stmt = connection.prepareStatement(insertRequest, Statement.RETURN_GENERATED_KEYS);
            stmt.setDouble(1, lat);
            stmt.setDouble(2, lon);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            rs.next();
            long requestId = rs.getLong(1);

            // Fetch data from Nominatim
            String responseData = fetchDataFromNominatim(lat, lon);

            // Log response
            String insertResponse = "INSERT INTO response_log (request_id, response) VALUES (?, ?)";
            PreparedStatement stmtResponse = connection.prepareStatement(insertResponse);
            stmtResponse.setLong(1, requestId);
            stmtResponse.setString(2, responseData);
            stmtResponse.executeUpdate();

            // Commit transaction
            connection.commit();

            return Response.ok(responseData, MediaType.APPLICATION_JSON).build();
        } catch (SQLException | ClassNotFoundException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Database error", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Database error: " + e.getMessage()).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to close connection", e);
                }
            }
        }
    }

    private String fetchDataFromNominatim(double lat, double lon) throws Exception {
        URL url = new URL(BASE_URL + "?lat=" + lat + "&lon=" + lon + "&format=jsonv2");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        }

        Scanner scanner = new Scanner(url.openStream());
        StringBuilder inline = new StringBuilder();
        while (scanner.hasNext()) {
            inline.append(scanner.nextLine());
        }
        scanner.close();

        return inline.toString();
    }


}