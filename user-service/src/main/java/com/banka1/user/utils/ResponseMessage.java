package com.banka1.user.utils;

import lombok.Getter;

@Getter
public enum ResponseMessage {
    INVALID_LOGIN("Token nije prosleđen ili je neispravan/istekao."),
    FORBIDDEN("Nedovoljna autorizacija."),
    FAILED_LOGIN("Pogresna lozinka ili korisnik ne postoji."),
    PASSWORD_SET_SUCCESS("Lozinka uspešno postavljena."),
    PASSWORD_RESET_REQUEST_SUCCESS("Zahtev za resetovanje lozinke uspešno poslat."),
    PASSWORD_RESET_SUCCESS("Lozinka uspešno resetovana."),
    LOGOUT_SUCCESS("Korisnik odjavljen.");
    private final String message;

    ResponseMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
