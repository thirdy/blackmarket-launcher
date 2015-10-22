/*
 */
package com.airhacks.airfield;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.TextProgressMonitor;

import net.thirdy.blackmarket.updater.BlackmarketUpdaterException;

/**
 *
 * @author adam-bien.com
 */
public class TakeDown {

    private final String remotePath;
    private final String localPath;
    private Git git;
	private ProgressMonitor monitor;

    public TakeDown(String localPath, String remotePath) {
    	monitor = new TextProgressMonitor();
        this.remotePath = remotePath;
        this.localPath = localPath;
    }

    void initialDownload() throws BlackmarketUpdaterException {
        try {
            this.git = Git.cloneRepository()
                    .setURI(remotePath)
                    .setProgressMonitor(monitor)
                    .setDirectory(new File(localPath))
                    .call();
            System.out.println("+App installed into: " + this.localPath);
        } catch (GitAPIException ex) {
            throw new BlackmarketUpdaterException("--Cannot download files: " + ex.getMessage(), ex);
        }

    }

    void update() throws BlackmarketUpdaterException {
    	 try {
             this.git
             	.reset()
             	.setMode(ResetCommand.ResetType.HARD).call();

             System.out.println("+Changed files removed");
         } catch (GitAPIException ex) {
             throw new BlackmarketUpdaterException("Cannot reset local repository", ex);
         }
        PullCommand command = this.git.pull();
        command.setProgressMonitor(monitor);
        try {
            PullResult pullResult = command.call();
            if (pullResult.isSuccessful()) {
                System.out.println("+Files updated, ready to start!");
            } else {
                System.out.println("--Download was not successful " + pullResult.toString());
            }
        } catch (GitAPIException ex) {
        	throw new BlackmarketUpdaterException("Exception on PullCommand.call()", ex);
        }
    }

    boolean openLocal() {
        File localRepo = new File(this.localPath);
        try {
            this.git = Git.open(localRepo);
        } catch (IOException ex) {
            System.out.println("-" + ex.getMessage());
            return false;
        }
        System.out.println("+Application already installed at: " + this.localPath);
        return true;
    }

    public void installOrUpdate() throws BlackmarketUpdaterException {
        boolean alreadyInstalled = openLocal();
        if (alreadyInstalled) {
            update();
        } else {
            initialDownload();
        }
    }

}
