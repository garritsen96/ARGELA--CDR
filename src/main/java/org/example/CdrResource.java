package org.example;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import jakarta.transaction.Transactional;

@Path("/cdr")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CdrResource {

    @POST
    @Transactional
    public Response yeniCdrEkle(Cdr cdr) {
        if (cdr == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"CDR verisi null olamaz\"}")
                    .build();
        }
        // Zorunlu alanları kontrol et
        if (cdr.arayanNumara == null || cdr.arayanNumara.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Arayan numara boş olamaz\"}")
                    .build();
        }
        if (cdr.arananNumara == null || cdr.arananNumara.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Aranan numara boş olamaz\"}")
                    .build();
        }
        cdr.persist();
        return Response.status(Response.Status.CREATED).entity(cdr).build();
    }

    @GET
    public List<Cdr> tumunuGetir() {
        return Cdr.listAll();
    }
}
