package org.example;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
 public class Cdr extends PanacheEntity {
    @JsonProperty("arayan_numara")
    public String arayanNumara;
    @JsonProperty("aranan_numara")
    public String arananNumara;
    @JsonProperty("arama_suresi")
    public double aramaSuresi;
    @JsonProperty("arama_zamani")
    public LocalDateTime aramaZamani;
    @JsonProperty("fatura_tutari")
    public Double faturaTutari;

    // Default constructor - JSON deserialization için gerekli
    public Cdr() {
    }

    // Parametreli constructor - JSON serileştirme için kullanılabilir
    public Cdr(String arayanNumara, String arananNumara, double aramaSuresi, LocalDateTime aramaZamani) {
        this.arayanNumara = arayanNumara;
        this.arananNumara = arananNumara;
        this.aramaSuresi = aramaSuresi;
        this.aramaZamani = aramaZamani;
        this.faturaTutari = faturaHesapla(aramaSuresi, aramaZamani);
    }

    @JsonProperty("arayan_numara")
    public String getArayanNumara() {
        return arayanNumara;
    }

    public void setArayanNumara(String arayanNumara) {
        this.arayanNumara = arayanNumara;
    }

    @JsonProperty("aranan_numara")
    public String getArananNumara() {
        return arananNumara;
    }

    public void setArananNumara(String arananNumara) {
        this.arananNumara = arananNumara;
    }

    @JsonProperty("arama_suresi")
    public double getAramaSuresi() {
        return aramaSuresi;
    }

    public void setAramaSuresi(double aramaSuresi) {
        this.aramaSuresi = aramaSuresi;
        // Fatura tutarını otomatik olarak güncelle
        if (this.aramaZamani != null) {
            this.faturaTutari = faturaHesapla(aramaSuresi, aramaZamani);
        }
    }

    @JsonProperty("arama_zamani")
    public LocalDateTime getAramaZamani() {
        return aramaZamani;
    }

    public void setAramaZamani(LocalDateTime aramaZamani) {
        this.aramaZamani = aramaZamani;
        // Fatura tutarını otomatik olarak güncelle
        if (this.aramaSuresi > 0) {
            this.faturaTutari = faturaHesapla(aramaSuresi, aramaZamani);
        }
    }
    
    @JsonProperty("fatura_tutari")
    public Double getFaturaTutari() {
        return faturaTutari;
    }

    public void setFaturaTutari(Double faturaTutari) {
        this.faturaTutari = faturaTutari;
    }

    //1. Fatura hesaplama, örneğin Temmuz 2025'te XXX numaranın faturasını hesapla,
    //2.Daha da geliştirmek için akşam belirli saatler araasında yapılan aramalar %50 indirimli olsun. Diyelimki 19.30 da aranıp 1 saat konuşulduğunda 30 dakikası tam ücret geriye kalan 30 dakikası %50 indirimli olur
    //3. Uluslararası arama tarifesi: Yurt dışından yurt içine = 4 TL/dakika (saat farkı yok)

    public double faturaHesapla(double aramaSuresi, LocalDateTime aramaZamani) {
        // Uluslararası arama kontrolü
        // Yurt dışından yurt içine arama: 4 TL/dakika
        // Yurt içi arama: Normal tarife (saat bazlı)
        
        // Yurt dışı numara kontrolü (90 ile başlayan numaralar yurt içi)
        if ((!arayanNumara.startsWith("90") && !arayanNumara.startsWith("0") && !arayanNumara.startsWith("+90")) 
            && (arananNumara.startsWith("90") || arananNumara.startsWith("0") || arananNumara.startsWith("+90"))) {
            // ULUSLARARASI ARAMA: Yurt dışından yurt içine
            return faturaTutari * 4.0;
        } else {
            // YURT İÇİ ARAMA: Normal tarife (saat bazlı)
            int dakikaBasinaBirimFiyat = 2;
            double fatura = 0.0;

            //Başlangıç zamanı
            LocalDateTime currentTime = aramaZamani;
            for (int i = 0; i < (int) aramaSuresi; i++) {
                if (currentTime.getHour() >= 19 || currentTime.getHour() <= 7) {
                    // %50 indirimli dakikalar
                    fatura = fatura + (dakikaBasinaBirimFiyat / 2.0);
                } else {
                    // Normal ücretli dakikalar
                    fatura = fatura + dakikaBasinaBirimFiyat;
                }
                currentTime = currentTime.plusMinutes(1);
            }
            // Küsüratlı dakikaların (Örneğin 2 dakika 30 saniye olan aramaların kalan 30 saniyesi) ücretlendirilmesi

            double KusuratKontrolu = aramaSuresi - (int) aramaSuresi;

            if (KusuratKontrolu > 0) {
                if (currentTime.getHour() >= 19 || currentTime.getHour() <= 7) {
                    // %50 indirimli dakikalar
                    fatura = fatura + (dakikaBasinaBirimFiyat / 2.0) * KusuratKontrolu;
                } else {
                    // Normal ücretli dakikalar
                    fatura = fatura + dakikaBasinaBirimFiyat * KusuratKontrolu;
                }
            }
            return fatura;
        }
    }
    
    // Mevcut kayıtların faturalarını güncelle
    public void faturaGuncelle() {
        this.faturaTutari = faturaHesapla(this.aramaSuresi, this.aramaZamani);
    }
}







