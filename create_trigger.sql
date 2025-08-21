-- CDR tablosuna yeni kayıt eklendiğinde otomatik fatura hesaplama trigger'ı
-- Bu trigger, INSERT sonrası otomatik olarak fatura hesaplar ve aylık toplam faturayı günceller
-- Uluslararası arama tarifesi: Yurt dışından yurt içine = 4 TL/dakika (saat farkı yok)

DELIMITER //

CREATE TRIGGER fatura_hesapla_after_insert
BEFORE INSERT ON Cdr
FOR EACH ROW
BEGIN
    -- Yeni eklenen kayıt için fatura hesapla
    DECLARE fatura_tutari DECIMAL(10,2);
    DECLARE dakika_basina_fiyat INT DEFAULT 2;
    DECLARE current_datetime DATETIME;
    DECLARE i INT DEFAULT 0;
    DECLARE kusurat DECIMAL(10,2);
    DECLARE aylik_toplam DECIMAL(10,2);
    
    -- Uluslararası arama kontrolü
    -- Yurt dışından yurt içine arama: 4 TL/dakika
    -- Yurt içi arama: Normal tarife (saat bazlı)
    
    -- Yurt dışı numara kontrolü (90 ile başlayan numaralar yurt içi)
    -- DOĞRU MANTIK: arayanNumara yurt dışı VE arananNumara yurt içi
    IF (NEW.arayanNumara NOT LIKE '90%' AND NEW.arayanNumara NOT LIKE '0%' AND NEW.arayanNumara NOT LIKE '+90%') 
       AND (NEW.arananNumara LIKE '90%' OR NEW.arananNumara LIKE '0%' OR NEW.arananNumara LIKE '+90%') THEN
        -- ULUSLARARASI ARAMA: Yurt dışından yurt içine
        -- Arama süresi değişmez, sadece fatura tutarı 4 TL/dakika olarak hesaplanır
        SET fatura_tutari = NEW.aramaSuresi * 4.0;
    ELSE
        -- YURT İÇİ ARAMA: Normal tarife (saat bazlı)
        SET fatura_tutari = 0.0;
        SET current_datetime = NEW.aramaZamani;
        
        -- Tam dakikalar için hesaplama
        WHILE i < FLOOR(NEW.aramaSuresi) DO
            IF HOUR(current_datetime) >= 19 OR HOUR(current_datetime) <= 7 THEN
                -- %50 indirimli dakikalar (19:00-07:00 arası)
                SET fatura_tutari = fatura_tutari + (dakika_basina_fiyat / 2.0);
            ELSE
                -- Normal ücretli dakikalar (07:00-19:00 arası)
                SET fatura_tutari = fatura_tutari + dakika_basina_fiyat;
            END IF;
            
            SET current_datetime = DATE_ADD(current_datetime, INTERVAL 1 MINUTE);
            SET i = i + 1;
        END WHILE;
        
        -- Küsüratlı dakikalar için hesaplama
        SET kusurat = NEW.aramaSuresi - FLOOR(NEW.aramaSuresi);
        IF kusurat > 0 THEN
            IF HOUR(current_datetime) >= 19 OR HOUR(current_datetime) <= 7 THEN
                -- %50 indirimli küsürat
                SET fatura_tutari = fatura_tutari + (dakika_basina_fiyat / 2.0) * kusurat;
            ELSE
                -- Normal ücretli küsürat
                SET fatura_tutari = fatura_tutari + dakika_basina_fiyat * kusurat;
            END IF;
        END IF;
    END IF;
    
    -- Aynı numaranın 1 ay içindeki toplam faturasını hesapla
    SELECT COALESCE(SUM(faturaTutari), 0) + fatura_tutari INTO aylik_toplam
    FROM Cdr 
    WHERE arayanNumara = NEW.arayanNumara 
    AND YEAR(aramaZamani) = YEAR(NEW.aramaZamani) 
    AND MONTH(aramaZamani) = MONTH(NEW.aramaZamani);
    
    -- Fatura tutarını NEW değerine ata
    SET NEW.faturaTutari = fatura_tutari;
    SET NEW.aylikToplamFatura = aylik_toplam;
END //

DELIMITER ;
