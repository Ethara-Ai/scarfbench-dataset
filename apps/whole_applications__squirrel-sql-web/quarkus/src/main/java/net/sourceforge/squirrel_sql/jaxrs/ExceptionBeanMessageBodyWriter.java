package net.sourceforge.squirrel_sql.jaxrs;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import net.sourceforge.squirrel_sql.dto.ExceptionBean;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ExceptionBeanMessageBodyWriter extends AbstractMessageBodyReaderWriter<ExceptionBean> {

}
