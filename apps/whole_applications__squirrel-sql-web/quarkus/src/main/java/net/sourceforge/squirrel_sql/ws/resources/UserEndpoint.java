package net.sourceforge.squirrel_sql.ws.resources;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import net.sourceforge.squirrel_sql.dto.ValueBean;
import net.sourceforge.squirrel_sql.ws.managers.UsersManager;
import net.sourceforge.squirrel_sql.ws.model.User;

/**
 * Users endpoints.
 * 
 * @author lv 2021
 */
@RequestScoped
@Path("/")
public class UserEndpoint {

    Logger logger = Logger.getLogger(SessionsEndpoint.class);

    @Inject
    UsersManager manager;

    @GET
    @Path("/Users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> findAll() {

        return manager.findAll();
    }

    @POST
    @Path("/Users")
    public ValueBean<User> createItem(User item) {
        item = manager.createNewUser(item);
        return new ValueBean<>(item);
    }

    @PUT
    @Path("/Users({identifier})")
    public ValueBean<User> updateItem(@PathParam("identifier") Integer identifier, User item) {
        item = manager.updateUser(item, identifier);
        return new ValueBean<>(item);
    }

    @DELETE
    @Path("/Users({identifier})")
    public void deleteItem(@PathParam("identifier") Integer identifier) {
        manager.removeUser(identifier);
    }
}
