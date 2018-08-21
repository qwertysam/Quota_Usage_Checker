import java.awt.Point;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class Settings {
	public boolean isAlwaysOntop;
	public int minutesToUpdate;
	public Point frameLocation;
	
	public Settings(boolean isAlwaysOntop, int minutesToUpdate, Point frameLocation) {
		this.isAlwaysOntop = isAlwaysOntop;
		this.minutesToUpdate = minutesToUpdate;
		this.frameLocation = frameLocation;
	}

	public static Settings load() {
		try (Reader reader = new FileReader(getLocation())) {
			Gson gson = new GsonBuilder().serializeNulls().create();

			final Type TYPE = new TypeToken<Settings>() {
			}.getType();

			return gson.fromJson(reader, TYPE);
		} catch (JsonSyntaxException e) {
			System.out.println("Error: Can't read file \"" + getLocation() + "\".");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public boolean save() {
		try (Writer writer = new FileWriter(getLocation())) {
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
			gson.toJson(this, writer);
		} catch (IOException e) {
			System.out.println("Error: Can't write file \"" + getLocation() + "\".");
			return false;
		}
		
		return true;
	}
	
	public static String getLocation() {
		return (new File("").equals(new File(File.separator))
				? System.getProperty("user.home") : new File("").getAbsolutePath()) + File.separator + "Usage_Checker.json";
	}
}
