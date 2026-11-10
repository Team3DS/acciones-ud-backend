package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlpacaQuoteDto {
    @JsonProperty("symbol")
    public String symbol;
    @JsonProperty("bidprice")
    public String bidPrice;
    @JsonProperty("askprice")
    public String askPrice;
    @JsonProperty("bidsize")
    public String bidSize;
    @JsonProperty("asksize")
    public String askSize;
    @JsonProperty("timestamp")
    public String timestamp;
}
