package org.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Testcontainers
public class CdrTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Inject
    CdrRepository cdrRepository;

    @Test
    public void testFaturaHesaplamaRealTime() {
        List<Cdr> cdrList = cdrRepository.listAll();
        for (Cdr cdr : cdrList) {
            double fatura = cdr.faturaHesapla(cdr.getAramaSuresi(), cdr.getAramaZamani());
            System.out.println("Arayan: " + cdr.getArayanNumara() + " | Fatura: " + fatura);
        }
    }
}
    

