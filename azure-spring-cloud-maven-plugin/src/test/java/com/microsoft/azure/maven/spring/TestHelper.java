/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.ServiceResourceInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Subscriptions;
import com.microsoft.azure.maven.common.telemetry.AppInsightHelper;
import com.microsoft.rest.RestException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.project.MavenProject;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

public class TestHelper {
    public static Model readMavenModel(File pomFile) throws IOException {
        final ModelReader reader = new DefaultModelReader();
        return reader.read(pomFile, Collections.emptyMap());
    }

    public static void mockAppInsightHelper() throws Exception {
        // mock for AppInsightHelper.INSTANCE
        final TelemetryClient client = mock(TelemetryClient.class);
        PowerMockito.whenNew(TelemetryClient.class).withAnyArguments().thenReturn(client);
        final AppInsightHelper mockAppInsightHelper = mock(AppInsightHelper.class);
        Whitebox.setInternalState(AppInsightHelper.class, "INSTANCE", mockAppInsightHelper);

    }

    public static List<MavenProject> prepareParentChildProjects() throws IOException {
        final List<MavenProject> res = new ArrayList<>();
        res.add(createParentProject());
        res.add(createChildProject("core"));
        res.add(createChildProject("service"));
        return res;
    }

    public static MavenProject createChildProject(String projectName) throws IOException {
        final File pom = new File(TestHelper.class.getResource("/maven/projects/parent-project/" + projectName + "/pom.xml").getFile());
        return createProject(pom);
    }

    public static MavenProject createParentProject() throws IOException {
        final File pom = new File(TestHelper.class.getResource("/maven/projects/parent-project/pom.xml").getFile());
        return createProject(pom);
    }

    public static void mockAzureWithSubs(Subscriptions subs) {
        mockStatic(Azure.class);
        final Azure.Configurable configuration = mock(Azure.Configurable.class);
        final Authenticated azureClient = mock(Authenticated.class);
        when(configuration.authenticate(ArgumentMatchers.any(AzureTokenCredentials.class))).thenReturn(azureClient);
        when(Azure.configure()).thenReturn(configuration);
        when(azureClient.subscriptions()).thenReturn(subs);
    }

    public static Subscriptions createMockSubscriptions(int count) {
        final Subscriptions subs = mock(Subscriptions.class);
        final PagedList<Subscription> list = new PagedList<Subscription>() {

            @Override
            public com.microsoft.azure.Page<Subscription> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
        for (int i = 0; i < count; i++) {
            final Subscription mockSub = mock(Subscription.class);
            if (i == 0) {
                when(mockSub.subscriptionId()).thenReturn("new_subs_id");
                when(mockSub.displayName()).thenReturn("new_subs_name");
            } else {
                when(mockSub.subscriptionId()).thenReturn("new_subs_id" + (i + 1));
                when(mockSub.displayName()).thenReturn("new_subs_name" + (i + 1));
            }
            list.add(mockSub);
        }

        when(subs.list()).thenReturn(list);
        return subs;
    }

    public static List<ServiceResourceInner> createServiceList() {
        return Arrays.asList(createMockAppClusterResourceInner("testCluster1"),
                createMockAppClusterResourceInner("testCluster2"),
                createMockAppClusterResourceInner("testCluster3"));
    }

    private static ServiceResourceInner createMockAppClusterResourceInner(String name) {
        final ServiceResourceInner mockResource = mock(ServiceResourceInner.class);
        when(mockResource.name()).thenReturn(name);
        return mockResource;
    }

    private static MavenProject createProject(final File pomFile) throws IOException {
        final MavenProject project = Mockito.mock(MavenProject.class);
        final Model model = TestHelper.readMavenModel(pomFile);
        Mockito.when(project.getModel()).thenReturn(model);
        final String name = model.getName();
        Mockito.when(project.getName()).thenReturn(name);
        final String packaging = model.getPackaging();
        Mockito.when(project.getPackaging()).thenReturn(packaging);
        final String groupId = model.getGroupId();
        Mockito.when(project.getGroupId()).thenReturn(groupId);

        final String version = model.getVersion();
        Mockito.when(project.getGroupId()).thenReturn(version);
        final String artifactId = model.getArtifactId();
        Mockito.when(project.getArtifactId()).thenReturn(artifactId);
        return project;
    }

}
