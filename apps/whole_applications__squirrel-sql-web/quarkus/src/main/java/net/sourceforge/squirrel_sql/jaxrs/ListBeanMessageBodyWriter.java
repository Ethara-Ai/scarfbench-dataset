package net.sourceforge.squirrel_sql.jaxrs;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import net.sourceforge.squirrel_sql.dto.ListBean;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ListBeanMessageBodyWriter extends AbstractMessageBodyReaderWriter<ListBean<?>> {

}
