package com.colombia.eps.savesession.service.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ip-api")
@Path("/json")
@ApplicationScoped
public interface GeolocationRestClient {

    @GET
    @Path("/{ip}")
    @Produces(MediaType.APPLICATION_JSON)
    String getLocationData(
            @PathParam("ip") String ip,
            @QueryParam("fields") String fields
    );
}
