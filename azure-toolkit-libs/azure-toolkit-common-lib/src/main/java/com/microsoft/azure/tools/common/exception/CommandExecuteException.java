package com.microsoft.azure.tools.common.exception;

public class CommandExecuteException extends RuntimeException {
    private static final long serialVersionUID = 4582230448665092548L;

    public CommandExecuteException(String message) {
        super(message);
    }
}
