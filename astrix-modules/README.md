Astrix Modules is a simple dependency injection (DI) framework. In addition to allowing simple DI it also allows modularising a code base by breaking it up into multiple modules. A module has an explicit boundary:

* All type bindings are internal to the module only
* A module can export a type which makes it available to other Modules
* A module can also import types that are exported by other modules, thereby making that type available internally in the module


### Example Usage

```java
public interface Ping {
	String ping(String message);
}

class PingImpl implements Ping {
	public String ping(String msg) {
		return msg;
	}
}

interface PingUi {
	String readMsg();
	void displayMsg(String msg);
}

class ConsoleUi implements PingUi {
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

class PingAppImpl implements PingApp {

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

class PingCoreModule implements Module {
	public void prepare(ModuleContext context) {
		context.bind(Ping.class, PingImpl.class); // Bind Ping interface to PingImpl.class
		context.bind(PingApp.class, PingAppImpl.class);
		
		context.importType(PingUi.class); // Import PingUi into this module
		
		context.export(PingApp.class); // Make PingApp available outside the module
	}
}

class PingUiModule implements Module {
	public void prepare(ModuleContext context) {
		context.bind(PingUi.class, ConsoleUi.class);
	
		context.export(PingUi.class); // Make PingUi available to other modules
	}
}

// Configure modules
ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
modulesConfigurer.register(new PingCoreModule());
modulesConfigurer.register(new PingUiModule());

// Create Modules instance
Modules modules = modulesConfigurer.configure();

// Use Modules to create PingApp. Only exported types can created explicitly
PingApp pingApp = modules.getInstance(PingApp.class); 
pingApp.run();
```
