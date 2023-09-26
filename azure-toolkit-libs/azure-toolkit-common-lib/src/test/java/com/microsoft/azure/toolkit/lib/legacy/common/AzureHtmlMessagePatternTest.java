package com.microsoft.azure.toolkit.lib.legacy.common;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureHtmlMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import org.junit.Assert;
import org.junit.Test;

public class AzureHtmlMessagePatternTest {

    @Test
    public void startWithNoSpace() {
        AzureString urlStartWithNoSpace = AzureString.fromString("https://github.com/microsoft/azure-tools-for-java");
        final AzureHtmlMessage message = new AzureHtmlMessage(IAzureMessage.Type.INFO, urlStartWithNoSpace);
        final String expectedResult = "<a href='https://github.com/microsoft/azure-tools-for-java'>https://github.com/microsoft/azure-tools-for-java</a>";
        Assert.assertEquals(expectedResult, message.getContent());
    }

    @Test
    public void startWithMultiSpaces() {
        AzureString urlStartWithMultiSpaces = AzureString.fromString("    https://github.com/microsoft/azure-tools-for-java");
        final AzureHtmlMessage message = new AzureHtmlMessage(IAzureMessage.Type.INFO, urlStartWithMultiSpaces);
        final String expectedResult = "    <a href='https://github.com/microsoft/azure-tools-for-java'>https://github.com/microsoft/azure-tools-for-java</a>";
        Assert.assertEquals(expectedResult, message.getContent());
    }

    @Test
    public void startWithSpecialChar() {
        AzureString urlStartWithSpecialChar = AzureString.fromString(": https://github.com/microsoft/azure-tools-for-java");
        final AzureHtmlMessage message = new AzureHtmlMessage(IAzureMessage.Type.INFO, urlStartWithSpecialChar);
        final String expectedResult = ": <a href='https://github.com/microsoft/azure-tools-for-java'>https://github.com/microsoft/azure-tools-for-java</a>";
        Assert.assertEquals(expectedResult, message.getContent());
    }

    @Test
    public void containsHref() {
        AzureString textStrContainsHref = AzureString.fromString("refer to <a href='https://azure.github.io/azure-sdk/releases/latest/java.html'>Azure SDK Releases</a>");
        final AzureHtmlMessage message = new AzureHtmlMessage(IAzureMessage.Type.INFO, textStrContainsHref);
        final String expectedResult = "refer to <a href='https://azure.github.io/azure-sdk/releases/latest/java.html'>Azure SDK Releases</a>";
        Assert.assertEquals(expectedResult, message.getContent());
    }

    @Test
    public void containsMultiHref() {
        AzureString textStrContainsHref = AzureString.fromString("refer to <a href='https://azure.github.io/azure-sdk/releases/latest/java.html'>Azure SDK Releases</a> and <a href='https://github.com/microsoft/azure-tools-for-java'>https://github.com/microsoft/azure-tools-for-java </a>");
        final AzureHtmlMessage message = new AzureHtmlMessage(IAzureMessage.Type.INFO, textStrContainsHref);
        final String expectedResult = "refer to <a href='https://azure.github.io/azure-sdk/releases/latest/java.html'>Azure SDK Releases</a> and <a href='https://github.com/microsoft/azure-tools-for-java'>https://github.com/microsoft/azure-tools-for-java </a>";
        Assert.assertEquals(expectedResult, message.getContent());
    }
}
