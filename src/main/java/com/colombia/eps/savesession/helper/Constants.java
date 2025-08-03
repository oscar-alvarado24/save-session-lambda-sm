package com.colombia.eps.savesession.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final String MSG_PARSE_JSON = "Fallo al parsear el json";
    public static final String MESSAGE = "message: %s";
    public static final String MSG_ERROR_WITH_PARAM = "Parámetro %s es requerido";
    public static final String EMAIL = "email";
    public static final String IP = "ip";
    public static final String CITY = "city";
    public static final String CITY_NOT_AVAILABLE = "Ciudad no disponible";
    public static final String API_URL = "http://ip-api.com/json/%s?fields=city,status,message";
    public static final String MSG_ERROR = "Error: %s";
    public static final String SUCCESSFULLY = "Lambda ejecutada exitosamente";
    public static final String MSG_ERROR_PARAMS_NOT_VALID = "Parametros de email y/o ip invalidos";
    public static final String MSG_ERROR_PROCESSING = "Error al ejecutar la lambda";
}
