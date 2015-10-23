/*
 * Copyright (C) 2015 thirdy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.thirdy.blackmarket.updater;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.airhacks.airfield.TakeDown;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * @author thirdy
 *
 */
public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {
		List<String> args = getParameters().getRaw();
		
        String local = args.size() == 2 ? args.get(0) : "./lib";
        String remote = args.size() == 2 ? args.get(1) : "https://github.com/thirdy/blackmarket-release.git";
        
        if (args.size() == 1 && "-skip".equalsIgnoreCase(args.get(0))) {
        	Platform.exit();
			return;
		}
        
        TextArea ta = TextAreaBuilder.create().prefWidth(800).prefHeight(600).wrapText(true).build();
        Console console = new Console(ta);
        PrintStream ps = new PrintStream(console, true);
        System.setOut(ps);
        System.setErr(ps);
       
		TakeDown installer = new TakeDown(local, remote);
        AirfieldService airfieldService = new AirfieldService(installer);
        
        airfieldService.setOnSucceeded(e -> runBlackmarketAndExit(local));
        airfieldService.setOnFailed(e -> {
        	Dialogs.showExceptionDialog(airfieldService.getException());
//        	Platform.exit();
        	runBlackmarketAndExit(local);
        });
		
        Label status = new Label("Sucessfully started Blackmarket Updater");
        status.textProperty().bind(airfieldService.messageProperty());
        
        ProgressBar p = new ProgressBar();
        p.setMaxWidth(Double.MAX_VALUE);
//        p.setMaxSize(150, 150);
        p.progressProperty().bind(airfieldService.progressProperty());

        StackPane bottom = new StackPane(p, status);
        BorderPane root = new BorderPane();
		root.setCenter(ta);
		root.setBottom(bottom);

		Scene scene = new Scene(root, 440, 350);
		scene.getStylesheets().add(this.getClass().getResource("/css/blackmarket-launcher.css").toExternalForm());
		primaryStage.setTitle("Blackmarket Updater");
		primaryStage.getIcons().add(new Image("/black-market-small.png"));
		primaryStage.setScene(scene);
		primaryStage.show();
		
		airfieldService.start();
	}

	// CAN'T GET THIS TO WORK
	private static void runBlackmarketAndExit(String local) {
//		String exe = local + "/blackmarket.exe";
//		String exe = "notepad.exe";
		String exe = "cmd /c start /D . blackmarket.exe";
//		CommandLine cmdLine = new CommandLine(exe);
//		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
//		Executor executor = new DefaultExecutor();
//		executor.setExitValue(1);


		System.out.println("Running Blackmarket Actual: " + exe);
		try {

//			executor.execute(cmdLine, resultHandler);
//			resultHandler.waitFor();

//			String[] args1 = {exe};
//			Runtime r = Runtime.getRuntime();
//			Process p = r.exec(args1);
			Process p = new ProcessBuilder(exe.split("\\s"))
					.start();
			p.waitFor();
			System.out.println("Successfully started Blackmarket Actual, launcher is now exiting.");
		} catch ( Exception e) {
			System.out.println("Failed to launch blackmarket.exe");
			e.printStackTrace();
			Dialogs.showExceptionDialog(e);
		}
		Platform.exit();
	}

	public static void main(String[] args) {
		System.out.println("Commandline usage:");
		System.out.println("param1: [PATH_TO_LOCAL_APP]");
		System.out.println("param2: [PATH_TO_REMOTE_GIT_REPO]");
		System.out.println("or");
		System.out.println("param1: -skip");
	 
		launch(args);
	}
	
	private static class AirfieldService extends Service<Void> {
		private final TakeDown installer;
        public AirfieldService(TakeDown installer) {
			super();
			this.installer = installer;
		}
		@Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws BlackmarketUpdaterException {
                	updateMessage("Now making sure that Blackmarket is up-to-date.");
                	installer.installOrUpdate();
                	updateMessage("Auto-update done. Launching Blackmarket.");
                	return null;
                }
            };
        }
    }

	 public static class Console extends OutputStream {

	        private TextArea output;

	        public Console(TextArea ta) {
	            this.output = ta;
	        }

	        @Override
	        public void write(int i) throws IOException {
	        	Platform.runLater(new Runnable() {
	                public void run() {
	                    output.appendText(String.valueOf((char) i));
	                }
	            });
	        }
	    }
}
