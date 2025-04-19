package com.banka1.banking.dto.interbank.newtx;

import com.banka1.banking.dto.interbank.newtx.assets.MonetaryAssetDTO;
import com.banka1.banking.dto.interbank.newtx.assets.OptionAssetDTO;
import com.banka1.banking.dto.interbank.newtx.assets.StockAssetDTO;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MonetaryAssetDTO.class, name = "MONAS"),
        @JsonSubTypes.Type(value = StockAssetDTO.class, name = "STOCK"),
        @JsonSubTypes.Type(value = OptionAssetDTO.class, name = "OPTION")
})
public abstract class AssetDTO {}

