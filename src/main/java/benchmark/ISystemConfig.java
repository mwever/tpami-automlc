package benchmark;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

@Sources({ "file:./system.properties" })
public interface ISystemConfig extends Config {

	@Key("python.cmd")
	@DefaultValue("python3")
	public String getPythonCommand();

}
