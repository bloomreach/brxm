package org.hippoecm.repository;

public abstract class SpiTestCase extends TestCase {

    HippoRepositoryServer backgroundServer = null;
    HippoRepository server = null;

    @Override
    protected void setUp(boolean clearRepository) throws Exception {
        backgroundServer = new HippoRepositoryServer();
        backgroundServer.run(true);
        Thread.sleep(3000);
        server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository/spi");

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
    }

    @Override
    public void tearDown(boolean clearRepository) throws Exception {
        if (session != null) {
            session.refresh(false);
            if (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
            }
            session.logout();
            session = null;
        }
        if (server != null) {
            server.close();
        }
        if (backgroundServer != null) {
            backgroundServer.close();
        }
    }

}
