package org.example;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
class Cdr extends PanacheEntity {
    public String arayanNumara;
    public String arananNumara;
    public int aramaSuresi;
    public LocalDateTime aramaZamani;

    // Default constructor - JSON deserialization için gerekli
    public Cdr() {
    }

    public Cdr(String arayanNumara, String arananNumara, int aramaSuresi, LocalDateTime aramaZamani) {
        this.arayanNumara = arayanNumara;
        this.arananNumara = arananNumara;
        this.aramaSuresi = aramaSuresi;
        this.aramaZamani = aramaZamani;
    }

    public String getArayanNumara() {
        return arayanNumara;
    }

    public void setArayanNumara(String arayanNumara) {
        this.arayanNumara = arayanNumara;
    }

    public String getArananNumara() {
        return arananNumara;
    }

    public void setArananNumara(String arananNumara) {
        this.arananNumara = arananNumara;
    }

    public int getAramaSuresi() {
        return aramaSuresi;
    }

    public void setAramaSuresi(int aramaSuresi) {
        this.aramaSuresi = aramaSuresi;
    }

    public LocalDateTime getAramaZamani() {
        return aramaZamani;
    }

    public void setAramaZamani(LocalDateTime aramaZamani) {
        this.aramaZamani = aramaZamani;
    }
}




