package com.banka1.user.model.helper;

import lombok.Getter;

@Getter
public enum Position {
    // primer pozicija koje zaposleni mogu da imaju u banci
    NONE,
    DIRECTOR,
    MANAGER,
    WORKER,
    HR
}
