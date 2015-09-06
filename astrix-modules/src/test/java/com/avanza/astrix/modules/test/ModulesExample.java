/*
 * Copyright 2014 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.astrix.modules.test;

import java.util.Scanner;

import com.avanza.astrix.modules.Module;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.modules.Modules;
import com.avanza.astrix.modules.ModulesConfigurer;

public class ModulesExample {
	
	public interface Ping {
		String ping(String message);
	}

	static class PingImpl implements Ping {
		public String ping(String msg) {
			return msg;
		}
	}

	interface PingUi {
		String readMsg();
		void displayMsg(String msg);
	}

	static class ConsoleUi implements PingUi {
		private Scanner consoleReader = new Scanner(System.in);
		public String readMsg() {
			return consoleReader.nextLine();
		}
		public void displayMsg(String msg) {
			System.out.println(msg);
		}
	}

	interface PingApp {
		void run();
	}

	static class PingAppImpl implements PingApp {

		private PingUi pingUi;
		private Ping ping;
		
		public PingAppImpl(PingUi pingUi, Ping ping) {
			this.pingUi = pingUi;
			this.ping = ping;
		}

		public void run() {
			pingUi.displayMsg("Enter message:");
			String msg = pingUi.readMsg();
			pingUi.displayMsg(ping.ping(msg));
		}
	}

	static class PingCoreModule implements Module {
		public void prepare(ModuleContext context) {
			context.bind(Ping.class, PingImpl.class); // Bind Ping interface to PingImpl.class
			context.bind(PingApp.class, PingAppImpl.class);
			
			context.importType(PingUi.class); // Import PingUi into this module
			
			context.export(PingApp.class); // Make PingApp available outside the module
		}
	}

	static class PingUiModule implements Module {
		public void prepare(ModuleContext context) {
			context.bind(PingUi.class, ConsoleUi.class);
		
			context.export(PingUi.class); // Make PingUi available to other modules
		}
	}
	
	public static void main(String[] args) {
		// Configure modules
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new PingCoreModule());
		modulesConfigurer.register(new PingUiModule());
		
		// Create Modules instance
		Modules modules = modulesConfigurer.configure();
		
		// Use Modules to create PingApp
		PingApp pingApp = modules.getInstance(PingApp.class); 
		pingApp.run();
	}


}
