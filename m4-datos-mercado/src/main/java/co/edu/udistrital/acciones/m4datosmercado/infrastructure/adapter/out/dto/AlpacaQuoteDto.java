package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlpacaQuoteDto {
    @JsonProperty("symbol")
    public String symbol;

    @JsonProperty("bid_price")
    public String bidPrice;

    @JsonProperty("ask_price")
    public String askPrice;

    @JsonProperty("bid_size")
    public String bidSize;

    @JsonProperty("ask_size")
    public String askSize;

    @JsonProperty("timestamp")
    public String timestamp;
}
