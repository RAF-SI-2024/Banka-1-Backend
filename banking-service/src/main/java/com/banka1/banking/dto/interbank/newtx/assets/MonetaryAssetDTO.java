package com.banka1.banking.dto.interbank.newtx.assets;

import com.banka1.banking.dto.interbank.newtx.AssetDTO;
import lombok.Data;

@Data
public class MonetaryAssetDTO extends AssetDTO {
    private String type; // uvek "MONAS"
    private CurrencyAsset asset;
}
