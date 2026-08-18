package helidon.tutorial.addressbook.resource;

import helidon.tutorial.addressbook.entity.Contact;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/contacts")
@ApplicationScoped
public class ContactResource {

    @PersistenceContext(unitName = "addressbook") EntityManager em;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public List<Contact> list() {
        return em.createQuery("SELECT c FROM Contact c ORDER BY c.id", Contact.class).getResultList();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Contact find(@PathParam("id") Long id) { return em.find(Contact.class, id); }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(Contact c) {
        em.persist(c);
        em.flush();
        return Response.status(201).entity(c).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response update(@PathParam("id") Long id, Contact c) {
        Contact existing = em.find(Contact.class, id);
        if (existing == null) return Response.status(404).build();
        c.setId(id);
        Contact merged = em.merge(c);
        return Response.ok(merged).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Contact existing = em.find(Contact.class, id);
        if (existing == null) return Response.status(404).build();
        em.remove(existing);
        return Response.noContent().build();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String count() {
        Long c = em.createQuery("SELECT COUNT(c) FROM Contact c", Long.class).getSingleResult();
        return c.toString();
    }
}
