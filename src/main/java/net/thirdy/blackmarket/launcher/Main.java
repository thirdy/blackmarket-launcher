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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.airhacks.airfield.TakeDown;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author thirdy
 *
 */
public class Main extends Application {
	
	BooleanProperty showLaunchButton = new SimpleBooleanProperty(false);

	@Override
	public void start(Stage primaryStage) {
		List<String> args = getParameters().getRaw();
		
        String local = args.size() == 2 ? args.get(0) : "./lib";
        String remote = args.size() == 2 ? args.get(1) : "https://github.com/thirdy/blackmarket-release.git";
        
        if (args.size() == 1 && "-skip".equalsIgnoreCase(args.get(0))) {
        	finishedAllTasks();
        	return;
        }
        
        TextArea updaterMessagesTextArea = new TextArea();
        updaterMessagesTextArea.setWrapText(true);
        updaterMessagesTextArea.setId("updaterMessagesTextArea");
        
        PrintStream takedownPrintStream = new PrintStream(new Console(updaterMessagesTextArea), true) {
        	@Override
        	public void flush() {
        		append("\n");
        		super.flush();
        	}
        };
       
		TakeDown installer = new TakeDown(local, remote, takedownPrintStream);
        AirfieldService airfieldService = new AirfieldService(installer);
        showLaunchButton.bind(airfieldService.newChangesProperty());
        
        airfieldService.setOnSucceeded(e -> {
        	takedownPrintStream.close();
        	finishedAllTasks();
        });
        
        airfieldService.setOnFailed(e -> {
        	Dialogs.showExceptionDialog(airfieldService.getException());
        	finishedAllTasks();
        });
        
        TextArea changelogTextArea = new TextArea();
        changelogTextArea.setWrapText(true);
        changelogTextArea.setId("changelogTextArea");
        
        Button launchBtn = new Button("Launch");
        launchBtn.setId("launchBtn");
        launchBtn.setMinWidth(180);
        launchBtn.visibleProperty().bind(showLaunchButton);
        launchBtn.setOnAction(e -> runBlackmarket());
		
        Label status = new Label("Sucessfully started Blackmarket Launcher");
        status.textProperty().bind(airfieldService.messageProperty());
        status.visibleProperty().bind(showLaunchButton.not());
        
        ProgressBar p = new ProgressBar();
        p.setMaxWidth(Double.MAX_VALUE);
        p.progressProperty().bind(airfieldService.progressProperty());
        p.visibleProperty().bind(showLaunchButton.not());
        
        VBox updaterMessagesTextAreaPane = new VBox(updaterMessagesTextArea);
        updaterMessagesTextAreaPane.setId("updaterMessagesTextAreaPane");
        
        VBox changelogTextAreaPane = new VBox(changelogTextArea);
        changelogTextAreaPane.setId("changelogTextAreaPane");
        
        int randomIdx = new Random().nextInt(2);
        System.out.println("randomIdx: " + randomIdx);
        String images = new String[] {"/DarkshrineAnnouncement.jpg", "/gggbanner-plain.png"} [randomIdx];
        ImageView backgroundImageView = new ImageView(images);

        HBox centerPane = new HBox();
        
        if (randomIdx == 1) {
        	changelogTextArea.setStyle("-fx-text-fill: black; -fx-font-weight: normal; -fx-font-size: 11pt;  -fx-font-family: \"Serif\"");
        	updaterMessagesTextArea.setStyle("-fx-text-fill: black;");
        	updaterMessagesTextArea.setMinWidth(230);
        	centerPane.getChildren().addAll(changelogTextAreaPane, updaterMessagesTextAreaPane);
        } else {
        	centerPane.getChildren().addAll(updaterMessagesTextAreaPane, changelogTextAreaPane);
        }
        
        
        centerPane.setSpacing(10);
        centerPane.setId("centerPane");
        BorderPane widgetsPane = new BorderPane();
        widgetsPane.setId("widgetsPane");
		widgetsPane.setCenter(centerPane);
        StackPane bottom = new StackPane(p, status, launchBtn);
        widgetsPane.setBottom(bottom);

        backgroundImageView.setId("backgroundImageView");
        StackPane root = new StackPane(backgroundImageView, widgetsPane);
        root.setId("root");
        
		Scene scene = new Scene(root, backgroundImageView.getImage().getWidth(), 359);
		scene.getStylesheets().add(this.getClass().getResource("/css/blackmarket-launcher.css").toExternalForm());
		primaryStage.setTitle("Blackmarket Launcher");
		primaryStage.getIcons().add(new Image("/black-market-small.png"));
		primaryStage.setScene(scene);
		
		updaterMessagesTextArea.maxWidthProperty().bind(scene.widthProperty().multiply(0.5));
		
		Task<Void> changelogDownloaderTask = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				UrlReader urlReader = new UrlReader(s -> changelogTextArea.appendText(s + System.lineSeparator()));
				urlReader.download("http://poeblackmarket.github.io/changelog.txt");
				return null;
			}
		};
		
		changelogDownloaderTask.setOnFailed(e -> {
			changelogTextArea.appendText("Failed to download changelog: " + changelogDownloaderTask.getException().toString());
		});
		
		
		Task<Properties> controlDownloaderTask = new Task<Properties>() {
			
			@Override
			protected Properties call() throws Exception {
				String controlurl = "http://poeblackmarket.github.io/control.properties.txt";

				System.out.println( "Now downloading control properties from: " + controlurl );
				
				URL url = new URL(controlurl);
				InputStream in = url.openStream();
				Reader reader = new InputStreamReader(in, "UTF-8"); // for example
				 
				Properties prop = new Properties();
				try {
				    prop.load(reader);
				} finally {
				    reader.close();
				}
				
				System.out.println( "Successfully downloaded control properties from: " + controlurl );

				return prop;
			}
		};
		
		controlDownloaderTask.setOnSucceeded(e -> {
			Properties properties = controlDownloaderTask.getValue();
			boolean updateEnabled = Boolean.valueOf(properties.getProperty("update.enabled", "true"));
			String currentBaseVersion = properties.getProperty("current.base.version", "0.6");
			System.out.println("Control Properties: " + properties.toString());
			if (!"0.6".equals(currentBaseVersion)) {
				Dialogs.showInfo("Your current version of Blackmarket is now outdated. You can still continue to use your version of Blackmarket. Check out the website "
						+ "to grab the latest version: http://poeblackmarket.github.io", 
						"New version of Blackmarket!");
				runBlackmarket();
			} else if (updateEnabled) {
				airfieldService.start();
				new Thread(changelogDownloaderTask).run();
			}
		});
		
		controlDownloaderTask.setOnFailed(e -> {
			Dialogs.showExceptionDialog(controlDownloaderTask.getException());
			finishedAllTasks();
		});
		
		primaryStage.show();
		
		new Thread(controlDownloaderTask).run();
	}

	private void finishedAllTasks() {
		if (!showLaunchButton.getValue()) {
			runBlackmarket();
			Platform.exit();
		}
	}

	private void runBlackmarket() {
		String exe = "cmd /c start /D . blackmarket.exe";
		System.out.println("Running Blackmarket Actual: " + exe);
		try {
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
		private BooleanProperty newChangesProperty = new SimpleBooleanProperty(false); 
		private final TakeDown installer;
        public AirfieldService(TakeDown installer) {
			super();
			this.installer = installer;
		}
		@Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws BlackmarketUpdaterLauncher {
                	updateMessage("Now making sure that Blackmarket is up-to-date.");
                	boolean newChanges = installer.installOrUpdate();
                	newChangesProperty.set(newChanges);
                	updateMessage("Auto-update done. Launching Blackmarket.");
                	return null;
                }
            };
        }
		public BooleanProperty newChangesProperty() {
			return newChangesProperty;
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
	                    String s = String.valueOf((char) i);
						output.appendText(s);
	                }
	            });
	        }
	    }
}
