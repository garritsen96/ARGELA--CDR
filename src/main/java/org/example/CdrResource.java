package org.example;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import jakarta.persistence.EntityManager;

@Path("/cdr")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CdrResource {

    @Inject
    CdrRepository cdrRepository;

    @Inject
    EntityManager entityManager;

    @POST
    @Transactional
    public Response yeniCdrEkle(Cdr cdr) {
        if (cdr == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"CDR verisi null olamaz\"}")
                    .build();
        }
        //Constraintsler
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
        
        // Fatura tutarını otomatik olarak hesapla
        if (cdr.aramaZamani != null && cdr.aramaSuresi > 0) {
            cdr.faturaTutari = cdr.faturaHesapla(cdr.aramaSuresi, cdr.aramaZamani);
        }
        
        cdrRepository.persist(cdr);
        return Response.status(Response.Status.CREATED).entity(cdr).build();
    }

    @GET
    public List<Cdr> tumunuGetir() {
        return cdrRepository.listAll();
    }

    @GET
    @Path("/{id}/fatura")
    public Response faturaHesapla(@PathParam("id") Long id) {
        Cdr cdr = cdrRepository.findByIdOptional(id).orElse(null);

        if (cdr == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Bu ID ile kayıt bulunamadı\"}")
                    .build();
        }

        double sure = cdr.getAramaSuresi();
        LocalDateTime zaman = cdr.getAramaZamani();
        double fatura = cdr.faturaHesapla(sure, zaman);


        return Response.ok("{\"fatura\":" + fatura + "}").build();
    }

    // Telefon numarasına göre aylık fatura hesaplama
    @GET
    @Path("/fatura/{telefonNumarasi}/{yil}/{ay}")
    public Response aylikFaturaHesapla(@PathParam("telefonNumarasi") String telefonNumarasi,
                                       @PathParam("yil") int yil,
                                       @PathParam("ay") int ay) {
        
        // Belirtilen ay için tarihleri belirle
        LocalDateTime ayBaslangic = LocalDateTime.of(yil, ay, 1, 0, 0, 0);
        LocalDateTime aySonu = ayBaslangic.plusMonths(1).minusSeconds(1);
        
        // O telefon numarasının o aydaki tüm aramalarını bul
        List<Cdr> aylikCdrler = cdrRepository.find(
            "arayanNumara = ?1 and aramaZamani >= ?2 and aramaZamani <= ?3",
            telefonNumarasi, ayBaslangic, aySonu
        ).list();
        
        if (aylikCdrler.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Bu telefon numarası için " + yil + "/" + ay + " ayında CDR kaydı bulunamadı\"}")
                    .build();
        }

        // Aylık toplam fatura hesapla
        double toplamFatura = 0.0;
        double toplamDakika = 0.0;
        int normalSaatAramaSayisi = 0;
        int aksamSaatAramaSayisi = 0;

        StringBuilder aramaDetaylari = new StringBuilder();
        aramaDetaylari.append("[");

        for (int i = 0; i < aylikCdrler.size(); i++) {
            Cdr cdr = aylikCdrler.get(i);
            double fatura = cdr.faturaHesapla(cdr.getAramaSuresi(), cdr.getAramaZamani());
            toplamFatura += fatura;
            toplamDakika += cdr.getAramaSuresi();
            
            // Arama saati kontrolü
            int saat = cdr.getAramaZamani().getHour();
            if (saat >= 19 || saat <= 7) {
                aksamSaatAramaSayisi++;
            } else {
                normalSaatAramaSayisi++;
            }
            
            // Arama detayını JSON'a ekle
            aramaDetaylari.append("{")
                    .append("\"id\":").append(cdr.id).append(",")
                    .append("\"aranan_numara\":\"").append(cdr.getArananNumara()).append("\",")
                    .append("\"arama_suresi\":").append(cdr.getAramaSuresi()).append(",")
                    .append("\"arama_zamani\":\"").append(cdr.getAramaZamani()).append("\",")
                    .append("\"fatura\":").append(fatura)
                    .append("}");
                    
            if (i < aylikCdrler.size() - 1) {
                aramaDetaylari.append(",");
            }
        }
        aramaDetaylari.append("]");

        // JSON response oluştur
        String jsonResponse = "{"
                + "\"telefon_numarasi\":\"" + telefonNumarasi + "\","
                + "\"yil\":" + yil + ","
                + "\"ay\":" + ay + ","
                + "\"toplam_arama_sayisi\":" + aylikCdrler.size() + ","
                + "\"normal_saat_aramalar\":" + normalSaatAramaSayisi + ","
                + "\"aksam_saat_aramalar\":" + aksamSaatAramaSayisi + ","
                + "\"toplam_dakika\":" + toplamDakika + ","
                + "\"toplam_fatura\":" + toplamFatura + ","
                + "\"arama_detaylari\":" + aramaDetaylari.toString()
                + "}";

        return Response.ok(jsonResponse).build();
    }

    // Telefon numarasının tüm zamanlar toplamı (aylık filtre olmadan)
    @GET
    @Path("/fatura/{telefonNumarasi}")
    public Response telefonFaturasiHesapla(@PathParam("telefonNumarasi") String telefonNumarasi) {
        List<Cdr> cdrListesi = cdrRepository.find("arayanNumara", telefonNumarasi).list();
        
        if (cdrListesi.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Bu telefon numarası için CDR kaydı bulunamadı\"}")
                    .build();
        }

        double toplamFatura = 0.0;
        for (Cdr cdr : cdrListesi) {
            toplamFatura += cdr.faturaHesapla(cdr.getAramaSuresi(), cdr.getAramaZamani());
        }

        return Response.ok()
                .entity("{\"telefon_numarasi\":\"" + telefonNumarasi + "\"," +
                       "\"toplam_arama_sayisi\":" + cdrListesi.size() + "," +
                       "\"toplam_fatura\":" + toplamFatura + "}")
                .build();


    }
    @DELETE
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response deleteAllCdr() {
        try {
            long silinen = Cdr.delete("1=1"); // Tüm kayıtları sil
            return Response.ok("Tüm CDR kayıtları silindi: " + silinen).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Kayıtlar silinirken hata oluştu: " + e.getMessage())
                    .build();
        }
    }
    
    @POST
    @Path("/fatura-guncelle")
    @Transactional
    public Response faturaGuncelle() {
        try {
            List<Cdr> cdrList = Cdr.listAll();
            for (Cdr cdr : cdrList) {
                cdr.faturaGuncelle(); // faturaHesapla() metodunu kullanır
                cdr.persist();
            }
            return Response.ok("Faturalar güncellendi!").build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
    
    @GET
    @Path("/test")
    public Response test() {
        return Response.ok("{\"message\":\"Test endpoint çalışıyor!\"}").build();
    }
    
    // Stored procedure ile fatura hesaplama
    @GET
    @Path("/fatura-guncelle-stored-proc/{id}")
    @Transactional
    public Response faturaGuncelleStoredProc(@PathParam("id") Long id) {
        try {
            // Stored procedure'ü çağır
            entityManager.createNativeQuery("CALL HesaplaFaturaTutari(?)")
                       .setParameter(1, id)
                       .executeUpdate();
            
            // Güncellenmiş kaydı getir
            Cdr cdr = cdrRepository.findByIdOptional(id).orElse(null);
            if (cdr == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Kayıt bulunamadı\"}")
                        .build();
            }
            
            return Response.ok("{\"id\":" + id + ",\"faturaTutari\":" + cdr.faturaTutari + "}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    
    // Java methodu ile otomatik fatura hesaplama
    @GET
    @Path("/fatura-hesapla-java/{id}")
    @Transactional
    public Response faturaHesaplaJava(@PathParam("id") Long id) {
        try {
            // Kaydı bul
            Cdr cdr = cdrRepository.findByIdOptional(id).orElse(null);
            if (cdr == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Kayıt bulunamadı\"}")
                        .build();
            }
            
            // Java methodu ile fatura hesapla
            double faturaTutari = cdr.faturaHesapla(cdr.aramaSuresi, cdr.aramaZamani);
            
            // Fatura tutarını güncelle
            cdr.faturaTutari = faturaTutari;
            cdrRepository.persist(cdr);
            
            return Response.ok("{\"id\":" + id + ",\"faturaTutari\":" + faturaTutari + "}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
/*INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (4, '5554578811', '4298453503', 50, '2025-01-15 14:30:00');


INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (5, '5054101867', '5066457097', 50, '2025-01-15 14:30:00');

INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (6, '1234567890', '5437689502', 50, '2025-01-15 14:30:00');


INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (7, '0987654321', '9804376581', 50, '2025-01-15 14:30:00');

INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (11, '5551111111', '5559999999', 60, NOW());

INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (104, '1987654321', '9804376581', 20, '2025-01-15 18:50:00');

ALTER TABLE Cdr ADD COLUMN faturaTutari DECIMAL(10,2);
DESCRIBE Cdr;

-- Tüm kayıtların faturalarını güncelle
INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (12, '5551234567', '5559876543', 90, '2025-01-15 20:00:00');

-- 1. Kayıt ekle
INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (107, '5553333333', '5554444444', 30, '2025-01-15 16:30:00');

-- 2. Fatura hesapla
CALL HesaplaFaturaTutari(107);

-- 1. Kayıt ekle
INSERT INTO Cdr (id, arayanNumara, arananNumara, aramaSuresi, aramaZamani)
VALUES (108, '5553333333', '5554444444', 30, '2025-01-15 16:30:00');

-- 2. Fatura hesapla
CALL HesaplaFaturaTutari(10);

UPDATE Cdr SET faturaTutari = NULL WHERE id > 0;

DELIMITER //

CREATE PROCEDURE HesaplaFaturaTutari(IN kayit_id INT)
BEGIN
    DECLARE arama_suresi DECIMAL(10,2);
    DECLARE arama_zamani DATETIME;
    DECLARE fatura_tutari DECIMAL(10,2);

    -- Kayıt bilgilerini al
    SELECT aramaSuresi, aramaZamani INTO arama_suresi, arama_zamani
    FROM Cdr WHERE id = kayit_id;

    -- Fatura hesapla - DOĞRU FORMÜL
    IF HOUR(arama_zamani) >= 19 OR HOUR(arama_zamani) <= 7 THEN
        SET fatura_tutari = arama_suresi * 1;
    ELSE
        SET fatura_tutari = arama_suresi * 2;
    END IF;

    -- Fatura tutarını güncelle
    UPDATE Cdr SET faturaTutari = fatura_tutari WHERE id = kayit_id;

    SELECT CONCAT('ID ', kayit_id, ' için fatura tutarı: ', fatura_tutari, ' TL') as sonuc;
END //

DELIMITER ;

-- 107 ve 108 için doğru fatura hesapla
CALL HesaplaFaturaTutari(107);
CALL HesaplaFaturaTutari(108);*/