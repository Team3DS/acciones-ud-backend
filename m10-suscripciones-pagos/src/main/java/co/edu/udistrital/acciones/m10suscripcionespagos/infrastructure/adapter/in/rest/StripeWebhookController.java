package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.RegistrarEventoStripeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pagos", description = "Webhooks de la pasarela de pagos")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/payments/webhook")
public class StripeWebhookController {

    private static final String PROBLEMA = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    private final RegistrarEventoStripeUseCase registrarEvento;

    public StripeWebhookController(RegistrarEventoStripeUseCase registrarEvento) {
        this.registrarEvento = registrarEvento;
    }

    @Operation(summary = "Recibe los eventos de Stripe",
            description = "Verifica la firma, guarda el evento en el inbox y responde 200. "
                    + "Un evento repetido tambien responde 200 sin duplicarlo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento registrado o ya existente"),
            @ApiResponse(responseCode = "400", description = "Firma, cuerpo o header invalido",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "El secreto del webhook no esta configurado",
                    content = @Content(mediaType = PROBLEMA,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/stripe", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> recibir(
            @RequestBody(required = false) byte[] cuerpo,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        registrarEvento.ejecutar(cuerpo, firma);
        return ResponseEntity.ok().build();
    }
}
