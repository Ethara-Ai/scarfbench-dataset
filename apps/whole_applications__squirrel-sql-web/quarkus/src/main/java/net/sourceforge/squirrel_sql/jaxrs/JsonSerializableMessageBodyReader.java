package net.sourceforge.squirrel_sql.jaxrs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JsonSerializableMessageBodyReader extends AbstractMessageBodyReaderWriter<JsonSerializable> {

}
