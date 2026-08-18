package micronaut.tutorial.addressbook.resource;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import micronaut.tutorial.addressbook.entity.Contact;

import java.util.List;

@Controller("/contacts")
@Singleton
public class ContactResource {

    @Inject EntityManager em;

    @Get(produces = MediaType.APPLICATION_JSON)
    @Transactional
    public List<Contact> list() {
        return em.createQuery("SELECT c FROM Contact c ORDER BY c.id", Contact.class).getResultList();
    }

    @Get(uri = "/{id}", produces = MediaType.APPLICATION_JSON)
    @Transactional
    public Contact find(@PathVariable Long id) {
        return em.find(Contact.class, id);
    }

    @Post(consumes = MediaType.APPLICATION_JSON)
    @Transactional
    public HttpResponse<Contact> create(@Body Contact c) {
        em.persist(c);
        em.flush();
        return HttpResponse.created(c);
    }

    @Put(uri = "/{id}", consumes = MediaType.APPLICATION_JSON)
    @Transactional
    public HttpResponse<Contact> update(@PathVariable Long id, @Body Contact c) {
        Contact existing = em.find(Contact.class, id);
        if (existing == null) return HttpResponse.notFound();
        c.setId(id);
        Contact merged = em.merge(c);
        return HttpResponse.ok(merged);
    }

    @Delete("/{id}")
    @Transactional
    public HttpResponse<Void> delete(@PathVariable Long id) {
        Contact existing = em.find(Contact.class, id);
        if (existing == null) return HttpResponse.notFound();
        em.remove(existing);
        return HttpResponse.noContent();
    }

    @Get(uri = "/count", produces = MediaType.TEXT_PLAIN)
    @Transactional
    public String count() {
        Long c = em.createQuery("SELECT COUNT(c) FROM Contact c", Long.class).getSingleResult();
        return c.toString();
    }
}
