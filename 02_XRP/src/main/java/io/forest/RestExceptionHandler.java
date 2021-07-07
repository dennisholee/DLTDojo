package io.forest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;

@RestControllerAdvice
public class RestExceptionHandler /* extends ResponseEntityExceptionHandler */ {

	@ExceptionHandler(JsonRpcClientErrorException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Map<String, Object> handleXrp(Exception e) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("error", e.getMessage());
		return body;
	}

	@ExceptionHandler(UnknownActionException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Map<String, Object> handleUnknownAction(Exception e) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("error", e.getMessage());
		return body;
	}

}
