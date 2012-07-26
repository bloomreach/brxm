/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.testutils.deployer;

import java.io.File;
import java.util.Vector;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Deployer {

    protected Logger logger = LoggerFactory.getLogger(Deployer.class);


    private DeploymentManager deploymentManager;
    private boolean done;

    public Deployer(String location, String username, String password) throws Exception {
        try {
            DeploymentFactoryManager dfm = DeploymentFactoryManager.getInstance();
            try {
                Class dfClass = Class.forName("com.sun.enterprise.deployapi.SunDeploymentFactory");
                DeploymentFactory dfInstance = (DeploymentFactory) dfClass.newInstance();
                dfm.registerDeploymentFactory(dfInstance);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            }
            deploymentManager = dfm.getDeploymentManager(location, username, password);
        } catch (DeploymentManagerCreationException ex) {
            logger.error("Cannot obtain deploy platform", ex);
            throw new Exception("Cannot obtain deploy platform", ex);
        }
    }

    public Deployer() throws Exception {
        this("deployer:Sun:AppServer::localhost:4848", "admin", "adminadmin");
    }

    /**
     * @param args the command line arguments
     */
    public void deploy(String ear) {
        done = false;
        synchronized (this) {

            final ProgressObject deplProgress = deploymentManager.distribute(deploymentManager.getTargets(), new File(ear), null);
            deplProgress.addProgressListener(new ProgressListener() {

                public void handleProgressEvent(ProgressEvent event) {
                    DeploymentStatus deployStatus = event.getDeploymentStatus();
                    System.out.println("Deployment status: " + deployStatus.getState().toString() + " " + deployStatus.getMessage());
                    if (deployStatus.isCompleted()) {
                        logger.info("Deployment completed");
                        ProgressObject enableProgress = deploymentManager.start(deplProgress.getResultTargetModuleIDs());
                        enableProgress.addProgressListener(new ProgressListener() {

                            public void handleProgressEvent(ProgressEvent evt) {
                                DeploymentStatus status = evt.getDeploymentStatus();
                                System.out.println("Enabling status: " + status.getState().toString() + " " + status.getMessage());
                                if (status.isCompleted()) {
                                    done = true;
                                    synchronized (Deployer.this) {
                                        Deployer.this.notify();
                                    }
                                    logger.info("Enabling completed");
                                }
                            }
                            });
                        if(enableProgress.getDeploymentStatus().isCompleted()) {
                            done = true;
                            synchronized (Deployer.this) {
                                Deployer.this.notify();
                            }
                            logger.info("Enabling completed");
                        }
                    }
                }
                });
            while (!done) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    logger.info("Wait for deploy process interrupted", ex);
                }
            }
            logger.info("Application running");
        }
    }

    public void undeploy(String ear) throws Exception {
        final TargetModuleID[] modules;

        if (ear.contains(File.separator)) {
            ear = ear.substring(ear.lastIndexOf(File.separator) + 1, ear.length());
        }
        try {
            TargetModuleID[] ears = deploymentManager.getAvailableModules(ModuleType.EAR, deploymentManager.getTargets());
            TargetModuleID[] wars = deploymentManager.getAvailableModules(ModuleType.WAR, deploymentManager.getTargets());
            TargetModuleID[] rars = deploymentManager.getAvailableModules(ModuleType.RAR, deploymentManager.getTargets());
            Vector<TargetModuleID> selectedModules = new Vector<TargetModuleID>();
            for (int i = 0; i < ears.length; i++) {
                if (ear == null || (ear.equals(ears[i].getModuleID()) || ear.equals(ears[i].getModuleID() + ".ear"))) {
                    selectedModules.add(ears[i]);
                }
            }
            for (int i = 0; i < wars.length; i++) {
                if (ear != null && (ear.equals(wars[i].getModuleID()) || ear.equals(wars[i].getModuleID() + ".war"))) {
                    selectedModules.add(wars[i]);
                }
            }
            for (int i = 0; i < rars.length; i++) {
                if (ear != null && (ear.equals(rars[i].getModuleID()) || ear.equals(rars[i].getModuleID() + ".war"))) {
                    selectedModules.add(rars[i]);
                }
            }
            modules = selectedModules.toArray(new TargetModuleID[selectedModules.size()]);
        } catch (TargetException ex) {
            logger.error("Cannot obtain deployment application", ex);
            throw new Exception("Cannot obtain deployed applications", ex);
        }

        if (modules.length == 0) {
            return;
        }

        done = false;
        synchronized (this) {
            ProgressObject disableProgress = deploymentManager.stop(modules);
            disableProgress.addProgressListener(new ProgressListener() {

                public void handleProgressEvent(ProgressEvent evt) {
                    DeploymentStatus displayStatus = evt.getDeploymentStatus();
                    logger.debug("Disabling status: " + displayStatus.getState().toString() + " " + displayStatus.getMessage());
                    if (displayStatus.isCompleted()) {
                        logger.info("Disabling completed");
                        ProgressObject undeployProgress = deploymentManager.undeploy(modules);
                        undeployProgress.addProgressListener(new ProgressListener() {
                            public void handleProgressEvent(ProgressEvent evt) {
                                DeploymentStatus displayStatus = evt.getDeploymentStatus();
                                logger.debug("Undeploy status: " + displayStatus.getState().toString() + " " + displayStatus.getMessage());
                                if (displayStatus.isCompleted()) {
                                    done = true;
                                    synchronized (Deployer.this) {
                                        Deployer.this.notify();
                                    }
                                    logger.info("Undeploy completed");
                                }
                            }
                        });
                        if(undeployProgress.getDeploymentStatus().isCompleted()) {
                            done = true;
                            synchronized (Deployer.this) {
                                Deployer.this.notify();
                            }
                            logger.info("Undeploy completed");
                        }
                    }
                }
                });
            if(disableProgress.getDeploymentStatus().isCompleted()) {
                logger.info("Disabling already completed");
                ProgressObject undeployProgress = deploymentManager.undeploy(modules);
                undeployProgress.addProgressListener(new ProgressListener() {
                        public void handleProgressEvent(ProgressEvent evt) {
                            DeploymentStatus displayStatus = evt.getDeploymentStatus();
                            logger.debug("Undeploy status: " + displayStatus.getState().toString() + " " + displayStatus.getMessage());
                            if (displayStatus.isCompleted()) {
                                done = true;
                                synchronized (Deployer.this) {
                                    Deployer.this.notify();
                                }
                                logger.info("Undeploy completed");
                            }
                        }
                    });
                if(undeployProgress.getDeploymentStatus().isCompleted()) {
                    done = true;
                    logger.info("Undeploy already completed");
                }
            }
            while (!done) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    logger.info("Wait for undeploy process interrupted", ex);

                }
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("fallthrough")
    public static void main(String[] args) {
        boolean doDeploy = false, doUndeploy = false;
        String source = null;
        switch (args.length) {
            case 1:
                if ("undeploy".equalsIgnoreCase(args[0])) {
                    doUndeploy = true;
                } else {
                    doDeploy = doUndeploy = true;
                    source = args[0];
                }
                break;
            case 2:
                source = args[1];
                if ("deploy".equalsIgnoreCase(args[0])) {
                    doDeploy = true;
                    break;
                } else if ("undeploy".equalsIgnoreCase(args[0])) {
                    doUndeploy = true;
                    break;
                }
            // deliberate fall through
            default:
                System.out.println("Bad command line arguments, use either:");
                System.out.println("  file.war|file.ear");
                System.out.println("to test deployment or one of:");
                System.out.println("  deploy file.war|file.ear");
                System.out.println("  undeploy");
                System.out.println("to deploy or undeploy all applications");
                System.exit(1);
        }
        try {
            Deployer deployer = new Deployer();
            if (doDeploy) {
                deployer.deploy(source);
            }
            if (doDeploy && doUndeploy) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
                    System.err.println(ex.getMessage());
                }
            }
            if (doUndeploy) {
                deployer.undeploy(source);
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.err.println(ex.getMessage());
            Throwable e = ex.getCause();
            if (e != null) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
