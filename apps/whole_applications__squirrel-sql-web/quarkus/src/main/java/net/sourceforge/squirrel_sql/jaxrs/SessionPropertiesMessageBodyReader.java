package net.sourceforge.squirrel_sql.jaxrs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import net.sourceforge.squirrel_sql.client.session.properties.SessionProperties;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class SessionPropertiesMessageBodyReader extends AbstractMessageBodyReaderWriter<SessionProperties> {

}
