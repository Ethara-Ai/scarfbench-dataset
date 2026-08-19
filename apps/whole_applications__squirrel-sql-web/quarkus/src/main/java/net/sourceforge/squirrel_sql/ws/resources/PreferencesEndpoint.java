package net.sourceforge.squirrel_sql.ws.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

import net.sourceforge.squirrel_sql.client.preferences.SquirrelPreferences;
import net.sourceforge.squirrel_sql.dto.ValueBean;
import net.sourceforge.squirrel_sql.fw.persist.ValidationException;
import net.sourceforge.squirrel_sql.ws.managers.PreferencesManager;

@Path("/")
@RequestScoped
public class PreferencesEndpoint {

    @Inject
    PreferencesManager manager;

    @GET
    @Path("/Preferences")
    public ValueBean<SquirrelPreferences> getItems() {
        SquirrelPreferences prefs = manager.get();
        return new ValueBean<>(prefs);
    }

    @PUT
    @Path("/Preferences")
    public ValueBean<SquirrelPreferences> update(SquirrelPreferences prefs) throws ValidationException {
        prefs = manager.update(prefs);
        return new ValueBean<>(prefs);
    }

}
