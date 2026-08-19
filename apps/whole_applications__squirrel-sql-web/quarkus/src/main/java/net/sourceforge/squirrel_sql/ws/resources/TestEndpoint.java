package net.sourceforge.squirrel_sql.ws.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class TestEndpoint {

    @GET
    @Path("/HelloWorld")
    public String helloWorld() {
        return "Hello World";
    }
}
