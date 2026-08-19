package net.sourceforge.squirrel_sql.jaxrs;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonSerializableMessageBodyWriter extends AbstractMessageBodyReaderWriter<JsonSerializable> {

}
