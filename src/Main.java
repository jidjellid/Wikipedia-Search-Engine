import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Main {
	
	public static void main(String[] args) {
		
	
		Relations t = new Relations();

		File dataPath = new File(args[0]);
		File parent = dataPath.getParentFile();
		
		if(new File(parent.getPath()+"/CLI.txt").exists() && new File(parent.getPath()+"/Pages.txt").exists() && new File(parent.getPath()+"/Relations.txt").exists()) {
			System.out.println("Saved data found, now loading :");
			t.loadData(parent.getPath());
			System.out.println("Finished loading");
		} else if(dataPath.isDirectory() && new File(dataPath.getPath()+"/CLI.txt").exists() && new File(dataPath.getPath()+"/Pages.txt").exists() && new File(dataPath.getPath()+"/Relations.txt").exists()) {
			System.out.println("Saved data found, now loading :");
			t.loadData(dataPath.getPath());
			System.out.println("Finished loading");
		} else {
			System.out.println("No saved data found, now computing :");
			t.computeFromCSV(args[0]);
			t.saveData(parent.getPath());
			System.out.println("Finished computing");
		}

		if(args.length > 1){
			System.out.println("Now searching for '"+args[1]+"/request.txt'");
			while(true){
				try {
					dataPath = new File(args[1]+"/request.txt");
					
					if(dataPath.exists() && !dataPath.isDirectory() && dataPath.length() != 0) { 
						
						BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[1]+"/request.txt"), StandardCharsets.UTF_8));
						BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]+"/response.txt"), StandardCharsets.UTF_8));

						String line = reader.readLine();
						System.out.println("Found request : " + line);

						for(String [] s : t.simpleSearch(line)){
							for(int i = 0; i < s.length; i++){
								writer.write(s[i]);
								if(i < s.length-1){
									writer.write(";");
								}
							}
								
							writer.write("\n");
						}
						reader.close();
						writer.close();
						dataPath.delete();
					}
					
					Thread.sleep(500);//Sleep 0.5s
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			Scanner input = new Scanner(System.in);
			String line;

			while(!(line = input.nextLine()).equals("exit")){
				System.out.println("Results for : '"+line+"'");
				for(String [] s : t.simpleSearch(line)){
					System.out.println("Title : "+s[0]+" | Link : "+s[1]+" | Score : "+s[2]);
				}
				System.out.println();
			}

			input.close();
		}

		System.exit(0);
	}
}
