/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class AzureHtmlMessage extends AzureMessage {
    static final Pattern URL_PATTERN = compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");

    static final Pattern HREF_PATTERN = compile("<a href=(.*)>(.*)</a>");
    private IAzureMessage rawMessage;

    public AzureHtmlMessage(@Nonnull Type type, @Nonnull AzureString message) {
        super(type, message);
    }

    public AzureHtmlMessage(IAzureMessage origin) {
        super(origin.getType(), origin.getMessage());
        this.rawMessage = origin.getRawMessage();
        if (origin instanceof AzureMessage) {
            this.setValueDecorator(((AzureMessage) origin).getValueDecorator());
        }
        this.setTitle(origin.getTitle());
        this.setPayload(origin.getPayload());
        this.setActions(origin.getActions());
    }

    @Nonnull
    @Override
    public String getContent() {
        return transformURLIntoLinks(super.getContent());
    }

    @Nullable
    @Override
    protected String getCause(@Nonnull Throwable throwable) {
        final String color = getErrorColor();
        return Optional.ofNullable(super.getCause(throwable))
                .map(cause -> String.format("<span style=\"color: %s;\">%s</span>", color, cause))
                .orElse(null);
    }

    @Override
    protected String getDetailItem(Operation o) {
        return String.format("<li>%s</li>", super.getDetailItem(o));
    }

    @Override
    public String decorateValue(@Nonnull Object p, @Nullable Supplier<String> dft) {
        String result = super.decorateValue(p, null);
        if (Objects.isNull(result)) {
            final String color = getValueColor();
            final String font = "'JetBrains Mono', Consolas, 'Liberation Mono', Menlo, Courier, monospace";
            result = String.format("<span style=\"color: %s;font-family: %s;\">%s</span>", color, font, p);
        }
        return Objects.isNull(result) && Objects.nonNull(dft) ? dft.get() : result;
    }

    public IAzureMessage getRawMessage() {
        return Optional.ofNullable(this.rawMessage).orElse(this);
    }

    private static String transformURLIntoLinks(String text) {
        String[] nonHrefLinks = HREF_PATTERN.split(text);
        final Matcher hrefMatcher = HREF_PATTERN.matcher(text);
        final List<String> hrefLinks = new ArrayList<>();
        while (hrefMatcher.find()) {
            hrefLinks.add(hrefMatcher.group());
        }
        final StringBuffer sb = new StringBuffer();
        // match http url only if it is not in <a href=xxx>xxx</a>
        for (int i = 0; i < nonHrefLinks.length; i++) {
            final Matcher m = URL_PATTERN.matcher(nonHrefLinks[i]);
            while (m.find()) {
                final String found = m.group(0);
                m.appendReplacement(sb, "<a href='" + found + "'>" + found + "</a>");
            }
            m.appendTail(sb);
            if (i < hrefLinks.size()) {
                sb.append(hrefLinks.get(i));
            }
        }
        return sb.toString();
    }

    protected String getErrorColor() {
        return "#FF0000";
    }

    protected String getValueColor() {
        return "#0000FF";
    }
}
