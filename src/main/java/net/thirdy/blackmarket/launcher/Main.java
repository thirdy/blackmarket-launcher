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
package net.thirdy.blackmarket.launcher;

import java.io.IOException;
import java.util.List;

import com.airhacks.airfield.TakeDown;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author thirdy
 *
 */
public class Main  extends Application {

	@Override
	public void start(Stage primaryStage) {
		List<String> args = getParameters().getRaw();
		
        String local = args.size() == 2 ? args.get(0) : "./lib";
        String remote = args.size() == 2 ? args.get(1) : "https://github.com/thirdy/blackmarket-release.git";
        
        if (args.size() == 1 && "-skip".equalsIgnoreCase(args.get(0))) {
			runBlackmarketAndExit(local);
			return;
		}
       
		TakeDown installer = new TakeDown(local, remote);
        AirfieldService airfieldService = new AirfieldService(installer);
        
        airfieldService.setOnSucceeded(e -> Main.runBlackmarketAndExit(local));
        airfieldService.setOnFailed(e -> {
        	Dialogs.showExceptionDialog(airfieldService.getException());
        	Platform.exit();
        });
		
        Label status = new Label("Sucessfully started Blackmarket Launcher");
        status.textProperty().bind(airfieldService.messageProperty());
        
        ProgressIndicator p = new ProgressIndicator();
        p.setMaxSize(150, 150);
        
        p.progressProperty().bind(airfieldService.progressProperty());

		StackPane root = new StackPane();
		root.getChildren().addAll(p, status);

		Scene scene = new Scene(root, 300, 250);

		primaryStage.setTitle("Blackmarket Launcher");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		airfieldService.start();
	}

	private static void runBlackmarketAndExit(String local) {
		System.out.println("Running Blackmarket Actual");
		try {
			Process p = new ProcessBuilder(local + "/blackmarket-actual.exe").start();
			System.out.println("Successfully started Blackmarket Actual, launcher is now exiting.");
		} catch (IOException e) {
			System.out.println("Failed to launch blackmarket-actual.exe");
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
                protected Void call() throws BlackmarketLauncherException {
                	updateMessage("Now making sure that Blackmarket is up-to-date.");
                	installer.installOrUpdate();
                	updateMessage("Auto-update done. Launching Blackmarket.");
                	return null;
                }
            };
        }
    }

}
