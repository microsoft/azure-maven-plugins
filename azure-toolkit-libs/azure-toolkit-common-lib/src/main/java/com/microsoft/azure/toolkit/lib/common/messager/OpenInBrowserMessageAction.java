package com.microsoft.azure.toolkit.lib.common.messager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.awt.*;
import java.net.URI;

@RequiredArgsConstructor
@Getter
public class OpenInBrowserMessageAction implements IAzureMessage.Action {
    private final String name;
    private final String url;

    @Override
    public String name() {
        return this.name;
    }

    @SneakyThrows
    @Override
    public void actionPerformed(IAzureMessage message) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(this.url));
        }
    }

    @Override
    public String toString() {
        return String.format("[%s](%s)", this.name, this.url);
    }
}