/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.worker.compute.app;

import com.iexec.common.docker.DockerRunRequest;
import com.iexec.common.docker.DockerRunResponse;
import com.iexec.common.task.TaskDescription;
import com.iexec.common.tee.TeeEnclaveConfiguration;
import com.iexec.common.utils.IexecEnvUtils;
import com.iexec.worker.config.WorkerConfigurationService;
import com.iexec.worker.docker.DockerService;
import com.iexec.worker.tee.scone.TeeSconeService;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class AppComputeService {

	private final WorkerConfigurationService workerConfigService;
	private final DockerService dockerService;
	private final TeeSconeService teeSconeService;

	public AppComputeService(WorkerConfigurationService workerConfigService, DockerService dockerService,
			TeeSconeService teeSconeService) {
		this.workerConfigService = workerConfigService;
		this.dockerService = dockerService;
		this.teeSconeService = teeSconeService;
	}

	@Slf4j
	public static class K8sWorker implements Runnable {

		private String cmd;
		private String script;

		public K8sWorker(String script, String cmd) {
			this.cmd = cmd;
			this.script = script;
		}

		@Override
		public void run() {
			log.info("This is a log message from the new thread.");
			try {
				switch (script) {
				case "start":
					startWorker();
					break;
				case "stop":
					stopWorker();
					break;
				default:
					throw new IllegalArgumentException("Unexpected value: " + script);
				}

			} catch (IOException | InterruptedException e) {
				log.info("{}", e);
				e.printStackTrace();
			}
		}

		private void startWorker() throws IOException, InterruptedException {
			Process process = executeScript("/start_script.sh", Arrays.asList(cmd.split(",")));

			// Capture the script's output
			InputStream inputStream = process.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader reader = new BufferedReader(inputStreamReader);

			// Log the script's output
			String line;
			while ((line = reader.readLine()) != null) {
				log.info("{}", line);
			}

			// Wait for the process to complete
			int exitCode = process.waitFor();

			// Log the script's exit code
			log.info("K8s Worker Starting Script Execution Completed with Exit Code: {}", exitCode);
		}

		private void stopWorker() throws IOException, InterruptedException {
			Process process = executeScript("/stop_script.sh", new ArrayList<>());

			// Capture the script's output
			InputStream inputStream = process.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader reader = new BufferedReader(inputStreamReader);

			// Log the script's output
			String line;
			while ((line = reader.readLine()) != null) {
				log.info("{}", line);
			}
			// Wait for the process to complete
			int exitCode = process.waitFor();

			// Log the script's exit code
			log.info("K8s Worker Cleaning Completed with Exit Code: {}", exitCode);
		}

		private Process executeScript(String scriptName, List<String> args) throws IOException, InterruptedException {
			// Get the input stream of the script from the resources
			InputStream scriptStream = K8sWorker.class.getResourceAsStream(scriptName);

			// Create a temporary file to copy the script content
			File tempScript = File.createTempFile("tempScript", ".sh");

			// Copy the script content from the resource input stream to the temporary file
			try (OutputStream outputStream = new FileOutputStream(tempScript)) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = scriptStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
			}

			// Set the temporary script file to be executable
			tempScript.setExecutable(true);

			// Command and its arguments as a list of strings
			List<String> command = new ArrayList<>();
			command.add("bash"); // Command or program to execute
			command.add(tempScript.getAbsolutePath()); // First argument
			command.addAll(args); // Second argument (if needed)

			// Create a ProcessBuilder with the command and arguments
			ProcessBuilder processBuilder = new ProcessBuilder(command);

			processBuilder.redirectErrorStream(true);

			// Start the process
			return processBuilder.start();

		}

	}

	public AppComputeResponse runCompute(TaskDescription taskDescription, String secureSessionId) {
		String chainTaskId = taskDescription.getChainTaskId();
		List<String> env = IexecEnvUtils.getComputeStageEnvList(taskDescription);
		if (taskDescription.isTeeTask()) {
			TeeEnclaveConfiguration enclaveConfig = taskDescription.getAppEnclaveConfiguration();
			List<String> strings = teeSconeService.buildComputeDockerEnv(secureSessionId,
					enclaveConfig != null ? enclaveConfig.getHeapSize() : 0);
			env.addAll(strings);
		}

		List<String> binds = Arrays.asList(dockerService.getInputBind(chainTaskId),
				dockerService.getIexecOutBind(chainTaskId));

		// k8s
		log.info("k8s starting...");
		// Create and start a new thread

		Thread startThread = new Thread(new K8sWorker("start", taskDescription.getCmd()));

		// Start the thread
		startThread.start();
		
		String cmd = taskDescription.getCmd();
		log.info(cmd);
		cmd = cmd.concat(" ");
		cmd = cmd.concat(chainTaskId);
		log.info(chainTaskId);
		log.info(cmd);

		DockerRunRequest runRequest = DockerRunRequest.builder().chainTaskId(chainTaskId)
				.imageUri(taskDescription.getAppUri()).containerName(getTaskContainerName(chainTaskId))
				.cmd(cmd).env(env).binds(binds)
				.maxExecutionTime(taskDescription.getMaxExecutionTime()).isSgx(taskDescription.isTeeTask())
				.shouldDisplayLogs(taskDescription.isDeveloperLoggerEnabled()).build();
		// Enclave should be able to connect to the LAS
		if (taskDescription.isTeeTask()) {
			runRequest.setDockerNetwork(workerConfigService.getDockerNetworkName());
		}
		DockerRunResponse dockerResponse = dockerService.run(runRequest);

		Thread stopThread = new Thread(new K8sWorker("stop", taskDescription.getCmd()));

		// Start the thread
		stopThread.start();
		
		log.info("k8s stopped and cleaned!");

		return AppComputeResponse.builder().isSuccessful(dockerResponse.isSuccessful())
				.stdout(dockerResponse.getStdout()).stderr(dockerResponse.getStderr()).build();
	}

	// We use the name "worker1-0xabc123" for app container to avoid
	// conflicts when running multiple workers on the same machine.
	// Exp: integration tests
	private String getTaskContainerName(String chainTaskId) {
		return workerConfigService.getWorkerName() + "-" + chainTaskId;
	}
}