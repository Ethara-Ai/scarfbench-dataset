package net.sourceforge.squirrel_sql.ws.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreeNode;
import net.sourceforge.squirrel_sql.client.session.schemainfo.SchemaInfo;
import net.sourceforge.squirrel_sql.dto.ListBean;
import net.sourceforge.squirrel_sql.dto.ObjectTreeNodeDto;
import net.sourceforge.squirrel_sql.dto.SchemaInfoDto;
import net.sourceforge.squirrel_sql.dto.TableInfoDto;
import net.sourceforge.squirrel_sql.dto.ValueBean;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.ws.exceptions.AuthorizationException;
import net.sourceforge.squirrel_sql.ws.managers.ObjectsTabManager;
import net.sourceforge.squirrel_sql.ws.managers.SessionsManager;

@Path("/")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ObjectsTabEndpoint {

    @Inject
    ObjectsTabManager manager;
    @Inject
    SessionsManager sessionsManager;

    @GET
    @Path("/Session({sessionId})/SchemaInfo")
    public ValueBean<SchemaInfoDto> getSchemaInfo(@PathParam("sessionId") String sessionId)
            throws AuthorizationException {
        ISession session = sessionsManager.getSessionById(sessionId);
        sessionsManager.checkSession(session);
        SchemaInfo schemaInfo = session.getSchemaInfo();
        // If null, may raise HTTP 404
        return new ValueBean<>(new SchemaInfoDto(schemaInfo));
    }

    @GET
    @Path("/Session({sessionId})/SchemaInfo/TableInfo")
    public ListBean<TableInfoDto> getTableInfo(@PathParam("sessionId") String sessionId) throws AuthorizationException {
        ISession session = sessionsManager.getSessionById(sessionId);
        sessionsManager.checkSession(session);
        ITableInfo[] tableInfos = session.getSchemaInfo().getITableInfos();
        List<TableInfoDto> lst = new ArrayList<>();
        for (ITableInfo t : tableInfos) {
            lst.add(new TableInfoDto(t));
        }
        // If null, may raise HTTP 404
        return new ListBean<>(lst);
    }

    @GET
    @Path("/Session({sessionId})/RootNode")
    public ValueBean<ObjectTreeNodeDto> getRootNode(@PathParam("sessionId") String sessionId)
            throws SQLException, AuthorizationException {
        ISession session = sessionsManager.getSessionById(sessionId);
        sessionsManager.checkSession(session);
        ObjectTreeNode rootNode = manager.createAndExpandRootNode(session);
        ObjectTreeNodeDto rootNodeDto = manager.node2Dto(rootNode);
        return new ValueBean<>(rootNodeDto);
    }

    @POST
    @Path("/Session({sessionId})/ExpandNode")
    @Consumes(MediaType.APPLICATION_JSON)
    public ListBean<ObjectTreeNodeDto> expandNode(@PathParam("sessionId") String sessionId,
            ObjectTreeNodeDto parentNodeDto) throws SQLException, AuthorizationException {
        ISession session = sessionsManager.getSessionById(sessionId);
        sessionsManager.checkSession(session);
        ObjectTreeNode node = manager.dto2Node(parentNodeDto, session);
        List<ObjectTreeNode> list = manager.expandNode(node);
        List<ObjectTreeNodeDto> listDto = list.stream().map(x -> manager.node2Dto(x)).collect(Collectors.toList());
        return new ListBean<>(listDto);
    }

}
