/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/

 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.conformance.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.conformanceTestApp.ConformanceTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConformanceTests extends FATServletClient {

    private static final String INSTALL_MCP_CONFORMANCE_PACKAGE = "npm install -g @modelcontextprotocol/conformance@0.1.9";

    // Artifactory Properties
    private static Properties startupProps = new Properties();
    private static final String ARTIFACTORY_TOKEN_KEY = "artifactory.download.token";
    private static final String ARTIFACTORY_USER_KEY = "artifactory.download.user";
    private static final String ARTIFACTORY_SERVER_KEY = "artifactory.download.server";
    private static String artToken;
    private static String artUser;
    private static String artServer;

    private static final String PATH_TO_ARTIFACTORY_NPM_MIRROR = "/artifactory/api/npm/wasliberty-npm-remote/";
    private static boolean ARTIFACTORY_IS_CONFIGURED = false;

    // Container config
    private static String MCP_SERVER_URL_FROM_CONTAINER = "http://host.docker.internal:";
    private static final String DOCKER_REGISTRY = "public.ecr.aws/docker/library/node:18-alpine";

    @Server("mcp-conformance-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/conformanceTests");
    private final static Logger logger = Logger.getLogger(ConformanceTests.class.getName());

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(DOCKER_REGISTRY).withCommand("tail", "-f", "/dev/null")
                                                                                         .withAccessToHost(true)
                                                                                         .withLogConsumer(new SimpleLogConsumer(ConformanceTests.class, "nodejs-container"));

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "conformanceTests.war").addPackage(ConformanceTools.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$")); // regex matches string that ends with /mcp e.g. "MCP server endpoint: http://macbookpro.home:8010/toolTest/mcp"
        loadArtifactoryConfigIfPresent();
        containerSetup();
    }

    private static void loadArtifactoryConfigIfPresent() {
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path propsPath = userHome.resolve("gradle.startup.properties");

        if (!Files.exists(propsPath)) {
            logger.info("No gradle.startup.properties found. Will use public npm registry.");
            return;
        }

        try (InputStream inputStream = Files.newInputStream(propsPath)) {
            startupProps.load(inputStream);
            ARTIFACTORY_IS_CONFIGURED = artifactoryConfigIsPresent();
        } catch (IOException e) {
            logger.warning("Failed to load gradle.startup.properties: " + e.getMessage());
        }
    }

    /**
     * Build MCP Server URL
     * Force modelcontextprotocol/conformance to always be a specific version for build compatibility
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private static void containerSetup() throws IOException, InterruptedException {
        MCP_SERVER_URL_FROM_CONTAINER += server.getHttpDefaultPort() + "/conformanceTests/mcp";

        if (ARTIFACTORY_IS_CONFIGURED) {
            String credentials = artUser + ":" + artToken;
            String credentialBase64 = java.util.Base64.getEncoder().encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String artifactoryServer = artServer + PATH_TO_ARTIFACTORY_NPM_MIRROR;
            String npmMirrorSetup = "npm config set registry https://" + artifactoryServer + " && " +
                                    "npm config set //" + artifactoryServer + ":_auth ";

            runCommandInContainer(npmMirrorSetup + credentialBase64);
        }

        runCommandInContainer(INSTALL_MCP_CONFORMANCE_PACKAGE);
    }

    private static void runCommandInContainer(String command) throws IOException, InterruptedException {
        Container.ExecResult result = container.execInContainer("sh", "-c", command);
        assertEquals(0, result.getExitCode());
    }

    private static boolean artifactoryConfigIsPresent() {
        artUser = startupProps.getProperty(ARTIFACTORY_USER_KEY);
        artToken = startupProps.getProperty(ARTIFACTORY_TOKEN_KEY);
        artServer = startupProps.getProperty(ARTIFACTORY_SERVER_KEY);

        boolean validUser = (artUser != null && !artUser.isBlank());
        boolean validToken = (artToken != null && !artToken.isBlank());
        boolean validServer = (artServer != null && !artServer.isBlank());

        return validUser && validToken && validServer;
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(
                          "CWMCM0014E", // An Internal Server Error occurred whilst processing the JSON-RPC request.
                          "CWMCM0010E", //  Tool method threw an unexpected exception
                          "CWMCM0011E" // An internal server error occurred
        );
    }

    @Test
    public void testModelContextProtocolConformance() throws Exception {
        checkMCPServerConformanceWithTest("server-initialize");
        checkMCPServerConformanceWithTest("tools-list");
        checkMCPServerConformanceWithTest("tools-call-simple-text");
        checkMCPServerConformanceWithTest("tools-call-image");
        checkMCPServerConformanceWithTest("tools-call-audio");
        checkMCPServerConformanceWithTest("tools-call-error");
    }

    /**
     * Run the MCP conformance test scenario using nodejs on the container
     *
     * @param scenarioName
     * @throws IOException
     * @throws InterruptedException
     */
    private void checkMCPServerConformanceWithTest(String scenarioName) throws IOException, InterruptedException {
        String command = "npx @modelcontextprotocol/conformance server --url " + MCP_SERVER_URL_FROM_CONTAINER + " --scenario " + scenarioName;
        Container.ExecResult result = container.execInContainer("sh", "-c", command);
        assertThat("MCP Conformance test scenario " + scenarioName + " failed", result.getStdout(), containsString("SUCCESS"));
    }
}