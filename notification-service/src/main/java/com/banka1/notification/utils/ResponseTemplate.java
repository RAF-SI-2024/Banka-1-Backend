package com.banka1.notification.utils;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Utility klasa za izgradnju odgovora po specifičnom template-u.
 * <p></p>
 * Uspešan request:
 * <pre>
 * {@code
 *     {
 *         "success": true,
 *         "data": { ... }
 *     }
 * }
 * </pre>
 * Neuspešan request:
 * <pre>
 * {@code
 *     {
 *         "success": false,
 *         "error": "..."
 *     }
 * }
 * </pre>
 */
public final class ResponseTemplate {
    private static final String UNKNOWN_ERROR = "Nepoznata greška";
    private ResponseTemplate() { }

    /**
     * Pravi odgovor na osnovu neizgrađenog {@link ResponseEntity} objekta i završava njegovu izgradnju.
     *
     * @param fromBuilder {@link ResponseEntity.BodyBuilder BodyBuilder} koji u sebi već ima ostale potrebne informacije o odgovoru (npr. {@link org.springframework.http.HttpStatusCode statusni kod}).
     * @param success Da li je zahtev uspešno obrađen ili ne.
     * @param data Telo odgovora.
     * @param error Greška pri obrađivanju zahteva ili <code>null</code> ako nije bilo greške.
     * @return {@link ResponseEntity} napravljen po specificiranom template-u.
     */
    public static ResponseEntity<?> create(ResponseEntity.BodyBuilder fromBuilder, boolean success, Map<?, ?> data, String error) {
        if(success) {
            return fromBuilder.body(Map.of(
                    "success", true,
                    "data", data == null ? Map.of() : data
            ));
        } else {
            return fromBuilder.body(Map.of(
                    "success", false,
                    "error", error == null ? UNKNOWN_ERROR : error
            ));
        }
    }

    /**
     * Pravi odgovor na osnovu neizgrađenog {@link ResponseEntity} objekta i završava njegovu izgradnju.
     * <p></p>
     * Ova verzija metode automatski pravi neuspeli odgovor na osnovu prosleđenog izuzetka.
     *
     * @param fromBuilder {@link ResponseEntity.BodyBuilder BodyBuilder} koji u sebi već ima ostale potrebne informacije o odgovoru (npr. {@link org.springframework.http.HttpStatusCode statusni kod}).
     * @param e Izuzetak koji je izazvao vraćanje ovog odgovora.
     * @return {@link ResponseEntity} napravljen po specificiranom template-u.
     */
    public static ResponseEntity<?> create(ResponseEntity.BodyBuilder fromBuilder, Exception e) {
        return create(fromBuilder, false, null, e.getMessage());
    }
}
