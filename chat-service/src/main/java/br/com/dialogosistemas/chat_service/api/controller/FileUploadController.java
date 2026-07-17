package br.com.dialogosistemas.chat_service.api.controller;

import br.com.dialogosistemas.chat_service.application.DTO.ConfirmUploadRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.RequestUploadRequestDTO;
import br.com.dialogosistemas.chat_service.application.DTO.RequestUploadResponseDTO;
import br.com.dialogosistemas.chat_service.application.usecase.ConfirmUploadUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.GetAttachmentDownloadUrlUseCase;
import br.com.dialogosistemas.chat_service.application.usecase.RequestUploadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "File Upload", description = "Upload e download de arquivos via GCS signed URLs")
public class FileUploadController {

    private final RequestUploadUseCase requestUploadUseCase;
    private final ConfirmUploadUseCase confirmUploadUseCase;
    private final GetAttachmentDownloadUrlUseCase getAttachmentDownloadUrlUseCase;

    public FileUploadController(RequestUploadUseCase requestUploadUseCase,
                                ConfirmUploadUseCase confirmUploadUseCase,
                                GetAttachmentDownloadUrlUseCase getAttachmentDownloadUrlUseCase) {
        this.requestUploadUseCase = requestUploadUseCase;
        this.confirmUploadUseCase = confirmUploadUseCase;
        this.getAttachmentDownloadUrlUseCase = getAttachmentDownloadUrlUseCase;
    }

    @PostMapping("/conversations/{conversationId}/upload/request")
    @Operation(summary = "Solicitar upload", description = "Gera uma signed URL para o cliente fazer upload direto no GCS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed URL gerada"),
            @ApiResponse(responseCode = "400", description = "Content-type ou tamanho invalido"),
            @ApiResponse(responseCode = "403", description = "Usuario nao e participante")
    })
    public ResponseEntity<RequestUploadResponseDTO> requestUpload(
            @PathVariable UUID conversationId,
            @RequestBody RequestUploadRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        var dto = new RequestUploadRequestDTO(conversationId, request.fileName(), request.contentType(), request.sizeInBytes());
        RequestUploadResponseDTO response = requestUploadUseCase.execute(dto, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations/{conversationId}/upload/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Confirmar upload", description = "Confirma que os arquivos foram uploaded e cria a mensagem com attachments.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mensagem criada com attachments"),
            @ApiResponse(responseCode = "400", description = "Upload ID invalido ou expirado"),
            @ApiResponse(responseCode = "403", description = "Upload nao pertence ao usuario")
    })
    public ResponseEntity<Void> confirmUpload(
            @PathVariable UUID conversationId,
            @RequestBody ConfirmUploadRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        var dto = new ConfirmUploadRequestDTO(conversationId, request.uploadIds(), request.caption());
        confirmUploadUseCase.execute(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/messages/{messageId}/attachment/{fileId}")
    @Operation(summary = "Obter URL de download", description = "Gera uma signed URL temporaria para download do attachment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Download URL gerada"),
            @ApiResponse(responseCode = "403", description = "Usuario nao e participante"),
            @ApiResponse(responseCode = "404", description = "Mensagem ou attachment nao encontrado")
    })
    public ResponseEntity<GetAttachmentDownloadUrlUseCase.DownloadUrlResponse> getDownloadUrl(
            @PathVariable UUID messageId,
            @PathVariable UUID fileId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        var response = getAttachmentDownloadUrlUseCase.execute(messageId, fileId, userId);
        return ResponseEntity.ok(response);
    }
}
