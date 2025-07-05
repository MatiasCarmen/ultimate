package com.mycompany.vcsystems.modelo.service;

import com.mycompany.vcsystems.modelo.entidades.Incidencia;

/**
 * Clase que encapsula el resultado de operaciones sobre incidencias
 * Proporciona información detallada sobre el éxito o fallo de la operación
 * Reemplaza el uso simple de Optional por un patrón más expresivo
 */
public class IncidenciaOperationResult {

    public enum ResultType {
        SUCCESS,           // Operación exitosa
        NOT_FOUND,        // Recurso no encontrado
        BUSINESS_ERROR,   // Error de validación de negocio
        TECHNICAL_ERROR   // Error técnico/sistema
    }

    private final ResultType type;
    private final String message;
    private final Incidencia incidencia;
    private final String errorCode;

    private IncidenciaOperationResult(ResultType type, String message, Incidencia incidencia, String errorCode) {
        this.type = type;
        this.message = message;
        this.incidencia = incidencia;
        this.errorCode = errorCode;
    }

    // Factory methods para crear diferentes tipos de resultados

    public static IncidenciaOperationResult success(Incidencia incidencia) {
        return new IncidenciaOperationResult(ResultType.SUCCESS, "Operación exitosa", incidencia, null);
    }

    public static IncidenciaOperationResult notFound(String message) {
        return new IncidenciaOperationResult(ResultType.NOT_FOUND, message, null, "RESOURCE_NOT_FOUND");
    }

    public static IncidenciaOperationResult businessError(String message) {
        return new IncidenciaOperationResult(ResultType.BUSINESS_ERROR, message, null, "BUSINESS_VALIDATION_ERROR");
    }

    public static IncidenciaOperationResult businessError(String message, String errorCode) {
        return new IncidenciaOperationResult(ResultType.BUSINESS_ERROR, message, null, errorCode);
    }

    public static IncidenciaOperationResult technicalError(String message) {
        return new IncidenciaOperationResult(ResultType.TECHNICAL_ERROR, message, null, "TECHNICAL_ERROR");
    }

    public static IncidenciaOperationResult technicalError(String message, String errorCode) {
        return new IncidenciaOperationResult(ResultType.TECHNICAL_ERROR, message, null, errorCode);
    }

    // Métodos de consulta

    public boolean isSuccess() {
        return type == ResultType.SUCCESS;
    }

    public boolean isNotFound() {
        return type == ResultType.NOT_FOUND;
    }

    public boolean isBusinessError() {
        return type == ResultType.BUSINESS_ERROR;
    }

    public boolean isTechnicalError() {
        return type == ResultType.TECHNICAL_ERROR;
    }

    public boolean hasError() {
        return !isSuccess();
    }

    // Getters

    public ResultType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Incidencia getIncidencia() {
        return incidencia;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Métodos de conveniencia para el controlador

    /**
     * Obtiene el código HTTP apropiado según el tipo de resultado
     */
    public int getHttpStatusCode() {
        return switch (type) {
            case SUCCESS -> 200;
            case NOT_FOUND -> 404;
            case BUSINESS_ERROR -> 400;
            case TECHNICAL_ERROR -> 500;
        };
    }

    /**
     * Convierte el resultado a un Optional para compatibilidad con código legacy
     */
    public java.util.Optional<Incidencia> toOptional() {
        return isSuccess() ? java.util.Optional.of(incidencia) : java.util.Optional.empty();
    }

    @Override
    public String toString() {
        return String.format("IncidenciaOperationResult{type=%s, message='%s', errorCode='%s', hasIncidencia=%s}",
            type, message, errorCode, incidencia != null);
    }
}
