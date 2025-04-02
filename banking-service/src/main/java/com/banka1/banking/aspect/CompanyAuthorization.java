package com.banka1.banking.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotacija koja se vezuje za kontroler metode za koje je potrebna autorizacija operacije vezane za kompanije.
 * Ako korisnik koji šalje zahtev i nije zaposlen, neće biti autorizovan.
 * <p></p>
 * Ako je <code>employeeOnlyOperation</code> postavljeno kao <code>true</code>, mušterije neće imati pristup ovoj metodi.
 * <p></p>
 * Primer korišćenja:
 * <p></p>
 * <pre>
 * {@code
 * @AccountAuthorization
 * @GetMapping("/{account_id}/...")
 * public ResponseEntity<?> requireAccountAccess(@RequestHeader("Authorization") String authorization, @PathVariable("account_id") Long accountId ...) {
 *     ...
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompanyAuthorization {
    boolean employeeOnlyOperation() default false;
}
