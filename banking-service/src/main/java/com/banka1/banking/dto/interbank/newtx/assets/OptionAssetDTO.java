package com.banka1.banking.dto.interbank.newtx.assets;

import com.banka1.banking.dto.interbank.newtx.AssetDTO;
import lombok.Data;

@Data
public class OptionAssetDTO extends AssetDTO {
    private OptionDescription asset;
}

